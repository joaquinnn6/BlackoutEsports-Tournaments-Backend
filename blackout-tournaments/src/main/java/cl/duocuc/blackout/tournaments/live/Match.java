package cl.duocuc.blackout.tournaments.live;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name="live_matches")
public class Match {
    @Id public UUID id;
    @Version public long revision;
    public long tournamentId;
    public String teamA;
    public String teamB;
    public String game;
    public String status;
    public long sequence;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="live_match_rounds")
    @OrderColumn(name="round_position")
    public List<String> rounds = new ArrayList<>();
}
