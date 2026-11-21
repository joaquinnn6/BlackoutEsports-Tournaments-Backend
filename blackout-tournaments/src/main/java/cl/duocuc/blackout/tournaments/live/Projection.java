package cl.duocuc.blackout.tournaments.live;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="match_projections")
public class Projection {
    @Id public String id;
    @Version public long revision;
    public String kind;
    public UUID matchId;
    public long matchVersion;
    public String teamA;
    public String teamB;
    public String game;
    public String status;
    public int teamAScore;
    public int teamBScore;
    public String winner;
    public Instant updatedAt;
    @Column(columnDefinition="text") public String rounds;
}
