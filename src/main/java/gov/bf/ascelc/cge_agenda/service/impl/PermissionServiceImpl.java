package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.entities.Permission;
import gov.bf.ascelc.cge_agenda.entities.RolePermission;
import gov.bf.ascelc.cge_agenda.repository.PermissionRepository;
import gov.bf.ascelc.cge_agenda.repository.RolePermissionRepository;
import gov.bf.ascelc.cge_agenda.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<String> getPermissionsCourantes() {
        List<String> roles = currentRoles();
        if (roles.isEmpty()) {
            return Set.of();
        }
        return rolePermissionRepository.findByRoleNameIn(roles).stream()
                .map(rp -> rp.getPermission().getCle())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public boolean aLaPermission(String permissionCle) {
        return getPermissionsCourantes().contains(permissionCle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRolesConnus() {
        return rolePermissionRepository.findAll().stream()
                .map(RolePermission::getRoleName)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getPermissionsDuRole(String roleName) {
        return rolePermissionRepository.findByRoleName(roleName).stream()
                .map(rp -> rp.getPermission().getCle())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    @Transactional
    public void definirPermissionsDuRole(String roleName, Set<String> permissionCles) {
        rolePermissionRepository.deleteByRoleName(roleName);
        for (String cle : permissionCles) {
            Permission permission = permissionRepository.findByCle(cle)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission inconnue : " + cle));
            rolePermissionRepository.save(RolePermission.builder()
                    .roleName(roleName)
                    .permission(permission)
                    .build());
        }
    }

    private List<String> currentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .filter(seen::add)
                .toList();
    }
}
