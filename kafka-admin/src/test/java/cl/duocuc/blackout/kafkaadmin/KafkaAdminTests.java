package cl.duocuc.blackout.kafkaadmin;

import cl.duocuc.blackout.kafkaadmin.service.KafkaAdministrationService;
import java.util.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaAdminTests {
    @Test void noPermiteReducirParticionesNiEliminarTopicosExternos() throws Exception {
        AdminClient admin=mock(AdminClient.class);
        var described=mock(DescribeTopicsResult.class);
        var node=new Node(1,"broker",9092);
        var info=new TopicPartitionInfo(0,node,List.of(node),List.of(node));
        when(admin.describeTopics(List.of("blackout.demo"))).thenReturn(described);
        when(described.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("blackout.demo",new TopicDescription("blackout.demo",false,List.of(info)))));
        var service=new KafkaAdministrationService(admin);
        assertThatThrownBy(()->service.increasePartitions("blackout.demo",1)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.delete("otro.sistema")).isInstanceOf(ResponseStatusException.class);
        verify(admin,never()).createPartitions(anyMap());
        verify(admin,never()).deleteTopics(anyCollection());
    }
    @Test void solicitaAumentoDeParticiones() throws Exception {
        AdminClient admin=mock(AdminClient.class);
        var described=mock(DescribeTopicsResult.class);
        when(admin.describeTopics(List.of("blackout.demo"))).thenReturn(described);
        when(described.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of("blackout.demo",new TopicDescription("blackout.demo",false,List.of()))));
        var created=mock(CreatePartitionsResult.class);
        when(admin.createPartitions(anyMap())).thenReturn(created);
        when(created.all()).thenReturn(KafkaFuture.completedFuture(null));
        new KafkaAdministrationService(admin).increasePartitions("blackout.demo",3);
        verify(admin).createPartitions(argThat(m->m.get("blackout.demo").totalCount()==3));
    }
}
