package cl.duocuc.blackout.tournaments.messaging;

import cl.duocuc.blackout.tournaments.TournamentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.rabbit.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitTournamentEventPublisher implements TournamentEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMessagingProperties properties;

    @Override
    public void tournamentCreated(TournamentResponse tournament) {
        TournamentCreatedEvent event = TournamentCreatedEvent.from(tournament);
        try {
            rabbitTemplate.convertAndSend(
                    properties.eventsExchange(),
                    properties.routing().tournamentCreated(),
                    event);
            log.info("Evento {} publicado para torneo {}", event.eventId(), tournament.id());
        } catch (AmqpException exception) {
            log.error("No fue posible publicar el evento {} del torneo {}", event.eventId(), tournament.id(), exception);
        }
    }
}
