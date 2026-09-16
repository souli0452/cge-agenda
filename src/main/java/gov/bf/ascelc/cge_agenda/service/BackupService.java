package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.BackupConfigDto;
import gov.bf.ascelc.cge_agenda.dto.BackupInfoDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BackupService {

    List<BackupInfoDto> list();

    BackupInfoDto create();

    String restore(String filename);

    void delete(String filename);

    byte[] download(String filename);

    BackupConfigDto getConfig();

    BackupConfigDto saveConfig(BackupConfigDto dto);

    List<BackupInfoDto> getCorbeille();

    void restoreFromCorbeille(String filename);

    void deletePermanently(String filename);

    /**
     * Exécute une sauvegarde automatique et purge les sauvegardes AUTO
     * excédant la rétention configurée. Appelé par le planificateur.
     */
    void runScheduledBackup();

    /**
     * Date/heure de la sauvegarde la plus récente (manuelle ou automatique),
     * vide si aucune sauvegarde n'existe. Utilisé par le planificateur pour
     * savoir si la sauvegarde du jour a déjà eu lieu.
     */
    Optional<LocalDateTime> lastBackupAt();
}
