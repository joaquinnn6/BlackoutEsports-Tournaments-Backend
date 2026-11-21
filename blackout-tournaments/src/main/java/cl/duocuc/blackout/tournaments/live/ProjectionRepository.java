package cl.duocuc.blackout.tournaments.live;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectionRepository extends JpaRepository<Projection,String> {
    List<Projection> findByKind(String kind);
}
