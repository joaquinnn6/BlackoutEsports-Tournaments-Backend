package cl.duocuc.blackout.scoreboard.config;

import cl.duocuc.blackout.scoreboard.dto.*;
import cl.duocuc.blackout.scoreboard.model.*;
import cl.duocuc.blackout.scoreboard.repository.*;
import cl.duocuc.blackout.scoreboard.service.*;


import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="app.kafka.enabled",havingValue="true")
public class KafkaConfiguration {
    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(3)
            .config("min.insync.replicas","2").config("retention.ms","604800000").config("cleanup.policy","delete").build();
    }
    @Bean NewTopic matchEvents() { return topic(MatchTopics.EVENTS); }
    @Bean NewTopic matchScores() { return topic(MatchTopics.SCORES); }
    @Bean NewTopic failedEvents() { return topic(MatchTopics.EVENTS+".DLT"); }
    @Bean NewTopic failedScores() { return topic(MatchTopics.SCORES+".DLT"); }
    @Bean DefaultErrorHandler kafkaErrors(KafkaTemplate<Object,Object> template) {
        var recoverer=new DeadLetterPublishingRecoverer(template,(record,error)->new TopicPartition(record.topic()+".DLT",record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        return new DefaultErrorHandler(recoverer,new FixedBackOff(1000L,2L));
    }
}
