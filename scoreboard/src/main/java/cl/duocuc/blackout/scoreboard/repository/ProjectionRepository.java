package cl.duocuc.blackout.scoreboard.repository;

import cl.duocuc.blackout.scoreboard.dto.*;
import cl.duocuc.blackout.scoreboard.model.*;
import cl.duocuc.blackout.scoreboard.repository.*;
import cl.duocuc.blackout.scoreboard.service.*;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectionRepository extends JpaRepository<Projection,String> {
    List<Projection> findByKind(String kind);
}
