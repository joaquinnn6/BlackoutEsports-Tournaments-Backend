package cl.duocuc.blackout.tournaments.messaging;

import cl.duocuc.blackout.tournaments.TournamentResponse;
import java.time.Instant;
import java.util.UUID;

public record TournamentCreatedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        TournamentData tournament) {

    public static TournamentCreatedEvent from(TournamentResponse tournament) {
        return new TournamentCreatedEvent(
                UUID.randomUUID(),
                "tournament.created",
                Instant.now(),
                "blackout-tournaments",
                new TournamentData(
                        tournament.id(),
                        tournament.name(),
                        tournament.game(),
                        tournament.status(),
                        tournament.date(),
                        tournament.teams()));
    }

    public record TournamentData(
            Long id,
            String name,
            String game,
            String status,
            String date,
            String teams) {}
}
