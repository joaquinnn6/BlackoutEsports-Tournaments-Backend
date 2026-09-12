package cl.duocuc.blackout.tournaments;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    boolean existsByNameKeyAndGameKey(String nameKey, String gameKey);
    boolean existsByNameKeyAndGameKeyAndIdNot(String nameKey, String gameKey, Long id);
}
