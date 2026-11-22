package cl.duocuc.blackout.scoreboard.service;

import cl.duocuc.blackout.scoreboard.dto.MatchEvent;
import cl.duocuc.blackout.scoreboard.model.PendingEvent;
import cl.duocuc.blackout.scoreboard.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventOutbox {
    private final OutboxRepository outbox;
    private final ObjectMapper json;
    public EventOutbox(OutboxRepository outbox, ObjectMapper json) { this.outbox=outbox; this.json=json; }

    @Transactional(propagation=Propagation.MANDATORY)
    public void enqueue(MatchEvent event, String destination) {
        try {
            PendingEvent pending = new PendingEvent();
            pending.eventId=event.eventId(); pending.matchId=event.matchId(); pending.destination=destination;
            pending.payload=json.writeValueAsString(event);
            outbox.save(pending);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo serializar el marcador", error);
        }
    }
}
