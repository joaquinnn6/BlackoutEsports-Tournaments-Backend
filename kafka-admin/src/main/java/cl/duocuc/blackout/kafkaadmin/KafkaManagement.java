package cl.duocuc.blackout.kafkaadmin;

import cl.duocuc.blackout.kafkaadmin.service.KafkaAdministrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kafka")
public class KafkaManagement {
    private final KafkaAdministrationService service;
    public KafkaManagement(KafkaAdministrationService service) { this.service=service; }
    public record TopicRequest(@Pattern(regexp="blackout\\.[a-zA-Z0-9._-]+") @NotBlank String name,
        @Min(1) @Max(12) int partitions,@Min(1) @Max(3) short replicas) {}
    public record Settings(@Min(60000) @Max(2592000000L) long retentionMs) {}
    public record Partitions(@Min(1) @Max(12) int count) {}
    public record Replicas(@Min(1) @Max(3) int count) {}
    @GetMapping("/topics") public Set<String> list() throws Exception { return service.list(); }
    @PostMapping("/topics") @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody TopicRequest input) throws Exception { service.create(input); }
    @GetMapping("/topics/{name}")
    public Map<String,Object> details(@PathVariable String name) throws Exception { return service.details(name); }
    @PutMapping("/topics/{name}/configuration")
    public void configure(@PathVariable String name,@Valid @RequestBody Settings input) throws Exception { service.configure(name,input); }
    @PutMapping("/topics/{name}/partitions")
    public void partitions(@PathVariable String name,@Valid @RequestBody Partitions input) throws Exception { service.increasePartitions(name,input.count()); }
    @PutMapping("/topics/{name}/replicas") @ResponseStatus(HttpStatus.ACCEPTED)
    public void replicas(@PathVariable String name,@Valid @RequestBody Replicas input) throws Exception { service.changeReplicas(name,input.count()); }
    @DeleteMapping("/topics/{name}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) throws Exception { service.delete(name); }
    @GetMapping("/groups") public List<String> groups() throws Exception { return service.groups(); }
    @GetMapping("/groups/{group}/offsets")
    public List<Map<String,Object>> offsets(@PathVariable String group) throws Exception { return service.offsets(group); }
}
