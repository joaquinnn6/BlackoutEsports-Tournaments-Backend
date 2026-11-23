package cl.duocuc.blackout.kafkaadmin;

import cl.duocuc.blackout.kafkaadmin.service.KafkaAdministrationService;
import java.time.Duration;
import java.util.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@EnabledIfSystemProperty(named="kafka.integration",matches="true")
class KafkaAdminIntegrationTests {
    @Test void administraTopicoDePruebaSinModificarLosTopicosDeLaAplicacion() throws Exception {
        String name="blackout.demo."+UUID.randomUUID();
        try(var client=AdminClient.create(Map.of("bootstrap.servers","localhost:19091,localhost:19092,localhost:19093"))) {
            var service=new KafkaAdministrationService(client);
            try {
                service.create(new KafkaManagement.TopicRequest(name,3,(short)3));
                assertThat(service.list()).contains(name);
                service.configure(name,new KafkaManagement.Settings(3600000));
                assertThat(service.details(name).get("retentionMs")).isEqualTo("3600000");
                service.increasePartitions(name,4);
                service.changeReplicas(name,2);
                await().atMost(Duration.ofSeconds(60)).untilAsserted(()-> {
                    var topic=client.describeTopics(List.of(name)).allTopicNames().get().get(name);
                    assertThat(topic.partitions()).hasSize(4);
                    assertThat(topic.partitions()).allSatisfy(p->assertThat(p.replicas()).hasSize(2));
                });
                service.changeReplicas(name,3);
                await().atMost(Duration.ofSeconds(60)).untilAsserted(()-> {
                    var topic=client.describeTopics(List.of(name)).allTopicNames().get().get(name);
                    assertThat(topic.partitions()).allSatisfy(p-> {
                        assertThat(p.replicas()).hasSize(3); assertThat(p.isr()).hasSize(3);
                    });
                });
                service.groups();
            } finally {
                service.delete(name);
            }
            await().atMost(Duration.ofSeconds(20)).untilAsserted(()->assertThat(service.list()).doesNotContain(name));
        }
    }
}
