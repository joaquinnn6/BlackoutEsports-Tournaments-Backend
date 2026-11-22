package cl.duocuc.blackout.scoreboard.messaging;

import cl.duocuc.blackout.scoreboard.dto.*;
import cl.duocuc.blackout.scoreboard.model.*;
import cl.duocuc.blackout.scoreboard.repository.*;
import cl.duocuc.blackout.scoreboard.service.*;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="app.kafka.enabled",havingValue="true")
public class MatchKafka {
    private final ObjectMapper json;
    private final ProjectionService projections;
    public MatchKafka(ObjectMapper json,ProjectionService projections) { this.json=json; this.projections=projections; }
    @KafkaListener(topics=MatchTopics.EVENTS, groupId="blackout-scoreboard")
    public void scores(String payload) throws Exception { projections.apply(json.readValue(payload,MatchEvent.class),"SCORE"); }
    @KafkaListener(topics=MatchTopics.SCORES, groupId="blackout-match-statistics")
    public void statistics(String payload) throws Exception { projections.apply(json.readValue(payload,MatchEvent.class),"STAT"); }
}
