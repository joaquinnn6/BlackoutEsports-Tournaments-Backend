package cl.duocuc.blackout.tournaments.live;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ProjectionService {
    private final ProjectionRepository projections;
    private final MatchService events;
    public ProjectionService(ProjectionRepository projections,MatchService events) {
        this.projections=projections; this.events=events;
    }
    @Transactional
    public void apply(MatchEvent event,String kind) {
        event.validate();
        String id=kind+":"+event.matchId();
        Projection p=projections.findById(id).orElseGet(Projection::new);
        if(p.matchVersion>=event.matchVersion()) return;
        p.id=id; p.kind=kind; p.matchId=event.matchId(); p.matchVersion=event.matchVersion();
        p.teamA=event.teamA(); p.teamB=event.teamB(); p.game=event.game(); p.status=event.status();
        p.teamAScore=event.scoreA(); p.teamBScore=event.scoreB(); p.updatedAt=event.occurredAt();
        p.rounds=String.join(",",event.rounds());
        p.winner="FINISHED".equals(event.status()) ? (p.teamAScore>p.teamBScore ? p.teamA : p.teamB) : null;
        projections.saveAndFlush(p);
        if("SCORE".equals(kind)) events.enqueue(event,MatchService.SCORES);
    }
}
