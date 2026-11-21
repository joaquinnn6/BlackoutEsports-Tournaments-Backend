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
    @Autowired ProjectionRepository projections;
    @Autowired ObjectMapper json;

    @Test void tresBrokersProcesanAmbosTopicosYDuplicados() throws Exception {
        UUID match=UUID.randomUUID();
        MatchEvent event=new MatchEvent(UUID.randomUUID(),1,"FINISHED",match,4,1,"Blackout","Phoenix","Valorant","FINISHED",List.of("A","B","A"),Instant.now());
        String payload=json.writeValueAsString(event);
        kafka.send(MatchService.EVENTS,match.toString(),payload).get(20,TimeUnit.SECONDS);
        kafka.send(MatchService.EVENTS,match.toString(),payload).get(20,TimeUnit.SECONDS);
        await().atMost(Duration.ofSeconds(60)).untilAsserted(()->{
            var result=projections.findById("STAT:"+match);
            assertThat(result).isPresent();
            assertThat(result.orElseThrow().teamAScore).isEqualTo(2);
            assertThat(result.orElseThrow().winner).isEqualTo("Blackout");
        });
    }
}
