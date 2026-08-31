package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.KcRoleDto;
import gov.bf.ascelc.cge_agenda.dto.PermissionDto;
import gov.bf.ascelc.cge_agenda.service.AdminUserService;
import gov.bf.ascelc.cge_agenda.service.PermissionService;
import gov.bf.ascelc.cge_agenda.utils.PermissionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

/**
 * Écran admin "Rôles & permissions" : les rôles Keycloak restent dynamiques
 * (AdminUserService.getRoles/createRole), ce contrôleur ne fait que décider quelles
 * permissions chacun de ces rôles accorde (role_permission, éditable sans redéploiement).
 */
@RestController
@RequestMapping(ADMIN_ROOT_URL)
@RequiredArgsConstructor
public class RolePermissionController {

    private final PermissionService permissionService;
    private final AdminUserService adminUserService;

    @GetMapping(ADMIN_PERMISSIONS_CATALOG)
    public ResponseEntity<List<PermissionDto>> getCatalogue() {
        List<PermissionDto> catalogue = PermissionCatalog.LABELS.entrySet().stream()
                .map(e -> PermissionDto.builder().cle(e.getKey()).description(e.getValue()).build())
                .toList();
        return ResponseEntity.ok(catalogue);
    }

    /**
     * Matrice rôle → permissions accordées, pour tous les rôles Keycloak existants
     * (même ceux sans aucune permission configurée pour l'instant).
     */
    @GetMapping(ADMIN_PERMISSIONS_ROLES)
    public ResponseEntity<Map<String, Set<String>>> getMatrice() {
        List<String> roles = adminUserService.getRoles().stream().map(KcRoleDto::getName).toList();
        Map<String, Set<String>> matrice = new java.util.LinkedHashMap<>();
        for (String role : roles) {
            matrice.put(role, permissionService.getPermissionsDuRole(role));
        }
        return ResponseEntity.ok(matrice);
    }

    @PutMapping(ADMIN_PERMISSIONS_BY_ROLE)
    public ResponseEntity<Void> definirPermissions(
            @PathVariable String roleName,
            @RequestBody Set<String> permissionCles
    ) {
        permissionService.definirPermissionsDuRole(roleName, permissionCles);
        return ResponseEntity.noContent().build();
    }
}
