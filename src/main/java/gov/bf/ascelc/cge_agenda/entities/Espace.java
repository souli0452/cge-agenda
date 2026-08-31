package gov.bf.ascelc.cge_agenda.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Espace agenda cloisonné d'un chef : il en est propriétaire (tous les droits, y compris
 * créer ses propres événements directement confirmés) et peut y désigner des gestionnaires
 * (MembreEspace) qui n'ont accès qu'à cet espace. Un chef = un espace.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "espace")
public class Espace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nom", nullable = false, length = 200)
    private String nom;

    @Column(name = "chef_email", nullable = false, unique = true, length = 255)
    private String chefEmail;

    @Column(name = "chef_nom", length = 200)
    private String chefNom;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
