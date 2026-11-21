package cl.duocuc.blackout.tournaments.live;

import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cl.duocuc.blackout.tournaments.TournamentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MatchService {
    public static final String EVENTS="blackout.matches.events";
    public static final String SCORES="blackout.matches.scoreboard";
    private final MatchRepository matches;
    private final TournamentRepository tournaments;
    private final OutboxRepository outbox;
    private final ObjectMapper json;
    public MatchService(MatchRepository matches, TournamentRepository tournaments, OutboxRepository outbox, ObjectMapper json) {
        this.matches=matches; this.tournaments=tournaments; this.outbox=outbox; this.json=json;
    }
    @Transactional
    public Match create(MatchController.Create input) {
        if (!tournaments.existsById(input.tournamentId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Torneo inexistente");
        if (input.teamA().trim().equalsIgnoreCase(input.teamB().trim())) throw bad("Los equipos deben ser distintos");
        Match m=new Match();
        m.id=UUID.randomUUID(); m.tournamentId=input.tournamentId();
        m.teamA=input.teamA().trim(); m.teamB=input.teamB().trim(); m.game=input.game().trim();
        m.status="SCHEDULED";
        m=matches.save(m);
        emit(m,"CREATED");
        return m;
    }
    @Transactional
    public Match change(UUID id, MatchController.Command command) {
        Match m=matches.lock(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (m.sequence!=command.expectedVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El partido cambió. Actualiza antes de continuar.");
        switch(command.action()) {
            case "START" -> { require(m,"SCHEDULED"); m.status="LIVE"; }
            case "A","B" -> {
                require(m,"LIVE");
                if(m.rounds.size()>=1000) throw bad("Límite de rondas alcanzado");
                m.rounds.add(command.action());
            }
            case "REVERT" -> {
                require(m,"LIVE");
                if(m.rounds.isEmpty()) throw bad("No hay rondas para anular");
                m.rounds.removeLast();
            }
            case "FINISH" -> {
                require(m,"LIVE");
                long a=m.rounds.stream().filter("A"::equals).count();
                if(a==m.rounds.size()-a) throw bad("El partido debe tener un ganador");
                m.status="FINISHED";
            }
            default -> throw bad("Acción inválida");
        }
        String type=switch(command.action()) {
            case "START" -> "STARTED";
            case "REVERT" -> "ROUND_REVERTED";
            case "FINISH" -> "FINISHED";
            default -> "ROUND_WON";
        };
        emit(m,type);
        return m;
    }
    private void require(Match m,String status) {
        if(!status.equals(m.status)) throw bad("Acción incompatible con el estado del partido");
    }
    private ResponseStatusException bad(String reason) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,reason); }
    private void emit(Match m,String type) {
        m.sequence++;
        enqueue(new MatchEvent(UUID.randomUUID(),1,type,m.id,m.sequence,m.tournamentId,m.teamA,m.teamB,m.game,m.status,List.copyOf(m.rounds),Instant.now()),EVENTS);
    }
    public void enqueue(MatchEvent event,String destination) {
        try {
            PendingEvent p=new PendingEvent();
            p.eventId=event.eventId(); p.matchId=event.matchId(); p.destination=destination;
            p.payload=json.writeValueAsString(event);
            outbox.save(p);
        } catch(JsonProcessingException e) { throw new IllegalStateException("No se pudo serializar el evento",e); }
    }
}
