package cl.duocuc.blackout.tournaments.live;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final MatchService service;
    private final MatchRepository matches;
    private final ScoreboardClient projections;
    public MatchController(MatchService service, MatchRepository matches, ScoreboardClient projections) {
        this.service=service; this.matches=matches; this.projections=projections;
    }
    public record Create(@Positive long tournamentId,@NotBlank @Size(max=100) String teamA,
        @NotBlank @Size(max=100) String teamB,@NotBlank @Size(max=100) String game) {}
    public record Command(@NotBlank String action,@Positive long expectedVersion) {}
    @GetMapping public com.fasterxml.jackson.databind.JsonNode scores() { return projections.read(false); }
    @GetMapping("/statistics") public com.fasterxml.jackson.databind.JsonNode statistics() { return projections.read(true); }
    @GetMapping("/manage") public List<Match> manage() { return matches.findAll(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Match create(@Valid @RequestBody Create input) { return service.create(input); }
    @PostMapping("/{id}/commands")
    public Match change(@PathVariable UUID id,@Valid @RequestBody Command command) { return service.change(id,command); }
}
