package gov.bf.ascelc.cge_agenda.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions
 * Capture TOUTES les exceptions de l'application et retourne des réponses HTTP appropriées
 */
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
    // CONTRAINTES BASE DE DONNÉES (Clés uniques, etc.)
    // ==========================================
    /**
     * Gère les violations de contraintes de base de données
     * Exemples : Clé unique, clé étrangère, etc.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "Erreur d'intégrité des données. ";

        // Identifier le type d'erreur
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
    /**
     * Gère les erreurs de taille de fichier dépassée
     * Limite définie dans application.properties
     */
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
    /**
     * Gère les erreurs de lecture/écriture de fichiers
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erreur lors de la manipulation du fichier : " + ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ==========================================
    // ARGUMENT ILLÉGAL (Paramètres invalides)
    // ==========================================
    /**
     * Gère les erreurs d'arguments invalides
     * Exemple : Enum invalide, paramètre null inattendu
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Paramètre invalide : " + ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // ==========================================
    // NULL POINTER EXCEPTION (Erreur de code)
    // ==========================================
    /**
     * Gère les NullPointerException (ne devrait pas arriver en production)
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException ex) {
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
    /**
     * Capture TOUTES les autres exceptions non gérées
     * C'est le filet de sécurité final
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Log l'erreur pour debugging
        System.err.println("Exception non gérée : " + ex.getClass().getName());
        ex.printStackTrace();

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Une erreur inattendue s'est produite. Veuillez réessayer.",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
