package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.OrgConfigDto;
import gov.bf.ascelc.cge_agenda.entities.OrgConfig;
import gov.bf.ascelc.cge_agenda.repository.OrgConfigRepository;
import gov.bf.ascelc.cge_agenda.service.OrgConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrgConfigServiceImpl implements OrgConfigService {

    private final OrgConfigRepository orgConfigRepository;

    private static final Map<String, String> TEMPLATE_LABELS = new LinkedHashMap<>();
    static {
        TEMPLATE_LABELS.put("invitation", "Invitation à un événement");
        TEMPLATE_LABELS.put("validation-request", "Demande de validation");
        TEMPLATE_LABELS.put("validated", "Événement validé");
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
        config.setSubjectValidated(dto.getSubjectValidated());
        config.setSubjectRejected(dto.getSubjectRejected());
        config.setSubjectChangesRequested(dto.getSubjectChangesRequested());
        config.setSubjectAmendmentsCorrected(dto.getSubjectAmendmentsCorrected());
        config.setSubjectCancellation(dto.getSubjectCancellation());
        config.setSubjectPostponement(dto.getSubjectPostponement());
        config.setSubjectEventUpdate(dto.getSubjectEventUpdate());
        config.setSubjectReminder(dto.getSubjectReminder());
        config.setSubjectDelegation(dto.getSubjectDelegation());
        return toDto(orgConfigRepository.save(config));
    }

    @Override
    @Transactional
    public String previewTemplate(String templateKey) {
        if (!TEMPLATE_LABELS.containsKey(templateKey)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modèle d'email inconnu : " + templateKey);
        }
        OrgConfig config = findOrCreateConfig();
        String subject = subjectFor(config, templateKey);
        String primary = config.getCouleurPrimaire() != null ? config.getCouleurPrimaire() : "#009640";
        String orgName = config.getNomOrganisation() != null ? config.getNomOrganisation() : "ASCE-LC";

        return "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:0 auto;border:1px solid #e0e0e0;border-radius:8px;overflow:hidden\">"
                + "<div style=\"background:" + primary + ";color:#fff;padding:20px 24px\">"
                + "<strong style=\"font-size:18px\">" + orgName + "</strong>"
                + "</div>"
                + "<div style=\"padding:24px\">"
                + "<h2 style=\"margin-top:0;color:" + primary + "\">" + TEMPLATE_LABELS.get(templateKey) + "</h2>"
                + "<p style=\"color:#555;font-size:14px\">Objet : <strong>" + escapeHtml(subject) + "</strong></p>"
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:16px 0\">"
                + "<p style=\"color:#333\">Ceci est un aperçu illustratif du modèle « " + TEMPLATE_LABELS.get(templateKey)
                + " ». Le contenu réel de l'email inclura les informations de l'événement concerné.</p>"
                + "</div>"
                + "</div>";
    }

    private String subjectFor(OrgConfig config, String templateKey) {
        return switch (templateKey) {
            case "invitation" -> config.getSubjectInvitation();
            case "validation-request" -> config.getSubjectValidationRequest();
            case "validated" -> config.getSubjectValidated();
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
                                .subjectValidated("Événement validé : {evenement}")
                                .subjectRejected("Événement rejeté : {evenement}")
                                .subjectChangesRequested("Corrections demandées : {evenement}")
                                .subjectAmendmentsCorrected("Corrections apportées : {evenement}")
                                .subjectCancellation("Annulation : {evenement}")
                                .subjectPostponement("Report : {evenement}")
                                .subjectEventUpdate("Mise à jour : {evenement}")
                                .subjectReminder("Rappel : {evenement}")
                                .subjectDelegation("Délégation : {evenement}")
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
                .subjectValidated(c.getSubjectValidated())
                .subjectRejected(c.getSubjectRejected())
                .subjectChangesRequested(c.getSubjectChangesRequested())
                .subjectAmendmentsCorrected(c.getSubjectAmendmentsCorrected())
                .subjectCancellation(c.getSubjectCancellation())
                .subjectPostponement(c.getSubjectPostponement())
                .subjectEventUpdate(c.getSubjectEventUpdate())
                .subjectReminder(c.getSubjectReminder())
                .subjectDelegation(c.getSubjectDelegation())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
