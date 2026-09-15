package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "org_config")
public class OrgConfig extends AuditEntity {

    @Column(name = "nom_organisation")
    private String nomOrganisation;

    @Column(name = "slogan")
    private String slogan;

    @Column(name = "email_expediteur_nom")
    private String emailExpediteurNom;

    @Column(name = "couleur_primaire")
    private String couleurPrimaire;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "site_web")
    private String siteWeb;

    @Column(name = "subject_invitation")
    private String subjectInvitation;

    @Column(name = "subject_validation_request")
    private String subjectValidationRequest;

    @Column(name = "subject_new_document")
    private String subjectNewDocument;

    @Column(name = "subject_rejected")
    private String subjectRejected;

    @Column(name = "subject_changes_requested")
    private String subjectChangesRequested;

    @Column(name = "subject_amendments_corrected")
    private String subjectAmendmentsCorrected;

    @Column(name = "subject_cancellation")
    private String subjectCancellation;

    @Column(name = "subject_postponement")
    private String subjectPostponement;

    @Column(name = "subject_event_update")
    private String subjectEventUpdate;

    @Column(name = "subject_reminder")
    private String subjectReminder;

    @Column(name = "subject_delegation")
    private String subjectDelegation;

    @Column(name = "subject_event_validated_creator")
    private String subjectEventValidatedCreator;

    @Column(name = "subject_event_validated_protocole")
    private String subjectEventValidatedProtocole;

    /** Plage horaire ouvrée utilisée pour le calcul des échéances de validation et des relances. */
    @Column(name = "heure_debut_ouvrable")
    private java.time.LocalTime heureDebutOuvrable;

    @Column(name = "heure_fin_ouvrable")
    private java.time.LocalTime heureFinOuvrable;

    @Column(name = "body_invitation", columnDefinition = "TEXT")
    private String bodyInvitation;

    @Column(name = "body_validation_request", columnDefinition = "TEXT")
    private String bodyValidationRequest;

    @Column(name = "body_new_document", columnDefinition = "TEXT")
    private String bodyNewDocument;

    @Column(name = "body_rejected", columnDefinition = "TEXT")
    private String bodyRejected;

    @Column(name = "body_changes_requested", columnDefinition = "TEXT")
    private String bodyChangesRequested;

    @Column(name = "body_amendments_corrected", columnDefinition = "TEXT")
    private String bodyAmendmentsCorrected;

    @Column(name = "body_cancellation", columnDefinition = "TEXT")
    private String bodyCancellation;

    @Column(name = "body_postponement", columnDefinition = "TEXT")
    private String bodyPostponement;

    @Column(name = "body_event_update", columnDefinition = "TEXT")
    private String bodyEventUpdate;

    @Column(name = "body_reminder", columnDefinition = "TEXT")
    private String bodyReminder;

    @Column(name = "body_delegation", columnDefinition = "TEXT")
    private String bodyDelegation;

    @Column(name = "body_event_validated_creator", columnDefinition = "TEXT")
    private String bodyEventValidatedCreator;

    @Column(name = "body_event_validated_protocole", columnDefinition = "TEXT")
    private String bodyEventValidatedProtocole;
}
