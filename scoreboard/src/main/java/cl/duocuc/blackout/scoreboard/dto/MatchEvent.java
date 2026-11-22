package cl.duocuc.blackout.scoreboard.dto;

import cl.duocuc.blackout.scoreboard.dto.*;
import cl.duocuc.blackout.scoreboard.model.*;
import cl.duocuc.blackout.scoreboard.repository.*;
import cl.duocuc.blackout.scoreboard.service.*;

import java.time.Instant;
import java.util.*;
public record MatchEvent(UUID eventId, int schemaVersion, String eventType, UUID matchId, long matchVersion,
        long tournamentId, String teamA, String teamB, String game, String status,
        List<String> rounds, Instant occurredAt) {
    public int scoreA() { return (int) rounds.stream().filter("A"::equals).count(); }
    public int scoreB() { return rounds.size() - scoreA(); }
    public void validate() {
        if (eventId == null || matchId == null || schemaVersion != 1 || matchVersion < 1
            || teamA == null || teamB == null || game == null || occurredAt == null
            || !Set.of("CREATED","STARTED","ROUND_WON","ROUND_REVERTED","FINISHED").contains(eventType)
            || !Set.of("SCHEDULED","LIVE","FINISHED").contains(status)
            || rounds == null || rounds.size() > 1000 || rounds.stream().anyMatch(r -> !"A".equals(r) && !"B".equals(r)))
            throw new IllegalArgumentException("Evento de partido inválido");
    }
}
