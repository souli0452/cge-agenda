package gov.bf.ascelc.cge_agenda.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions
 * Capture TOUTES les exceptions de l'application et retourne des réponses HTTP appropriées
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère toutes les exceptions métier lancées avec ResponseStatusException
     * Exemples : NOT_FOUND, CONFLICT, BAD_REQUEST, etc.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        ErrorResponse error = new ErrorResponse(
                ex.getStatusCode().value(),
                ex.getReason(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    // ==========================================
    // GESTION ERREUR UUID (KEYCLOAK)
    // ==========================================
    /**
     * Gère les erreurs de conversion UUID (erreur avec "all", etc.)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleUUIDConversionError(
            MethodArgumentTypeMismatchException ex
    ) {
        log.error("❌ Erreur de conversion de type : {}", ex.getMessage());

        String message = "Format invalide pour le paramètre '" + ex.getName() + "'. ";

        if (ex.getRequiredType() != null && ex.getRequiredType().getSimpleName().equals("UUID")) {
            message += "Un UUID valide est attendu (format: 123e4567-e89b-12d3-a456-426614174000).";
        } else {
            message += "Type attendu: " + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "inconnu");
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // ==========================================
    // VALIDATION DES DONNÉES (@Valid, @NotNull, etc.)
    // ==========================================
    /**
     * Gère les erreurs de validation des DTOs
     * Capture : @NotNull, @NotBlank, @Email, @Size, etc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // ==========================================
    // CONFLIT DE VERSION (verrou optimiste)
    // ==========================================
    /**
     * Deux utilisateurs ont modifié le même événement en même temps.
     * Le second à enregistrer reçoit un 409 et doit recharger la fiche.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Cet événement a été modifié entre-temps par quelqu'un d'autre. Veuillez recharger la page avant de réessayer.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ==========================================
    // CONTRAINTES BASE DE DONNÉES (Clés uniques, etc.)
    // ==========================================
    /**
     * Gère les violations de contraintes de base de données
     * Exemples : Clé unique, clé étrangère, etc.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "Erreur d'intégrité des données. ";

        if (ex.getMessage().contains("unique constraint") ||
                ex.getMessage().contains("duplicate key")) {
            message += "Cette valeur existe déjà dans la base de données.";
        } else if (ex.getMessage().contains("foreign key")) {
            message += "Impossible de supprimer car des données liées existent.";
        } else {
            message += "Vérifiez les données saisies.";
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ==========================================
    // FICHIER TROP VOLUMINEUX
    // ==========================================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "Fichier trop volumineux. Taille maximale autorisée : 10 MB",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    // ==========================================
    // ERREURS D'ENTRÉE/SORTIE (Fichiers)
    // ==========================================
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        log.error("❌ IOException : {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erreur lors de la manipulation du fichier",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ==========================================
    // ARGUMENT ILLÉGAL (Paramètres invalides)
    // ==========================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("⚠ IllegalArgumentException : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Paramètre invalide",
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // ==========================================
    // NULL POINTER EXCEPTION (Erreur de code)
    // ==========================================
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException ex) {
        log.error("❌ NullPointerException détectée", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erreur interne : une valeur requise est manquante",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ==========================================
    // EXCEPTION GÉNÉRIQUE (Catch-all)
    // ==========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("❌ Exception non gérée : " + ex.getClass().getName(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Une erreur inattendue s'est produite. Veuillez réessayer.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}