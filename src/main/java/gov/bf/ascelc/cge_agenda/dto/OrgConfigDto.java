package gov.bf.ascelc.cge_agenda.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgConfigDto {

    private UUID id;
    private String nomOrganisation;
    private String slogan;
    private String emailExpediteurNom;
    private String couleurPrimaire;
    private String logoUrl;
    private String adresse;
    private String siteWeb;

    private String subjectInvitation;
    private String subjectValidationRequest;
    private String subjectNewDocument;
    private String subjectRejected;
    private String subjectChangesRequested;
    private String subjectAmendmentsCorrected;
    private String subjectCancellation;
    private String subjectPostponement;
    private String subjectEventUpdate;
    private String subjectReminder;
    private String subjectDelegation;
    private String subjectEventValidatedCreator;
    private String subjectEventValidatedProtocole;
    private java.time.LocalTime heureDebutOuvrable;
    private java.time.LocalTime heureFinOuvrable;

    private String bodyInvitation;
    private String bodyValidationRequest;
    private String bodyNewDocument;
    private String bodyRejected;
    private String bodyChangesRequested;
    private String bodyAmendmentsCorrected;
    private String bodyCancellation;
    private String bodyPostponement;
    private String bodyEventUpdate;
    private String bodyReminder;
    private String bodyDelegation;
    private String bodyEventValidatedCreator;
    private String bodyEventValidatedProtocole;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd' 'HH:mm:ss")
    private LocalDateTime updatedAt;
}
