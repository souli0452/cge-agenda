package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "file")
public class File extends AuditEntity {

    @Column(name = "fileName", nullable = false)
    private String fileName;

    @Column(name = "fileLink",nullable = false)
    private String fileLink;

    @Column(name = "fileType")
    private String fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

}
