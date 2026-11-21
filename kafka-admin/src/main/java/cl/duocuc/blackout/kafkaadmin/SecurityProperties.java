package cl.duocuc.blackout.kafkaadmin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String entraIssuer,
        String entraAudience,
        String entraScope,
        String cognitoIssuer,
        String cognitoClientId
) { }
