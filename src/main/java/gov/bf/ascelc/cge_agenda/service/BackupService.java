package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.BackupConfigDto;
import gov.bf.ascelc.cge_agenda.dto.BackupInfoDto;

import java.util.List;

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
}
