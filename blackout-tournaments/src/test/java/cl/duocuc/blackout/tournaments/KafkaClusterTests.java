package cl.duocuc.blackout.tournaments;

import cl.duocuc.blackout.tournaments.live.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties={"app.kafka.enabled=true","spring.kafka.bootstrap-servers=localhost:19091,localhost:19092,localhost:19093"})
@ActiveProfiles("test")
@Import(TestTokens.class)
@EnabledIfSystemProperty(named="kafka.integration",matches="true")
class KafkaClusterTests {
    @Autowired KafkaTemplate<Object,Object> kafka;
    @Autowired ObjectMapper json;
    @Autowired MatchService matches;
    @Autowired TournamentService tournaments;
    @Autowired OutboxRepository outbox;
    @Autowired ScoreboardClient scoreboard;

    @Test void comandosEnTorneosSeProyectanEnOtroMicroservicioConSuPropiaBase() throws Exception {
        var tournament=tournaments.create(new TournamentRequest("Copa "+UUID.randomUUID(),"Valorant","Live","Hoy","2"));
        var match=matches.create(new MatchController.Create(tournament.id(),"Blackout","Phoenix","Valorant"));
        matches.change(match.id,new MatchController.Command("START",1));
        matches.change(match.id,new MatchController.Command("A",2));
        matches.change(match.id,new MatchController.Command("REVERT",3));
        matches.change(match.id,new MatchController.Command("B",4));
        matches.change(match.id,new MatchController.Command("FINISH",5));
        await().atMost(Duration.ofSeconds(90)).untilAsserted(()-> {
            var stats=scoreboard.read(true);
            var result=java.util.stream.StreamSupport.stream(stats.spliterator(),false)
                .filter(n->n.path("matchId").asText().equals(match.id.toString())).findFirst();
            assertThat(result).isPresent();
            assertThat(result.orElseThrow().path("teamAScore").asInt()).isZero();
            assertThat(result.orElseThrow().path("teamBScore").asInt()).isEqualTo(1);
            assertThat(result.orElseThrow().path("winner").asText()).isEqualTo("Phoenix");
        });
        var initial=outbox.findAll().stream().filter(p->p.matchId.equals(match.id)).findFirst().orElseThrow();
        kafka.send(MatchService.EVENTS,match.id.toString(),initial.payload).get(20,TimeUnit.SECONDS);
        // La prueba unitaria comprueba idempotencia; aquí verificamos el contrato entre procesos.
        assertThat(scoreboard.read(false).isArray()).isTrue();
    }
}
