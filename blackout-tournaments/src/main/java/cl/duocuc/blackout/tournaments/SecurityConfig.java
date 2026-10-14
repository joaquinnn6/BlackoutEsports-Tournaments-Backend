package cl.duocuc.blackout.tournaments;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        return new TrustedJwtDecoder(properties);
    }

    static JwtAuthenticationToken authentication(Jwt jwt, SecurityProperties properties) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        String issuer = jwt.getClaimAsString("iss");
        if (properties.entraIssuer().equals(issuer)) {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.stream()
                        .filter(role -> role.equals("Admin") || role.equals("Staff"))
                        .forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
        } else if (properties.cognitoIssuer().equals(issuer)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_Fan"));
        }
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, SecurityProperties properties) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tournaments", "/api/tournaments/**")
                            .permitAll()
                        .requestMatchers("/api/tournaments", "/api/tournaments/**").hasAnyRole("Admin", "Staff")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(token -> authentication(token, properties))))
                .build();
    }
}
