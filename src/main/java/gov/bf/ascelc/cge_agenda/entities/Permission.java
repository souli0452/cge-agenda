package gov.bf.ascelc.cge_agenda.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Catalogue fixe des capacités que l'application sait vérifier (ex. EVENT_VALIDATE,
 * ADMIN_CONFIG). La liste des clés est définie en code (PermissionCatalog) ; ce qui est
 * dynamique, c'est la table RolePermission qui décide quel rôle Keycloak donne quelle clé.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cle", nullable = false, unique = true, length = 60)
    private String cle;

    @Column(name = "description", length = 255)
    private String description;
}
