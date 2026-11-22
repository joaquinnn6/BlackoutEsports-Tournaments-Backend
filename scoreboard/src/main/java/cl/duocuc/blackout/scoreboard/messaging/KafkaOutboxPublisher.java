package cl.duocuc.blackout.scoreboard.messaging;

import cl.duocuc.blackout.scoreboard.dto.*;
import cl.duocuc.blackout.scoreboard.model.*;
import cl.duocuc.blackout.scoreboard.repository.*;
import cl.duocuc.blackout.scoreboard.service.*;


import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name="app.kafka.enabled",havingValue="true")
public class KafkaOutboxPublisher {
    private final OutboxRepository outbox;
    private final KafkaTemplate<Object,Object> kafka;
    public KafkaOutboxPublisher(OutboxRepository outbox,KafkaTemplate<Object,Object> kafka) {this.outbox=outbox;this.kafka=kafka;}
    @Scheduled(fixedDelayString="${app.kafka.publish-delay:250}")
    @Transactional(rollbackFor=Exception.class)
    public void publishNext() throws Exception {
        var pending=outbox.findFirstByPublishedFalseOrderByIdAsc();
        if(pending.isEmpty()) return;
        var event=pending.get();
        kafka.send(event.destination,event.matchId.toString(),event.payload).get(15,TimeUnit.SECONDS);
        event.published=true;
    }
}
