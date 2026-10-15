package cl.duocuc.blackout.tournaments.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cl.duocuc.blackout.tournaments.TournamentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitTournamentEventPublisherTest {
    @Test
    void publishesCreatedTournamentWithConfiguredRoutingKey() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMessagingProperties properties = new RabbitMessagingProperties(
                true,
                "blackout.events.exchange",
                "blackout.dead-letter.exchange",
                new RabbitMessagingProperties.Queues("fan.notifications.queue", "fan.notifications.dlq"),
                new RabbitMessagingProperties.Routing("tournament.#", "fan.notifications.failed", "tournament.created"),
                3);
        RabbitTournamentEventPublisher publisher = new RabbitTournamentEventPublisher(rabbitTemplate, properties);
        TournamentResponse tournament = new TournamentResponse(
                8L, "Copa Blackout", "Valorant", "Programado", "25 OCT 2026", "16 equipos");

        publisher.tournamentCreated(tournament);

        verify(rabbitTemplate).convertAndSend(
                eq("blackout.events.exchange"),
                eq("tournament.created"),
                any(TournamentCreatedEvent.class));
    }
}
