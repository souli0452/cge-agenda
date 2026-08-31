package gov.bf.ascelc.cge_agenda.service;

import java.util.List;
import java.util.Set;

public interface PermissionService {

    /**
     * Permissions résolues pour l'utilisateur actuellement authentifié = union des
     * permissions accordées à chacun de ses rôles Keycloak (table role_permission).
     */
    Set<String> getPermissionsCourantes();

    boolean aLaPermission(String permissionCle);

    /**
     * Rôles Keycloak connus du système de permissions (tous les rôles ayant au moins
     * une ligne role_permission, utilisé par l'écran admin pour construire la matrice).
     */
    List<String> getRolesConnus();

    /**
     * Permissions accordées à un rôle donné.
     */
    Set<String> getPermissionsDuRole(String roleName);

    /**
     * Remplace l'ensemble des permissions d'un rôle par la liste donnée (admin only).
     */
    void definirPermissionsDuRole(String roleName, Set<String> permissionCles);
}
