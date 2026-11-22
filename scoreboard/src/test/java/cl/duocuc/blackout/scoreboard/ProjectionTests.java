package cl.duocuc.blackout.scoreboard;

import cl.duocuc.blackout.scoreboard.dto.*;
import cl.duocuc.blackout.scoreboard.repository.*;
import cl.duocuc.blackout.scoreboard.service.ProjectionService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectionTests {
    @Autowired ProjectionService service;
    @Autowired ProjectionRepository projections;
    @Autowired OutboxRepository outbox;
    @Autowired MockMvc mvc;

    @BeforeEach void clear() { outbox.deleteAll(); projections.deleteAll(); }
    @Test void duplicadosVersionAntiguaYAnulacionNoCambianElResultadoConfirmado() throws Exception {
        UUID id=UUID.randomUUID();
        MatchEvent old=event(id,1,"LIVE",List.of("A","A"));
        MatchEvent corrected=event(id,2,"LIVE",List.of("A"));
        service.apply(old,"SCORE"); service.apply(corrected,"SCORE");
        service.apply(old,"SCORE"); service.apply(corrected,"SCORE");
        assertThat(outbox.count()).isEqualTo(2);
        mvc.perform(get("/api/matches")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].teamAScore").value(1));
        MatchEvent finished=event(id,3,"FINISHED",List.of("A","B","B"));
        service.apply(finished,"SCORE"); service.apply(finished,"STAT"); service.apply(finished,"STAT");
        assertThat(outbox.count()).isEqualTo(3);
        mvc.perform(get("/api/matches/statistics")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].winner").value("Phoenix"));
        mvc.perform(post("/api/matches")).andExpect(status().isForbidden());
    }
    @Test void eventoInvalidoNoGuardaProyeccionNiOutbox() {
        assertThatThrownBy(()->service.apply(event(UUID.randomUUID(),1,"LIVE",List.of("C")),"SCORE"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(projections.count()).isZero(); assertThat(outbox.count()).isZero();
    }
    MatchEvent event(UUID id,long version,String status,List<String> rounds) {
        return new MatchEvent(UUID.randomUUID(),1,status.equals("FINISHED") ? "FINISHED" : "ROUND_WON",id,
            version,1,"Blackout","Phoenix","Valorant",status,rounds,Instant.now());
    }
}
