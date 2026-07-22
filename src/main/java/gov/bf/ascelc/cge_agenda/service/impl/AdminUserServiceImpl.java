package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.KcRoleDto;
import gov.bf.ascelc.cge_agenda.dto.KeycloakUserDto;
import gov.bf.ascelc.cge_agenda.dto.UserPayloadDto;
import gov.bf.ascelc.cge_agenda.service.AdminUserService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
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

    @Value("${keycloak.admin.realm}")
    private String realmName;

    private RealmResource realm() {
        return keycloakAdminClient.realm(realmName);
    }

    // ==========================================
    // UTILISATEURS
    // ==========================================

    @Override
    public List<KeycloakUserDto> getUsers() {
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

        Response response = realm().users().create(user);
        if (response.getStatus() != 201) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(response.getStatus()),
                    "Impossible de créer l'utilisateur dans Keycloak"
            );
        }
        String userId = CreatedResponseUtil.getCreatedId(response);

        if (payload.getPassword() != null && !payload.getPassword().isBlank()) {
            setPassword(userId, payload.getPassword(), true);
        }
        if (payload.getRole() != null && !payload.getRole().isBlank()) {
            assignRole(userId, payload.getRole());
        }

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

        userResource.update(user);

        if (payload.getRole() != null && !payload.getRole().isBlank()) {
            assignRole(id, payload.getRole());
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
                .build();
    }
}
