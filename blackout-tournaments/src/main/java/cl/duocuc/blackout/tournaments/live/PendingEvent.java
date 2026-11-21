package cl.duocuc.blackout.tournaments.live;
import jakarta.persistence.*;
import java.util.UUID;
@Entity
@Table(name="match_outbox", uniqueConstraints=@UniqueConstraint(columnNames={"destination","eventId"}))
public class PendingEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public UUID eventId;
    public UUID matchId;
    @Column(nullable=false) public String destination;
    @Column(columnDefinition="text",nullable=false) public String payload;
    public boolean published;
}
