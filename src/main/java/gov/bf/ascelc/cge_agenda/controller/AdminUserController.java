package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.KcRoleDto;
import gov.bf.ascelc.cge_agenda.dto.KeycloakUserDto;
import gov.bf.ascelc.cge_agenda.dto.UserPayloadDto;
import gov.bf.ascelc.cge_agenda.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(ADMIN_ROOT_URL)
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping(ADMIN_USERS)
    public ResponseEntity<List<KeycloakUserDto>> getUsers() {
        return ResponseEntity.ok(adminUserService.getUsers());
    }

    @PostMapping(ADMIN_USERS)
    public ResponseEntity<KeycloakUserDto> createUser(@RequestBody UserPayloadDto payload) {
        return ResponseEntity.ok(adminUserService.createUser(payload));
    }

    @PutMapping(ADMIN_USER_BY_ID)
    public ResponseEntity<KeycloakUserDto> updateUser(@PathVariable String id, @RequestBody UserPayloadDto payload) {
        return ResponseEntity.ok(adminUserService.updateUser(id, payload));
    }

    @PatchMapping(ADMIN_USER_STATUS)
    public ResponseEntity<Void> setUserStatus(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        adminUserService.setUserStatus(id, Boolean.TRUE.equals(body.get("enabled")));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(ADMIN_USER_RESET_PASSWORD)
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable String id) {
        String temporaryPassword = adminUserService.resetPassword(id);
        return ResponseEntity.ok(Map.of("temporaryPassword", temporaryPassword));
    }

    @DeleteMapping(ADMIN_USER_BY_ID)
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ADMIN_ROLES)
    public ResponseEntity<List<KcRoleDto>> getRoles() {
        return ResponseEntity.ok(adminUserService.getRoles());
    }

    @PostMapping(ADMIN_ROLES)
    public ResponseEntity<KcRoleDto> createRole(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminUserService.createRole(body.get("name"), body.get("description")));
    }

    @DeleteMapping(ADMIN_ROLE_BY_NAME)
    public ResponseEntity<Void> deleteRole(@PathVariable String roleName) {
        adminUserService.deleteRole(roleName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ADMIN_USER_ROLES)
    public ResponseEntity<List<String>> getUserRoles(@PathVariable String userId) {
        return ResponseEntity.ok(adminUserService.getUserRoles(userId));
    }

    @PostMapping(ADMIN_USER_ROLE_BY_NAME)
    public ResponseEntity<Void> assignRole(@PathVariable String userId, @PathVariable String roleName) {
        adminUserService.assignRole(userId, roleName);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(ADMIN_USER_ROLE_BY_NAME)
    public ResponseEntity<Void> removeRole(@PathVariable String userId, @PathVariable String roleName) {
        adminUserService.removeRole(userId, roleName);
        return ResponseEntity.noContent().build();
    }
}
