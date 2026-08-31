package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.KcRoleDto;
import gov.bf.ascelc.cge_agenda.dto.KeycloakUserDto;
import gov.bf.ascelc.cge_agenda.dto.UserPayloadDto;

import java.util.List;

public interface AdminUserService {

    List<KeycloakUserDto> getUsers();

    KeycloakUserDto createUser(UserPayloadDto payload);

    KeycloakUserDto updateUser(String id, UserPayloadDto payload);

    void setUserStatus(String id, boolean enabled);

    /**
     * Génère un mot de passe temporaire aléatoire pour l'utilisateur et le retourne
     * (à communiquer à l'utilisateur — il ne pourra plus être récupéré ensuite).
     */
    String resetPassword(String id);

    void deleteUser(String id);

    List<KcRoleDto> getRoles();

    KcRoleDto createRole(String name, String description);

    void deleteRole(String roleName);

    List<String> getUserRoles(String userId);

    void assignRole(String userId, String roleName);

    void removeRole(String userId, String roleName);
}
