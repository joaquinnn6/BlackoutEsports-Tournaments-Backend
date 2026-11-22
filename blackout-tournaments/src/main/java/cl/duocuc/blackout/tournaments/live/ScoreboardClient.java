package cl.duocuc.blackout.tournaments.live;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ScoreboardClient {
    private final RestClient http;
    public ScoreboardClient(RestClient.Builder builder, @Value("${app.scoreboard.url:http://localhost:8087}") String url) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        http=builder.baseUrl(url).requestFactory(factory).build();
    }
    public JsonNode read(boolean statistics) {
        try {
            JsonNode result=http.get().uri(statistics ? "/api/matches/statistics" : "/api/matches").retrieve().body(JsonNode.class);
            if (result==null || !result.isArray()) throw new IllegalStateException("Respuesta de marcador inválida");
            return result;
        } catch (RestClientException | IllegalStateException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El marcador se está actualizando. Intenta nuevamente.", error);
        }
    }
}
