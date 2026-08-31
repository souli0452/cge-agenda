package gov.bf.ascelc.cge_agenda.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Génère et vérifie des liens signés (HMAC-SHA256) à usage unique par email — utilisés
 * pour accepter/décliner une délégation sans authentification (clic direct depuis le mail).
 * Payload = eventId|email|expiration ; signature = HMAC-SHA256(secret, payload).
 */
@Slf4j
@Service
public class SignedTokenService {

    @Value("${app.delegation.token-secret}")
    private String secret;

    private static final String ALGO = "HmacSHA256";
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    public String generate(UUID eventId, String email, Duration validity) {
        long expiry = Instant.now().plus(validity).getEpochSecond();
        String payload = eventId + "|" + email + "|" + expiry;
        String signature = sign(payload);
        return ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + signature;
    }

    public Optional<Payload> verify(String token) {
        try {
            int dot = token.indexOf('.');
            if (dot < 0) {
                return Optional.empty();
            }
            String encodedPayload = token.substring(0, dot);
            String signature = token.substring(dot + 1);
            String payload = new String(DEC.decode(encodedPayload), StandardCharsets.UTF_8);

            if (!sign(payload).equals(signature)) {
                log.warn("⚠ Signature de token invalide");
                return Optional.empty();
            }

            String[] parts = payload.split("\\|", 3);
            if (parts.length != 3) {
                return Optional.empty();
            }
            UUID eventId = UUID.fromString(parts[0]);
            String email = parts[1];
            long expiry = Long.parseLong(parts[2]);

            if (Instant.now().getEpochSecond() > expiry) {
                log.warn("⚠ Token expiré pour l'événement {}", eventId);
                return Optional.empty();
            }
            return Optional.of(new Payload(eventId, email));
        } catch (Exception e) {
            log.warn("⚠ Token invalide : {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
            return ENC.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de signer le token", e);
        }
    }

    public record Payload(UUID eventId, String email) {}
}
