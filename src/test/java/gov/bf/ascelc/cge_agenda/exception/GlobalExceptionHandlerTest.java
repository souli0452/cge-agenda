package gov.bf.ascelc.cge_agenda.exception;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sans handler dedie, une BadRequestException levee par le client admin
 * Keycloak (ex: mot de passe qui ne respecte pas la politique du realm lors
 * d'une creation/reinitialisation) tombe dans le catch-all generique et
 * ressort en 500 avec un message qui ne dit rien de la vraie cause.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleKeycloakBadRequest_returns400WithActionableMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleKeycloakBadRequest(new BadRequestException("HTTP 400 Bad Request"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("politique de sécurité");
    }
}
