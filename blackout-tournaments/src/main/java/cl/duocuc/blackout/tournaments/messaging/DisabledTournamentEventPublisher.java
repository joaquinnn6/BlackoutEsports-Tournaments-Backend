package cl.duocuc.blackout.tournaments.messaging;

import cl.duocuc.blackout.tournaments.TournamentResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbit.enabled", havingValue = "false")
public class DisabledTournamentEventPublisher implements TournamentEventPublisher {
    @Override
    public void tournamentCreated(TournamentResponse tournament) {
        // Permite ejecutar pruebas o desarrollo aislado sin un broker RabbitMQ.
    }
}
