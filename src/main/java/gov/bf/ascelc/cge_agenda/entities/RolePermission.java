package gov.bf.ascelc.cge_agenda.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Association dynamique rôle Keycloak (nom libre, pas d'enum : les rôles peuvent être
 * créés à la volée via AdminUserServiceImpl.createRole) → permission. C'est cette table,
 * éditable depuis l'écran admin "Rôles & permissions", qui rend les droits dynamiques.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "role_permission", uniqueConstraints = @UniqueConstraint(
        name = "uk_role_permission", columnNames = {"role_name", "permission_id"}))
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "role_name", nullable = false, length = 60)
    private String roleName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_role_permission_permission"))
    private Permission permission;
}
