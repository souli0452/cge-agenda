
package gov.bf.ascelc.cge_agenda.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static gov.bf.ascelc.cge_agenda.utils.Constant.ROLE_ADMIN;
import static gov.bf.ascelc.cge_agenda.utils.Constant.ROLE_USER;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    public SecurityConfig(JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(fo -> fo.disable()))
                .authorizeHttpRequests(ar -> ar
                        // ==========================================
                        // PUBLIC - Swagger
                        // ==========================================
                        .requestMatchers("/swagger-ui.html", "/v3/**", "/swagger-ui/**").permitAll()

                        // ==========================================
                        // ADMIN SEULEMENT - Suppression totale
                        // ==========================================
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole(ROLE_ADMIN)

                        // ==========================================
                        // USER & ADMIN - Écriture (POST/PUT/PATCH)
                        // ==========================================
                        .requestMatchers(HttpMethod.POST, "/api/v1/cge-agenda/event/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/cge-agenda/event/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cge-agenda/event/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/cge-agenda/participant/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/cge-agenda/participant/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/cge-agenda/file/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)

                        // ==========================================
                        // LECTURE - Tous les utilisateurs authentifiés
                        // ==========================================
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)

                        // ==========================================
                        // EXPORT PDF - Nécessite authentification
                        // ==========================================
                        .requestMatchers("/api/v1/cge-agenda/events/export").hasAnyRole(ROLE_USER, ROLE_ADMIN)

                        // ==========================================
                        // PAR DÉFAUT - Authentifié requis
                        // ==========================================
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(o2 -> o2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}