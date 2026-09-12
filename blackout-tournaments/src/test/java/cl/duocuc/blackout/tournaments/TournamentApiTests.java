package cl.duocuc.blackout.tournaments;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TournamentApiTests {
    @Autowired MockMvc mvc;
    @Autowired TournamentRepository repository;
    @Autowired ObjectMapper mapper;

    @BeforeEach void clean() { repository.deleteAll(); }

    private String body(String name) throws Exception {
        return mapper.writeValueAsString(new TournamentRequest(name, "Valorant", "En curso", "EN VIVO", "16 equipos"));
    }

    private long createTournament(String name) throws Exception {
        String response = mvc.perform(post("/api/tournaments").contentType(MediaType.APPLICATION_JSON).content(body(name)))
                .andExpect(status().isCreated()).andExpect(header().string("Location", startsWith("/api/tournaments/")))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }

    @Test void crudPersistsChangesAndDeletes() throws Exception {
        mvc.perform(get("/api/tournaments")).andExpect(status().isOk()).andExpect(content().json("[]"));
        long id = createTournament("OpsLeague");
        mvc.perform(get("/api/tournaments/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("OpsLeague"));
        mvc.perform(put("/api/tournaments/" + id).contentType(MediaType.APPLICATION_JSON).content(body("Finals")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Finals"));
        mvc.perform(get("/api/tournaments")).andExpect(jsonPath("$[0].name").value("Finals")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(delete("/api/tournaments/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tournaments/" + id)).andExpect(status().isNotFound());
    }

    @Test void rejectsMissingBlankAndOversizedFields() throws Exception {
        mvc.perform(post("/api/tournaments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.game").exists());
        mvc.perform(post("/api/tournaments").contentType(MediaType.APPLICATION_JSON).content(body("  ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
        mvc.perform(post("/api/tournaments").contentType(MediaType.APPLICATION_JSON).content(body("x".repeat(121))))
                .andExpect(status().isBadRequest());
    }

    @Test void rejectsDuplicatesIncludingCaseAndSpaces() throws Exception {
        long first = createTournament("OpsLeague");
        long second = createTournament("Finals");
        mvc.perform(post("/api/tournaments").contentType(MediaType.APPLICATION_JSON).content(body(" opsleague ")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/tournaments/" + second).contentType(MediaType.APPLICATION_JSON).content(body("OpsLeague")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/tournaments/" + first).contentType(MediaType.APPLICATION_JSON).content(body("OpsLeague")))
                .andExpect(status().isOk());
    }

    @Test void missingUpdateAndDeleteReturn404() throws Exception {
        mvc.perform(put("/api/tournaments/9999").contentType(MediaType.APPLICATION_JSON).content(body("Missing")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/tournaments/9999")).andExpect(status().isNotFound());
    }

    @Test void malformedJsonAndIdsReturn400() throws Exception {
        mvc.perform(post("/api/tournaments").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/tournaments/abc")).andExpect(status().isBadRequest());
    }

    @Test void corsAllowsAdminOriginAndRejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/tournaments").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/tournaments").header("Origin", "https://unknown.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    @Test void openApiDocumentsCrud() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/tournaments'].post").exists())
                .andExpect(jsonPath("$.paths['/api/tournaments/{id}'].delete").exists());
    }
}
