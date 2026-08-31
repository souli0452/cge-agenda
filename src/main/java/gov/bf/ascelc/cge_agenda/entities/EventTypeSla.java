package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_type_sla")
public class EventTypeSla {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, unique = true, length = 50)
    private EventType eventType;

    @Column(name = "delai_heures_ouvrables", nullable = false)
    private Integer delaiHeuresOuvrables;

    @Column(name = "delai_avant_evenement_heures", nullable = false)
    private Integer delaiAvantEvenementHeures;
}
