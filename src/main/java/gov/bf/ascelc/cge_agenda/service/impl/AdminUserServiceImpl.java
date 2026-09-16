package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.KcRoleDto;
import gov.bf.ascelc.cge_agenda.dto.KeycloakUserDto;
import gov.bf.ascelc.cge_agenda.dto.UserPayloadDto;
import gov.bf.ascelc.cge_agenda.service.AdminUserService;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final Keycloak keycloakAdminClient;
    private final EmailService emailService;

    @Value("${keycloak.admin.realm}")
    private String realmName;

    @Value("${app.user.default-password:Asce@2026}")
    private String defaultPassword;

    private RealmResource realm() {
        return keycloakAdminClient.realm(realmName);
    }

    // ==========================================
    // UTILISATEURS
    // ==========================================

    @Override
    public List<KeycloakUserDto> getUsers() {
        // L'API admin Keycloak n'offre pas de récupération groupée des rôles par
        // utilisateur : un appel réseau par utilisateur est nécessaire. Séquentiel
        // plutôt que parallèle : le client admin Keycloak partagé (io.keycloak.admin.client.Keycloak)
        // n'est pas garanti pour un usage concurrent illimité, et paralléliser ces
        // appels a déjà provoqué un blocage en production (requête qui ne se termine
        // jamais). Avec le faible nombre d'utilisateurs de l'appli, le coût séquentiel
        // reste négligeable.
        return realm().users().list().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public KeycloakUserDto createUser(UserPayloadDto payload) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(payload.getUsername());
        user.setEmail(payload.getEmail());
        user.setFirstName(payload.getFirstName());
        user.setLastName(payload.getLastName());
        user.setEnabled(payload.isEnabled());
        user.setEmailVerified(true);

        // Le nom/prénom sont déjà renseignés par l'admin : on ne force que le
        // changement du mot de passe temporaire au premier login, pas les actions
        // par défaut du realm (qui incluent "Update Profile" sinon).
        List<String> requiredActions = new ArrayList<>(List.of("UPDATE_PASSWORD"));
        if (payload.isRequireMfa()) {
            requiredActions.add("CONFIGURE_TOTP");
        }
        user.setRequiredActions(requiredActions);

        Response response = realm().users().create(user);
        if (response.getStatus() != 201) {
            HttpStatus status = HttpStatus.valueOf(response.getStatus());
            String message = status == HttpStatus.CONFLICT
                    ? "Un utilisateur avec ce nom d'utilisateur ou cet email existe déjà"
                    : "Impossible de créer l'utilisateur (" + response.getStatus() + ")";
            response.close();
            throw new ResponseStatusException(status, message);
        }
        String userId = CreatedResponseUtil.getCreatedId(response);

        // Toujours un mot de passe temporaire (saisi par l'admin ou, à défaut, une
        // valeur par défaut) — l'utilisateur le change obligatoirement à la première
        // connexion (UPDATE_PASSWORD ci-dessus), et le reçoit par email ci-dessous.
        String motDePasse = (payload.getPassword() != null && !payload.getPassword().isBlank())
                ? payload.getPassword() : defaultPassword;
        setPassword(userId, motDePasse, true);

        if (payload.getRole() != null && !payload.getRole().isBlank()) {
            assignRole(userId, payload.getRole());
        }

        emailService.sendAccountCreatedEmail(payload.getEmail(), payload.getUsername(), motDePasse);

        return toDto(realm().users().get(userId).toRepresentation());
    }

    @Override
    public KeycloakUserDto updateUser(String id, UserPayloadDto payload) {
        UserResource userResource = getUserResourceOrThrow(id);
        UserRepresentation user = userResource.toRepresentation();

        user.setUsername(payload.getUsername());
        user.setEmail(payload.getEmail());
        user.setFirstName(payload.getFirstName());
        user.setLastName(payload.getLastName());
        user.setEnabled(payload.isEnabled());

        List<String> requiredActions = user.getRequiredActions() != null
                ? new ArrayList<>(user.getRequiredActions()) : new ArrayList<>();
        if (payload.isRequireMfa() && !requiredActions.contains("CONFIGURE_TOTP")) {
            requiredActions.add("CONFIGURE_TOTP");
        } else if (!payload.isRequireMfa()) {
            requiredActions.remove("CONFIGURE_TOTP");
        }
        user.setRequiredActions(requiredActions);

        userResource.update(user);

        if (payload.getRole() != null && !payload.getRole().isBlank()) {
            replaceBusinessRole(id, payload.getRole());
        }

        return toDto(userResource.toRepresentation());
    }

    @Override
    public void setUserStatus(String id, boolean enabled) {
        UserResource userResource = getUserResourceOrThrow(id);
        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(enabled);
        userResource.update(user);
    }

    @Override
    public String resetPassword(String id) {
        getUserResourceOrThrow(id);
        String temporaryPassword = generateTemporaryPassword();
        setPassword(id, temporaryPassword, true);
        return temporaryPassword;
    }

    private static final String PWD_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PWD_LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String PWD_DIGIT = "23456789";
    private static final String PWD_SPECIAL = "!@#$%&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateTemporaryPassword() {
        String all = PWD_UPPER + PWD_LOWER + PWD_DIGIT + PWD_SPECIAL;
        StringBuilder sb = new StringBuilder();
        // Garantit au moins un caractère de chaque catégorie
        sb.append(PWD_UPPER.charAt(RANDOM.nextInt(PWD_UPPER.length())));
        sb.append(PWD_LOWER.charAt(RANDOM.nextInt(PWD_LOWER.length())));
        sb.append(PWD_DIGIT.charAt(RANDOM.nextInt(PWD_DIGIT.length())));
        sb.append(PWD_SPECIAL.charAt(RANDOM.nextInt(PWD_SPECIAL.length())));
        for (int i = 0; i < 8; i++) {
            sb.append(all.charAt(RANDOM.nextInt(all.length())));
        }
        // Mélange les caractères pour ne pas avoir un motif prévisible en tête
        List<Character> chars = new ArrayList<>();
        for (char c : sb.toString().toCharArray()) chars.add(c);
        Collections.shuffle(chars, RANDOM);
        StringBuilder shuffled = new StringBuilder();
        chars.forEach(shuffled::append);
        return shuffled.toString();
    }

    @Override
    public void deleteUser(String id) {
        getUserResourceOrThrow(id).remove();
    }

    // ==========================================
    // RÔLES
    // ==========================================

    @Override
    public List<KcRoleDto> getRoles() {
        return realm().roles().list().stream()
                .map(r -> KcRoleDto.builder()
                        .id(r.getId())
                        .name(r.getName())
                        .description(r.getDescription())
                        .build())
                .toList();
    }

    @Override
    public KcRoleDto createRole(String name, String description) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(name);
        role.setDescription(description);
        realm().roles().create(role);

        RoleRepresentation created = realm().roles().get(name).toRepresentation();
        return KcRoleDto.builder()
                .id(created.getId())
                .name(created.getName())
                .description(created.getDescription())
                .build();
    }

    @Override
    public void deleteRole(String roleName) {
        try {
            realm().roles().get(roleName).remove();
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle non trouvé : " + roleName);
        }
    }

    @Override
    public List<String> getUserRoles(String userId) {
        return getUserResourceOrThrow(userId).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .toList();
    }

    /**
     * L'écran d'édition ne propose qu'un seul rôle métier à la fois (un simple
     * <select>) : changer ce rôle doit REMPLACER l'ancien, pas s'ajouter à côté.
     * Sans ça, l'utilisateur accumule les rôles au fil des modifications et
     * l'affichage peut continuer à montrer un ancien rôle (le premier trouvé,
     * sans ordre garanti côté Keycloak).
     */
    private void replaceBusinessRole(String userId, String newRole) {
        RoleScopeResource roleScope = getUserResourceOrThrow(userId).roles().realmLevel();
        List<RoleRepresentation> currentRoles = roleScope.listAll();

        List<RoleRepresentation> toRemove = currentRoles.stream()
                .filter(r -> !isTechnicalRole(r.getName()))
                .filter(r -> !r.getName().equals(newRole))
                .toList();
        if (!toRemove.isEmpty()) {
            roleScope.remove(toRemove);
        }

        boolean alreadyAssigned = currentRoles.stream().anyMatch(r -> r.getName().equals(newRole));
        if (!alreadyAssigned) {
            assignRole(userId, newRole);
        }
    }

    /**
     * Rôles internes créés automatiquement par Keycloak (jamais assignés
     * volontairement par un admin) : à ignorer lors du remplacement du rôle
     * métier d'un utilisateur.
     */
    private boolean isTechnicalRole(String role) {
        return "offline_access".equals(role) ||
               "uma_authorization".equals(role) ||
               role.startsWith("default-roles-");
    }

    @Override
    public void assignRole(String userId, String roleName) {
        RoleRepresentation role = realm().roles().get(roleName).toRepresentation();
        getUserResourceOrThrow(userId).roles().realmLevel().add(List.of(role));
    }

    @Override
    public void removeRole(String userId, String roleName) {
        RoleRepresentation role = realm().roles().get(roleName).toRepresentation();
        getUserResourceOrThrow(userId).roles().realmLevel().remove(List.of(role));
    }

    // ==========================================
    // UTILITAIRES
    // ==========================================

    private UserResource getUserResourceOrThrow(String id) {
        UserResource userResource = realm().users().get(id);
        try {
            userResource.toRepresentation();
        } catch (jakarta.ws.rs.NotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé : " + id);
        }
        return userResource;
    }

    private void setPassword(String userId, String password, boolean temporary) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(temporary);
        realm().users().get(userId).resetPassword(credential);
    }

    private KeycloakUserDto toDto(UserRepresentation user) {
        List<String> realmRoles;
        try {
            realmRoles = realm().users().get(user.getId()).roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .toList();
        } catch (Exception e) {
            realmRoles = List.of();
        }

        return KeycloakUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(Boolean.TRUE.equals(user.isEnabled()))
                .emailVerified(Boolean.TRUE.equals(user.isEmailVerified()))
                .createdTimestamp(user.getCreatedTimestamp())
                .realmRoles(realmRoles)
                .mfaRequired(user.getRequiredActions() != null && user.getRequiredActions().contains("CONFIGURE_TOTP"))
                .build();
    }
}
