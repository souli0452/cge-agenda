package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.entities.BackupConfig;
import gov.bf.ascelc.cge_agenda.repository.BackupConfigRepository;
import gov.bf.ascelc.cge_agenda.service.BackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupSchedulerTest {

    @Mock
    private BackupConfigRepository backupConfigRepository;

    @Mock
    private BackupService backupService;

    private BackupScheduler scheduler;

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @BeforeEach
    void setUp() {
        scheduler = new BackupScheduler(backupConfigRepository, backupService);
    }

    private void setClockAt(LocalDateTime dateTime) {
        Clock fixed = Clock.fixed(dateTime.atZone(ZONE).toInstant(), ZONE);
        ReflectionTestUtils.setField(scheduler, "clock", fixed);
    }

    private BackupConfig configAt(int hour, int minute) {
        return BackupConfig.builder()
                .autoEnabled(true)
                .backupHour(hour)
                .backupMinute(minute)
                .retentionCount(30)
                .build();
    }

    @Test
    void neSauvegardePasSiDesactive() {
        setClockAt(LocalDateTime.of(2026, 9, 16, 2, 0));
        BackupConfig config = configAt(2, 0);
        config.setAutoEnabled(false);
        when(backupConfigRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(config));

        scheduler.checkAndRunScheduledBackup();

        verify(backupService, never()).runScheduledBackup();
    }

    @Test
    void neSauvegardePasAvantLHeurePlanifiee() {
        setClockAt(LocalDateTime.of(2026, 9, 16, 1, 59));
        when(backupConfigRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configAt(2, 0)));

        scheduler.checkAndRunScheduledBackup();

        verify(backupService, never()).runScheduledBackup();
    }

    @Test
    void declencheLaSauvegardePileALHeurePlanifieeSiAucuneAujourdhui() {
        setClockAt(LocalDateTime.of(2026, 9, 16, 2, 0));
        when(backupConfigRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configAt(2, 0)));
        when(backupService.lastBackupAt()).thenReturn(Optional.empty());

        scheduler.checkAndRunScheduledBackup();

        verify(backupService).runScheduledBackup();
    }

    @Test
    void rattrapeLaSauvegardeSiLAppEtaitIndisponiblePileALHeure() {
        // L'app redémarre à 02:07 : l'heure planifiée (02:00) est déjà passée,
        // et aucune sauvegarde n'a encore eu lieu aujourd'hui après 02:00.
        setClockAt(LocalDateTime.of(2026, 9, 16, 2, 7));
        when(backupConfigRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configAt(2, 0)));
        when(backupService.lastBackupAt()).thenReturn(Optional.of(LocalDateTime.of(2026, 9, 15, 2, 0)));

        scheduler.checkAndRunScheduledBackup();

        verify(backupService).runScheduledBackup();
    }

    @Test
    void neRelancePasSiUneSauvegardeADejaEuLieuAujourdhuiApresLHeure() {
        setClockAt(LocalDateTime.of(2026, 9, 16, 2, 30));
        when(backupConfigRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configAt(2, 0)));
        when(backupService.lastBackupAt()).thenReturn(Optional.of(LocalDateTime.of(2026, 9, 16, 2, 0)));

        scheduler.checkAndRunScheduledBackup();

        verify(backupService, never()).runScheduledBackup();
    }
}
