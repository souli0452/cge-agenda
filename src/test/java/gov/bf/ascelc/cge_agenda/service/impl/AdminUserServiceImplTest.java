package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.UserPayloadDto;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un utilisateur n'a qu'un seul rôle métier "actif" du point de vue de l'écran
 * d'édition (un unique <select>) : changer ce rôle doit REMPLACER l'ancien,
 * pas l'accumuler. Sans ce garde-fou, l'utilisateur se retrouve avec les deux
 * rôles assignés côté Keycloak, et l'affichage montre parfois encore l'ancien
 * rôle (premier trouvé dans la liste, sans ordre garanti).
 */
class AdminUserServiceImplTest {

    private Keycloak keycloak;
    private UserResource userResource;
    private RoleMappingResource roleMappingResource;
    private RoleScopeResource roleScopeResource;
    private RolesResource rolesResource;
    private AdminUserServiceImpl service;

    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        keycloak = mock(Keycloak.class);
        EmailService emailService = mock(EmailService.class);
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        userResource = mock(UserResource.class);
        roleMappingResource = mock(RoleMappingResource.class);
        roleScopeResource = mock(RoleScopeResource.class);
        rolesResource = mock(RolesResource.class);

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(new UserRepresentation());
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(realmResource.roles()).thenReturn(rolesResource);

        service = new AdminUserServiceImpl(keycloak, emailService);
        ReflectionTestUtils.setField(service, "realmName", "asce-lc-realm");
        ReflectionTestUtils.setField(service, "defaultPassword", "Asce@2026");
    }

    private void mockRoleLookup(String name) {
        RoleResource roleResource = mock(RoleResource.class);
        RoleRepresentation rep = new RoleRepresentation();
        rep.setName(name);
        when(roleResource.toRepresentation()).thenReturn(rep);
        when(rolesResource.get(name)).thenReturn(roleResource);
    }

    private UserPayloadDto payloadWithRole(String role) {
        UserPayloadDto dto = new UserPayloadDto();
        dto.setUsername("j.dupont");
        dto.setEmail("j.dupont@ascelc.bf");
        dto.setFirstName("Jean");
        dto.setLastName("Dupont");
        dto.setEnabled(true);
        dto.setRole(role);
        return dto;
    }

    @Test
    void updateUser_whenRoleChanges_removesThePreviousBusinessRoleBeforeAssigningTheNewOne() {
        RoleRepresentation existingUserRole = new RoleRepresentation();
        existingUserRole.setName("USER");
        when(roleScopeResource.listAll()).thenReturn(List.of(existingUserRole));
        mockRoleLookup("ADMIN");

        service.updateUser(USER_ID, payloadWithRole("ADMIN"));

        ArgumentCaptor<List<RoleRepresentation>> removed = ArgumentCaptor.forClass(List.class);
        verify(roleScopeResource).remove(removed.capture());
        assertThat(removed.getValue()).extracting(RoleRepresentation::getName).containsExactly("USER");

        ArgumentCaptor<List<RoleRepresentation>> added = ArgumentCaptor.forClass(List.class);
        verify(roleScopeResource).add(added.capture());
        assertThat(added.getValue()).extracting(RoleRepresentation::getName).containsExactly("ADMIN");
    }

    @Test
    void updateUser_whenRoleUnchanged_doesNotRemoveOrReassignIt() {
        RoleRepresentation existingUserRole = new RoleRepresentation();
        existingUserRole.setName("ADMIN");
        when(roleScopeResource.listAll()).thenReturn(List.of(existingUserRole));
        mockRoleLookup("ADMIN");

        service.updateUser(USER_ID, payloadWithRole("ADMIN"));

        verify(roleScopeResource, never()).remove(any());
        verify(roleScopeResource, never()).add(any());
    }

    @Test
    void updateUser_ignoresTechnicalKeycloakRolesWhenReplacing() {
        RoleRepresentation defaultRoles = new RoleRepresentation();
        defaultRoles.setName("default-roles-asce-lc-realm");
        RoleRepresentation offlineAccess = new RoleRepresentation();
        offlineAccess.setName("offline_access");
        RoleRepresentation businessRole = new RoleRepresentation();
        businessRole.setName("SECRETAIRE");
        when(roleScopeResource.listAll()).thenReturn(List.of(defaultRoles, offlineAccess, businessRole));
        mockRoleLookup("PROTOCOLE");

        service.updateUser(USER_ID, payloadWithRole("PROTOCOLE"));

        ArgumentCaptor<List<RoleRepresentation>> removed = ArgumentCaptor.forClass(List.class);
        verify(roleScopeResource).remove(removed.capture());
        assertThat(removed.getValue()).extracting(RoleRepresentation::getName).containsExactly("SECRETAIRE");
    }
}
