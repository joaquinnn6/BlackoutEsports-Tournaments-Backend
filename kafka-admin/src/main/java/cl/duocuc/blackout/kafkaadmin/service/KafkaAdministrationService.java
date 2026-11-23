package cl.duocuc.blackout.kafkaadmin.service;

import cl.duocuc.blackout.kafkaadmin.KafkaManagement;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KafkaAdministrationService {
    private final AdminClient admin;
    public KafkaAdministrationService(AdminClient admin) { this.admin=admin; }
    private void check(String name) {
        if(!name.matches("blackout\\.[a-zA-Z0-9._-]+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Solo tópicos Blackout");
    }
    public Set<String> list() throws Exception { return admin.listTopics().names().get(10,TimeUnit.SECONDS); }
    public void create(KafkaManagement.TopicRequest input) throws Exception {
        admin.createTopics(List.of(new NewTopic(input.name(),input.partitions(),input.replicas())
            .configs(Map.of("retention.ms","604800000","cleanup.policy","delete", "min.insync.replicas", Integer.toString(Math.min(2,input.replicas())))))).all().get(10,TimeUnit.SECONDS);
    }
    public Map<String,Object> details(String name) throws Exception {
        check(name);
        var topic=admin.describeTopics(List.of(name)).allTopicNames().get(10,TimeUnit.SECONDS).get(name);
        var resource=new ConfigResource(ConfigResource.Type.TOPIC,name);
        var config=admin.describeConfigs(List.of(resource)).all().get(10,TimeUnit.SECONDS).get(resource);
        return Map.of("name",name,"partitions",topic.partitions().stream().map(p->Map.of(
            "partition",p.partition(),"leader",p.leader()==null ? -1 : p.leader().id(),"replicas",p.replicas().stream().map(n->n.id()).toList(),
            "inSync",p.isr().stream().map(n->n.id()).toList())).toList(),
            "retentionMs",config.get("retention.ms").value(),"cleanupPolicy",config.get("cleanup.policy").value());
    }
    public void configure(String name,KafkaManagement.Settings settings) throws Exception {
        check(name);
        var resource=new ConfigResource(ConfigResource.Type.TOPIC,name);
        var operation=new AlterConfigOp(new ConfigEntry("retention.ms",Long.toString(settings.retentionMs())),AlterConfigOp.OpType.SET);
        admin.incrementalAlterConfigs(Map.of(resource,List.of(operation))).all().get(10,TimeUnit.SECONDS);
    }
    public void delete(String name) throws Exception {
        check(name);
        admin.deleteTopics(List.of(name)).all().get(10,TimeUnit.SECONDS);
    }
    public List<String> groups() throws Exception {
        return admin.listConsumerGroups().all().get(10,TimeUnit.SECONDS).stream().map(ConsumerGroupListing::groupId).toList();
    }
    public List<Map<String,Object>> offsets(String group) throws Exception {
        var committed=admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get(10,TimeUnit.SECONDS);
        var request=new HashMap<org.apache.kafka.common.TopicPartition,OffsetSpec>();
        committed.keySet().forEach(p->request.put(p,OffsetSpec.latest()));
        var latest=admin.listOffsets(request).all().get(10,TimeUnit.SECONDS);
        List<Map<String,Object>> result=new ArrayList<>();
        committed.forEach((p,o)->result.add(Map.of("topic",p.topic(),"partition",p.partition(),"committed",o.offset(),
            "end",latest.get(p).offset(),"lag",Math.max(0,latest.get(p).offset()-o.offset()))));
        return result;
    }
    public void increasePartitions(String name, int count) throws Exception {
        check(name);
        var current=admin.describeTopics(List.of(name)).allTopicNames().get(10,TimeUnit.SECONDS).get(name);
        if(count<=current.partitions().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Solo se permite aumentar particiones");
        admin.createPartitions(Map.of(name,NewPartitions.increaseTo(count))).all().get(10,TimeUnit.SECONDS);
    }

    public void changeReplicas(String name, int count) throws Exception {
        check(name);
        var nodes=admin.describeCluster().nodes().get(10,TimeUnit.SECONDS).stream().map(n->n.id()).sorted().toList();
        if(count>nodes.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No hay suficientes brokers activos");
        var topic=admin.describeTopics(List.of(name)).allTopicNames().get(10,TimeUnit.SECONDS).get(name);
        var resource=new ConfigResource(ConfigResource.Type.TOPIC,name);
        var configuration=admin.describeConfigs(List.of(resource)).all().get(10,TimeUnit.SECONDS).get(resource);
        if(count<Integer.parseInt(configuration.get("min.insync.replicas").value()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Las réplicas no pueden ser menores que min.insync.replicas");
        Map<org.apache.kafka.common.TopicPartition,Optional<NewPartitionReassignment>> assignments=new HashMap<>();
        for(var partition:topic.partitions()) {
            List<Integer> replicas=new ArrayList<>();
            for(int offset=0;offset<count;offset++) replicas.add(nodes.get((partition.partition()+offset)%nodes.size()));
            assignments.put(new org.apache.kafka.common.TopicPartition(name,partition.partition()),Optional.of(new NewPartitionReassignment(replicas)));
        }
        admin.alterPartitionReassignments(assignments).all().get(10,TimeUnit.SECONDS);
    }
}
