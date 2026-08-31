
package gov.bf.ascelc.cge_agenda.security;

import org.springframework.beans.factory.annotation.Value;
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
import static gov.bf.ascelc.cge_agenda.utils.Constant.ROLE_CGE;
import static gov.bf.ascelc.cge_agenda.utils.Constant.ROLE_SECRETAIRE;
import static gov.bf.ascelc.cge_agenda.utils.Constant.ROLE_PROTOCOLE;
import static gov.bf.ascelc.cge_agenda.utils.Constant.ROLE_DIRECTEUR_CABINET;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000}")
    private String corsAllowedOrigins;

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
                        // WORKFLOW DE VALIDATION - tous rôles métier (le vrai contrôle est
                        // désormais "êtes-vous le chef de l'espace de cet événement ?",
                        // vérifié en service — EventServiceImpl.assertEstValidateur — pas
                        // par rôle Keycloak, puisque le chef peut avoir n'importe quel rôle)
                        // ==========================================
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cge-agenda/event/validate/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cge-agenda/event/reject/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cge-agenda/event/request-changes/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cge-agenda/event/delegate/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cge-agenda/event/*/observation")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cge-agenda/event/*/demander-delegation")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)

                        // ==========================================
                        // DÉLÉGATION — liens signés cliqués depuis l'email, sans authentification
                        // ==========================================
                        .requestMatchers("/api/v1/cge-agenda/delegation/**").permitAll()

                        // ==========================================
                        // ESPACE-MEMBRE — lien signé d'invitation, sans authentification
                        // ==========================================
                        .requestMatchers("/api/v1/cge-agenda/espace-membre/**").permitAll()

                        // ==========================================
                        // ESPACES — gestion des gestionnaires par le propriétaire (vérifié en
                        // service), création/suppression d'espace réservée à ADMIN via /admin/espaces
                        // ==========================================
                        .requestMatchers(HttpMethod.POST, "/api/v1/cge-agenda/espaces/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/cge-agenda/espaces/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)

                        // ==========================================
                        // ÉCRITURE - Tous les rôles métier authentifiés (POST/PUT/PATCH)
                        // ==========================================
                        .requestMatchers(HttpMethod.POST, "/api/v1/cge-agenda/event/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/cge-agenda/event/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/cge-agenda/event/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.POST, "/api/v1/cge-agenda/participant/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/cge-agenda/participant/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.POST, "/api/v1/cge-agenda/file/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.POST, "/api/v1/cge-agenda/schedule/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/cge-agenda/schedule/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)

                        // ==========================================
                        // STATISTIQUES - ADMIN/CGE/DIRECTEUR_CABINET seulement
                        // (règle spécifique, évaluée avant la règle GET générique ci-dessous)
                        // ==========================================
                        .requestMatchers(HttpMethod.GET, "/api/v1/cge-agenda/stats/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_DIRECTEUR_CABINET)

                        // ==========================================
                        // LECTURE - Tous les rôles métier authentifiés
                        // ==========================================
                        .requestMatchers(HttpMethod.GET, "/api/**")
                                .hasAnyRole(ROLE_ADMIN, ROLE_CGE, ROLE_SECRETAIRE, ROLE_PROTOCOLE, ROLE_DIRECTEUR_CABINET)

                        // ==========================================
                        // JOURNAL D'AUDIT - ADMIN seulement
                        // ==========================================
                        .requestMatchers("/api/v1/cge-agenda/audit/**").hasRole(ROLE_ADMIN)

                        // ==========================================
                        // ADMINISTRATION (utilisateurs/rôles) - ADMIN seulement
                        // ==========================================
                        .requestMatchers("/api/v1/cge-agenda/admin/**").hasRole(ROLE_ADMIN)

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
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
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