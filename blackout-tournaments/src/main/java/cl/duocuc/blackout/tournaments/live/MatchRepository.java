package cl.duocuc.blackout.tournaments.live;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface MatchRepository extends JpaRepository<Match,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :id")
    Optional<Match> lock(UUID id);
}
