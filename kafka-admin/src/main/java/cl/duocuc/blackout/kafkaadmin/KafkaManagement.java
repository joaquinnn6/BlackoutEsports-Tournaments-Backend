package cl.duocuc.blackout.kafkaadmin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Configuration
class AdminConnection {
    @Bean(destroyMethod="close")
    AdminClient adminClient(@Value("${spring.kafka.bootstrap-servers}") String servers) {
        return AdminClient.create(Map.of("bootstrap.servers",servers,"default.api.timeout.ms",10000));
    }
}

@RestController
@RequestMapping("/api/kafka")
public class KafkaManagement {
    private final AdminClient admin;
    public KafkaManagement(AdminClient admin) { this.admin=admin; }
    public record TopicRequest(@Pattern(regexp="blackout\\.[a-zA-Z0-9._-]+") @NotBlank String name,
            @Min(1) @Max(12) int partitions,@Min(1) @Max(3) short replicas) {}
    public record Settings(@Min(60000) @Max(2592000000L) long retentionMs) {}
    private void check(String name) {
        if(!name.startsWith("blackout.")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Solo tópicos Blackout");
    }
    @GetMapping("/topics")
    public Set<String> list() throws Exception { return admin.listTopics().names().get(10,TimeUnit.SECONDS); }
    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody TopicRequest input) throws Exception {
        admin.createTopics(List.of(new NewTopic(input.name(),input.partitions(),input.replicas())
            .configs(Map.of("retention.ms","604800000","cleanup.policy","delete")))).all().get(10,TimeUnit.SECONDS);
    }
    @GetMapping("/topics/{name}")
    public Map<String,Object> details(@PathVariable String name) throws Exception {
        check(name);
        var topic=admin.describeTopics(List.of(name)).allTopicNames().get(10,TimeUnit.SECONDS).get(name);
        var resource=new ConfigResource(ConfigResource.Type.TOPIC,name);
        var config=admin.describeConfigs(List.of(resource)).all().get(10,TimeUnit.SECONDS).get(resource);
        return Map.of("name",name,"partitions",topic.partitions().stream().map(p->Map.of(
            "partition",p.partition(),"leader",p.leader().id(),"replicas",p.replicas().stream().map(n->n.id()).toList(),
            "inSync",p.isr().stream().map(n->n.id()).toList())).toList(),
            "retentionMs",config.get("retention.ms").value(),"cleanupPolicy",config.get("cleanup.policy").value());
    }
    @PutMapping("/topics/{name}/configuration")
    public void configure(@PathVariable String name,@Valid @RequestBody Settings settings) throws Exception {
        check(name);
        var resource=new ConfigResource(ConfigResource.Type.TOPIC,name);
        var operation=new AlterConfigOp(new ConfigEntry("retention.ms",Long.toString(settings.retentionMs())),AlterConfigOp.OpType.SET);
        admin.incrementalAlterConfigs(Map.of(resource,List.of(operation))).all().get(10,TimeUnit.SECONDS);
    }
    @DeleteMapping("/topics/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) throws Exception {
        check(name);
        admin.deleteTopics(List.of(name)).all().get(10,TimeUnit.SECONDS);
    }
    @GetMapping("/groups")
    public List<String> groups() throws Exception {
        return admin.listConsumerGroups().all().get(10,TimeUnit.SECONDS).stream().map(ConsumerGroupListing::groupId).toList();
    }
    @GetMapping("/groups/{group}/offsets")
    public List<Map<String,Object>> offsets(@PathVariable String group) throws Exception {
        var committed=admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get(10,TimeUnit.SECONDS);
        var request=new HashMap<org.apache.kafka.common.TopicPartition,OffsetSpec>();
        committed.keySet().forEach(p->request.put(p,OffsetSpec.latest()));
        var latest=admin.listOffsets(request).all().get(10,TimeUnit.SECONDS);
        List<Map<String,Object>> result=new ArrayList<>();
        committed.forEach((p,o)->result.add(Map.of("topic",p.topic(),"partition",p.partition(),"committed",o.offset(),
            "end",latest.get(p).offset(),"lag",Math.max(0,latest.get(p).offset()-o.offset()))));
        return result;
    }
}
