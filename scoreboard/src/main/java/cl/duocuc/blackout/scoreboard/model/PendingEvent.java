package cl.duocuc.blackout.scoreboard.model;

import cl.duocuc.blackout.scoreboard.dto.*;
import cl.duocuc.blackout.scoreboard.model.*;
import cl.duocuc.blackout.scoreboard.repository.*;
import cl.duocuc.blackout.scoreboard.service.*;

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
