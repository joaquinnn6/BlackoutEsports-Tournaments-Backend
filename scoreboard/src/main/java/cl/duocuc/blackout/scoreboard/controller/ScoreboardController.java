package cl.duocuc.blackout.scoreboard.controller;

import cl.duocuc.blackout.scoreboard.model.Projection;
import cl.duocuc.blackout.scoreboard.repository.ProjectionRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class ScoreboardController {
    private final ProjectionRepository projections;
    public ScoreboardController(ProjectionRepository projections) { this.projections=projections; }
    @GetMapping public List<Projection> scores() { return projections.findByKind("SCORE"); }
    @GetMapping("/statistics") public List<Projection> statistics() { return projections.findByKind("STAT"); }
}
