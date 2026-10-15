package cl.duocuc.blackout.tournaments.messaging;

import cl.duocuc.blackout.tournaments.TournamentResponse;

public interface TournamentEventPublisher {
    void tournamentCreated(TournamentResponse tournament);
}
