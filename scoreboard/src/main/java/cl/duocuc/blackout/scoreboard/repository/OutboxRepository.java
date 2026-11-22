package cl.duocuc.blackout.scoreboard.repository;

import cl.duocuc.blackout.scoreboard.dto.*;
import cl.duocuc.blackout.scoreboard.model.*;
import cl.duocuc.blackout.scoreboard.repository.*;
import cl.duocuc.blackout.scoreboard.service.*;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface OutboxRepository extends JpaRepository<PendingEvent,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingEvent> findFirstByPublishedFalseOrderByIdAsc();
}
