package cl.duocuc.blackout.tournaments;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentService {
    private final TournamentRepository repository;

    public List<TournamentResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(TournamentResponse::from).toList();
    }

    public TournamentResponse findById(Long id) {
        return TournamentResponse.from(requireTournament(id));
    }

    @Transactional
    public TournamentResponse create(TournamentRequest request) {
        checkDuplicate(request, null);
        Tournament tournament = new Tournament();
        apply(tournament, request);
        return TournamentResponse.from(repository.saveAndFlush(tournament));
    }

    @Transactional
    public TournamentResponse update(Long id, TournamentRequest request) {
        Tournament tournament = requireTournament(id);
        checkDuplicate(request, id);
        apply(tournament, request);
        return TournamentResponse.from(repository.saveAndFlush(tournament));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(requireTournament(id));
    }

    private Tournament requireTournament(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Torneo no encontrado"));
    }

    private void checkDuplicate(TournamentRequest request, Long id) {
        String name = key(request.name());
        String game = key(request.game());
        boolean exists = id == null ? repository.existsByNameKeyAndGameKey(name, game)
                : repository.existsByNameKeyAndGameKeyAndIdNot(name, game, id);
        if (exists) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe ese torneo para el juego");
    }

    private static String key(String text) {
        return text.strip().toLowerCase(Locale.ROOT);
    }

    private static void apply(Tournament tournament, TournamentRequest request) {
        tournament.setName(request.name().strip());
        tournament.setGame(request.game().strip());
        tournament.setStatus(request.status().strip());
        tournament.setDate(request.date().strip());
        tournament.setTeams(request.teams().strip());
        tournament.setNameKey(key(request.name()));
        tournament.setGameKey(key(request.game()));
    }
}
