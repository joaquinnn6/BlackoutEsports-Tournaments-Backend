package cl.duocuc.blackout.tournaments;

import cl.duocuc.blackout.tournaments.live.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestTokens.class)
class LiveMatchTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OutboxRepository outbox;
    @Autowired ProjectionService projector;
    @Autowired ProjectionRepository projections;
    @Autowired MatchRepository matches;

    @BeforeEach void clear() { outbox.deleteAll(); projections.deleteAll(); matches.deleteAll(); }

    @Test void rondaAnulacionDuplicadoYProyeccion() throws Exception {
        String admin="Bearer "+TestTokens.admin();
        String tournament=mvc.perform(post("/api/tournaments").header("Authorization",admin)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new TournamentRequest("Copa","Valorant","Live","Hoy","2"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long tournamentId=json.readTree(tournament).get("id").asLong();
        var create=new MatchController.Create(tournamentId,"Blackout","Phoenix","Valorant");
        String body=mvc.perform(post("/api/matches").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(create))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=json.readTree(body).get("id").asText();
        String path="/api/matches/"+id+"/commands";
        command(path,admin,"START",1,200);
        command(path,admin,"A",2,200);
        command(path,admin,"A",2,409);
        command(path,"Bearer "+TestTokens.fan(),"B",3,403);
        command(path,admin,"REVERT",3,200);
        command(path,admin,"REVERT",4,400);
        command(path,admin,"FINISH",4,400);
        command(path,admin,"B",4,200);
        command(path,admin,"FINISH",5,200);
        command(path,admin,"A",6,400);
        mvc.perform(get("/api/matches/manage")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/matches")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        var source=outbox.findAll();
        assertThat(source).hasSize(6);
        for(var p:source) {
            MatchEvent event=json.readValue(p.payload,MatchEvent.class);
            projector.apply(event,"SCORE");
            projector.apply(event,"SCORE");
        }
        assertThat(outbox.count()).isEqualTo(12);
        projector.apply(json.readValue(source.getFirst().payload,MatchEvent.class),"SCORE");
        mvc.perform(get("/api/matches")).andExpect(jsonPath("$[0].teamAScore").value(0))
            .andExpect(jsonPath("$[0].teamBScore").value(1)).andExpect(jsonPath("$[0].winner").value("Phoenix"));
        for(var p:outbox.findAll()) if(p.destination.equals(MatchService.SCORES))
            projector.apply(json.readValue(p.payload,MatchEvent.class),"STAT");
        mvc.perform(get("/api/matches/statistics")).andExpect(jsonPath("$[0].winner").value("Phoenix"));
    }

    void command(String path,String token,String action,long version,int expected) throws Exception {
        mvc.perform(post(path).header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(new MatchController.Command(action,version))))
            .andExpect(status().is(expected));
    }
}
