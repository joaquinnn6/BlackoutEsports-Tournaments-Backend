package cl.duocuc.blackout.tournaments;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Tag(name = "Torneos", description = "Crear, consultar, editar y eliminar torneos")
public class TournamentController {
    private final TournamentService service;

    @GetMapping
    public List<TournamentResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public TournamentResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@Valid @RequestBody TournamentRequest request) {
        TournamentResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/tournaments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TournamentResponse update(@PathVariable Long id, @Valid @RequestBody TournamentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
