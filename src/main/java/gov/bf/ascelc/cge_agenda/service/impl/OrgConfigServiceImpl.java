package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.OrgConfigDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.OrgConfig;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.repository.OrgConfigRepository;
import gov.bf.ascelc.cge_agenda.service.OrgConfigService;
import gov.bf.ascelc.cge_agenda.utils.EmailTemplateVariables;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrgConfigServiceImpl implements OrgConfigService {

    private final OrgConfigRepository orgConfigRepository;
    private final SpringTemplateEngine templateEngine;

    private static final Map<String, String> TEMPLATE_LABELS = new LinkedHashMap<>();
    static {
        TEMPLATE_LABELS.put("invitation", "Invitation à un événement");
        TEMPLATE_LABELS.put("validation-request", "Demande de validation");
        TEMPLATE_LABELS.put("new-document", "Nouveau document ajouté");
        TEMPLATE_LABELS.put("rejected", "Événement rejeté");
        TEMPLATE_LABELS.put("changes-requested", "Corrections demandées");
        TEMPLATE_LABELS.put("amendments-corrected", "Corrections apportées");
        TEMPLATE_LABELS.put("cancellation", "Annulation");
        TEMPLATE_LABELS.put("postponement", "Report");
        TEMPLATE_LABELS.put("event-update", "Mise à jour d'un événement");
        TEMPLATE_LABELS.put("reminder", "Rappel automatique");
        TEMPLATE_LABELS.put("delegation", "Délégation de participation");
    }

    @Override
    @Transactional
    public OrgConfigDto getConfig() {
        return toDto(findOrCreateConfig());
    }

    @Override
    @Transactional
    public OrgConfigDto updateConfig(OrgConfigDto dto) {
        OrgConfig config = findOrCreateConfig();
        config.setNomOrganisation(dto.getNomOrganisation());
        config.setSlogan(dto.getSlogan());
        config.setEmailExpediteurNom(dto.getEmailExpediteurNom());
        config.setCouleurPrimaire(dto.getCouleurPrimaire());
        config.setLogoUrl(dto.getLogoUrl());
        config.setAdresse(dto.getAdresse());
        config.setSiteWeb(dto.getSiteWeb());
        config.setSubjectInvitation(dto.getSubjectInvitation());
        config.setSubjectValidationRequest(dto.getSubjectValidationRequest());
        config.setSubjectNewDocument(dto.getSubjectNewDocument());
        config.setSubjectRejected(dto.getSubjectRejected());
        config.setSubjectChangesRequested(dto.getSubjectChangesRequested());
        config.setSubjectAmendmentsCorrected(dto.getSubjectAmendmentsCorrected());
        config.setSubjectCancellation(dto.getSubjectCancellation());
        config.setSubjectPostponement(dto.getSubjectPostponement());
        config.setSubjectEventUpdate(dto.getSubjectEventUpdate());
        config.setSubjectReminder(dto.getSubjectReminder());
        config.setSubjectDelegation(dto.getSubjectDelegation());
        config.setBodyInvitation(dto.getBodyInvitation());
        config.setBodyValidationRequest(dto.getBodyValidationRequest());
        config.setBodyNewDocument(dto.getBodyNewDocument());
        config.setBodyRejected(dto.getBodyRejected());
        config.setBodyChangesRequested(dto.getBodyChangesRequested());
        config.setBodyAmendmentsCorrected(dto.getBodyAmendmentsCorrected());
        config.setBodyCancellation(dto.getBodyCancellation());
        config.setBodyPostponement(dto.getBodyPostponement());
        config.setBodyEventUpdate(dto.getBodyEventUpdate());
        config.setBodyReminder(dto.getBodyReminder());
        config.setBodyDelegation(dto.getBodyDelegation());
        return toDto(orgConfigRepository.save(config));
    }

    @Override
    @Transactional
    public String previewTemplate(String templateKey) {
        if (!TEMPLATE_LABELS.containsKey(templateKey)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modèle d'email inconnu : " + templateKey);
        }
        OrgConfig config = findOrCreateConfig();

        Event sampleEvent = Event.builder()
                .title("Réunion de démonstration")
                .type(EventType.REUNION)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(7))
                .ville("Ouagadougou")
                .pays("Burkina Faso")
                .rejectionReason("Motif d'exemple : budget non détaillé")
                .changeSuggestions("Exemple : préciser le lieu exact de la réunion")
                .build();
        Participant sampleParticipant = Participant.builder()
                .firstName("Jean").lastName("Dupont").email("jean.dupont@example.com")
                .build();

        Map<String, String> vars = new HashMap<>();
        vars.put("evenement", sampleEvent.getTitle());
        vars.put("participant", sampleParticipant.getFirstName() + " " + sampleParticipant.getLastName());
        vars.put("date_debut", sampleEvent.getStartDate().toString());
        vars.put("date_fin", sampleEvent.getEndDate().toString());
        vars.put("lieu", sampleEvent.getVille());

        String subject = EmailTemplateVariables.substitute(subjectFor(config, templateKey), vars);
        String customMessage = EmailTemplateVariables.substitute(bodyFor(config, templateKey), vars);

        Context context = new Context(Locale.FRENCH);
        context.setVariable("baseUrl", "https://agenda.asce-lc.bf");
        context.setVariable("apiUrl", "https://agenda.asce-lc.bf");
        context.setVariable("event", sampleEvent);
        context.setVariable("participant", sampleParticipant);
        context.setVariable("file", gov.bf.ascelc.cge_agenda.entities.File.builder()
                .fileName("exemple.pdf").build());
        context.setVariable("heureDebut", "09:00");
        context.setVariable("heureFin", "17:00");
        context.setVariable("customMessage", customMessage);

        String html = templateEngine.process("email/" + templateKey, context);

        return "<div style=\"padding:12px;background:#f0f4f0;\">"
                + "<div style=\"max-width:640px;margin:0 auto 12px;background:#fff3cd;border:1px solid #ffc107;"
                + "border-radius:6px;padding:10px 16px;font-family:Arial,sans-serif;font-size:13px;color:#664d03;\">"
                + "Aperçu avec des données d'exemple — Objet : <strong>" + escapeHtml(subject) + "</strong>"
                + "</div>"
                + html
                + "</div>";
    }

    private String subjectFor(OrgConfig config, String templateKey) {
        return switch (templateKey) {
            case "invitation" -> config.getSubjectInvitation();
            case "validation-request" -> config.getSubjectValidationRequest();
            case "new-document" -> config.getSubjectNewDocument();
            case "rejected" -> config.getSubjectRejected();
            case "changes-requested" -> config.getSubjectChangesRequested();
            case "amendments-corrected" -> config.getSubjectAmendmentsCorrected();
            case "cancellation" -> config.getSubjectCancellation();
            case "postponement" -> config.getSubjectPostponement();
            case "event-update" -> config.getSubjectEventUpdate();
            case "reminder" -> config.getSubjectReminder();
            case "delegation" -> config.getSubjectDelegation();
            default -> TEMPLATE_LABELS.get(templateKey);
        };
    }

    private String bodyFor(OrgConfig config, String templateKey) {
        return switch (templateKey) {
            case "invitation" -> config.getBodyInvitation();
            case "validation-request" -> config.getBodyValidationRequest();
            case "new-document" -> config.getBodyNewDocument();
            case "rejected" -> config.getBodyRejected();
            case "changes-requested" -> config.getBodyChangesRequested();
            case "amendments-corrected" -> config.getBodyAmendmentsCorrected();
            case "cancellation" -> config.getBodyCancellation();
            case "postponement" -> config.getBodyPostponement();
            case "event-update" -> config.getBodyEventUpdate();
            case "reminder" -> config.getBodyReminder();
            case "delegation" -> config.getBodyDelegation();
            default -> "";
        };
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private OrgConfig findOrCreateConfig() {
        return orgConfigRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> orgConfigRepository.save(
                        OrgConfig.builder()
                                .nomOrganisation("ASCE-LC")
                                .slogan("Contrôle Général d'État")
                                .emailExpediteurNom("Agenda ASCE-LC")
                                .couleurPrimaire("#009640")
                                .subjectInvitation("Invitation : {evenement}")
                                .subjectValidationRequest("Demande de validation : {evenement}")
                                .subjectNewDocument("Nouveau document : {evenement}")
                                .subjectRejected("Événement rejeté : {evenement}")
                                .subjectChangesRequested("Corrections demandées : {evenement}")
                                .subjectAmendmentsCorrected("Corrections apportées : {evenement}")
                                .subjectCancellation("Annulation : {evenement}")
                                .subjectPostponement("Report : {evenement}")
                                .subjectEventUpdate("Mise à jour : {evenement}")
                                .subjectReminder("Rappel : {evenement}")
                                .subjectDelegation("Délégation : {evenement}")
                                .bodyInvitation("Vous avez été inscrit(e) à l'événement {evenement}. Veuillez en prendre note dans votre agenda.")
                                .bodyValidationRequest("Un événement a été soumis et attend votre validation.")
                                .bodyNewDocument("Un nouveau document a été ajouté à l'événement {evenement}. Vous pouvez le consulter et le télécharger dès maintenant.")
                                .bodyRejected("Votre événement {evenement} a été rejeté. Le motif est détaillé ci-dessous.")
                                .bodyChangesRequested("Le CGE a demandé des corrections sur votre événement {evenement} avant de pouvoir le valider.")
                                .bodyAmendmentsCorrected("Le créateur a apporté les corrections demandées. L'événement est de nouveau en attente de votre validation.")
                                .bodyCancellation("L'événement {evenement} a été annulé.")
                                .bodyPostponement("L'événement {evenement} a été reporté à une nouvelle date.")
                                .bodyEventUpdate("L'événement {evenement} a été modifié. Veuillez consulter les nouvelles informations ci-dessous et mettre à jour votre agenda.")
                                .bodyReminder("Rappel : l'événement {evenement} approche.")
                                .bodyDelegation("Vous avez été désigné(e) pour représenter le CGE à l'événement suivant :")
                                .build()
                ));
    }

    private OrgConfigDto toDto(OrgConfig c) {
        return OrgConfigDto.builder()
                .id(c.getId())
                .nomOrganisation(c.getNomOrganisation())
                .slogan(c.getSlogan())
                .emailExpediteurNom(c.getEmailExpediteurNom())
                .couleurPrimaire(c.getCouleurPrimaire())
                .logoUrl(c.getLogoUrl())
                .adresse(c.getAdresse())
                .siteWeb(c.getSiteWeb())
                .subjectInvitation(c.getSubjectInvitation())
                .subjectValidationRequest(c.getSubjectValidationRequest())
                .subjectNewDocument(c.getSubjectNewDocument())
                .subjectRejected(c.getSubjectRejected())
                .subjectChangesRequested(c.getSubjectChangesRequested())
                .subjectAmendmentsCorrected(c.getSubjectAmendmentsCorrected())
                .subjectCancellation(c.getSubjectCancellation())
                .subjectPostponement(c.getSubjectPostponement())
                .subjectEventUpdate(c.getSubjectEventUpdate())
                .subjectReminder(c.getSubjectReminder())
                .subjectDelegation(c.getSubjectDelegation())
                .bodyInvitation(c.getBodyInvitation())
                .bodyValidationRequest(c.getBodyValidationRequest())
                .bodyNewDocument(c.getBodyNewDocument())
                .bodyRejected(c.getBodyRejected())
                .bodyChangesRequested(c.getBodyChangesRequested())
                .bodyAmendmentsCorrected(c.getBodyAmendmentsCorrected())
                .bodyCancellation(c.getBodyCancellation())
                .bodyPostponement(c.getBodyPostponement())
                .bodyEventUpdate(c.getBodyEventUpdate())
                .bodyReminder(c.getBodyReminder())
                .bodyDelegation(c.getBodyDelegation())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
