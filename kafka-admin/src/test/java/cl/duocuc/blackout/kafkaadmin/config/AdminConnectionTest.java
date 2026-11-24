package cl.duocuc.blackout.kafkaadmin.config;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminConnectionTest {
    @Test
    void createsRealClientWithCompatibleTimeoutsWithoutRequiringBroker() {
        var client = new AdminConnection().adminClient("127.0.0.1:1");
        try {
            assertNotNull(client);
        } finally {
            client.close(Duration.ZERO);
        }
    }
}
