package cl.duocuc.blackout.tournaments.live;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface OutboxRepository extends JpaRepository<PendingEvent,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingEvent> findFirstByPublishedFalseOrderByIdAsc();
}
