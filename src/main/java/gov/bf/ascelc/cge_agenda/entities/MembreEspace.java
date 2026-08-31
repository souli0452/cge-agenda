package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.enums.MembreEspaceRole;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceStatut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Affectation d'un gestionnaire (secrétaire, protocole) à l'espace d'un chef. Le
 * cloisonnement d'accès aux événements repose entièrement sur cette table : un
 * gestionnaire n'a accès qu'aux espaces où il figure ici avec le statut ACTIF.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "membre_espace", uniqueConstraints = @UniqueConstraint(
        name = "uk_membre_espace", columnNames = {"espace_id", "membre_email"}))
public class MembreEspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espace_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_membre_espace_espace"))
    private Espace espace;

    @Column(name = "membre_email", nullable = false, length = 255)
    private String membreEmail;

    @Column(name = "membre_nom", length = 200)
    private String membreNom;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MembreEspaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private MembreEspaceStatut statut = MembreEspaceStatut.INVITE;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "invited_by_email", length = 255)
    private String invitedByEmail;
}
