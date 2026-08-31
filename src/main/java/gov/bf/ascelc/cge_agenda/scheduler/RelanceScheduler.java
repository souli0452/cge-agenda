package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.JourFerieRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import gov.bf.ascelc.cge_agenda.service.OrgConfigService;
import gov.bf.ascelc.cge_agenda.utils.BusinessHoursCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Relances échelonnées pour les événements en attente de validation : 50%/80%/100% du
 * délai (CGE), puis échéance+24h et toutes les 24h ensuite (créateur + protocole en plus).
 * Une seule instance de l'application (pas de ShedLock) : dédupe en mémoire, réinitialisée
 * naturellement à chaque resoumission puisque la clé inclut l'échéance courante.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelanceScheduler {

    private final EventRepository eventRepository;
    private final JourFerieRepository jourFerieRepository;
    private final EmailService emailService;
    private final OrgConfigService orgConfigService;

    private final Set<String> relancesEnvoyees = ConcurrentHashMap.newKeySet();

    @Scheduled(cron = "0 0 * * * *")
    public void verifierRelances() {
        LocalDateTime now = LocalDateTime.now();
        Set<LocalDate> feries = jourFerieRepository.findAllByOrderByDateAsc().stream()
                .map(gov.bf.ascelc.cge_agenda.entities.JourFerie::getDate)
                .collect(Collectors.toSet());

        var orgConfig = orgConfigService.getConfig();
        var heureDebut = orgConfig.getHeureDebutOuvrable() != null
                ? orgConfig.getHeureDebutOuvrable() : BusinessHoursCalculator.DEFAULT_BUSINESS_START;
        var heureFin = orgConfig.getHeureFinOuvrable() != null
                ? orgConfig.getHeureFinOuvrable() : BusinessHoursCalculator.DEFAULT_BUSINESS_END;

        if (!BusinessHoursCalculator.isBusinessMoment(now, feries, heureDebut, heureFin)) {
            return;
        }

        List<Event> events = eventRepository.findByStatusAndEcheanceValidationIsNotNull(EventStatus.EN_ATTENTE_VALIDATION);
        log.info("=== Vérification relances validation : {} événement(s) en attente ===", events.size());

        for (Event event : events) {
            try {
                traiterEvent(event, now);
            } catch (Exception e) {
                log.error("❌ Erreur relance pour '{}' : {}", event.getTitle(), e.getMessage());
            }
        }
    }

    private void traiterEvent(Event event, LocalDateTime now) {
        LocalDateTime soumisLe = event.getSoumisLe();
        LocalDateTime echeance = event.getEcheanceValidation();
        if (soumisLe == null || echeance == null) {
            return;
        }

        String base = event.getId() + "|" + echeance;

        if (now.isBefore(echeance)) {
            long totalMinutes = Duration.between(soumisLe, echeance).toMinutes();
            long ecouleesMinutes = Duration.between(soumisLe, now).toMinutes();
            double pct = totalMinutes <= 0 ? 100 : (100.0 * ecouleesMinutes / totalMinutes);

            if (pct >= 80) {
                envoyerSiNouveau(base + "|80", () -> emailService.sendRelanceValidation(event.getId(), 80));
            } else if (pct >= 50) {
                envoyerSiNouveau(base + "|50", () -> emailService.sendRelanceValidation(event.getId(), 50));
            }
            return;
        }

        // Échéance atteinte ou dépassée
        envoyerSiNouveau(base + "|100", () -> emailService.sendRelanceValidation(event.getId(), 100));

        long heuresDepassement = Duration.between(echeance, now).toHours();
        if (heuresDepassement >= 24) {
            long palierEscalade = heuresDepassement / 24; // 1, 2, 3... toutes les 24h
            envoyerSiNouveau(base + "|escalade-" + palierEscalade,
                    () -> emailService.sendRelanceValidationEscalade(event.getId()));
        }
    }

    private void envoyerSiNouveau(String key, Runnable action) {
        if (relancesEnvoyees.add(key)) {
            action.run();
        }
    }
}
