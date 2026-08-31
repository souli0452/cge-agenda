package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.repository.BackupConfigRepository;
import gov.bf.ascelc.cge_agenda.service.impl.BackupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Vérifie la protection anti-path-traversal des noms de fichiers de sauvegarde :
 * un nom de fichier fourni par le client ne doit jamais pouvoir sortir du dossier
 * de sauvegarde (ex: télécharger/supprimer un fichier arbitraire du serveur).
 */
class BackupServiceSecurityTest {

    private BackupServiceImpl service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        BackupConfigRepository repo = mock(BackupConfigRepository.class);
        service = new BackupServiceImpl(repo);

        Path backupDir = tempDir.resolve("backups");
        Files.createDirectories(backupDir.resolve("corbeille"));

        ReflectionTestUtils.setField(service, "backupDir", backupDir.toString());
        ReflectionTestUtils.setField(service, "pgDumpPath", "pg_dump");
        ReflectionTestUtils.setField(service, "pgRestorePath", "pg_restore");
        ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:postgresql://localhost:5432/test");
        ReflectionTestUtils.setField(service, "datasourceUsername", "test");
        ReflectionTestUtils.setField(service, "datasourcePassword", "test");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../../etc/passwd.backup",
            "..\\..\\windows\\system32\\config.backup",
            "foo/../../../secret.backup",
            "not-a-backup-file.txt",
            "no-extension"
    })
    void download_rejectsMaliciousOrInvalidFilenames(String maliciousFilename) {
        assertThatThrownBy(() -> service.download(maliciousFilename))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void download_acceptsLegitimateFilenameButReturns404WhenAbsent() {
        // Un nom de fichier valide qui n'existe pas doit échouer en 404 (introuvable),
        // pas en 400 (invalide) — la validation du nom et l'existence sont deux choses distinctes.
        assertThatThrownBy(() -> service.download("backup_manual_20260101_120000.backup"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void delete_onExistingFile_movesItToCorbeilleWithoutThrowing() throws Exception {
        Path backupDir = tempDir.resolve("backups");
        Path realFile = backupDir.resolve("backup_manual_20260101_120000.backup");
        Files.writeString(realFile, "dummy content");

        assertThatCode(() -> service.delete("backup_manual_20260101_120000.backup"))
                .doesNotThrowAnyException();

        assertThatCode(() -> {
            if (Files.exists(realFile)) {
                throw new AssertionError("Le fichier aurait dû être déplacé vers la corbeille");
            }
            if (!Files.exists(backupDir.resolve("corbeille").resolve("backup_manual_20260101_120000.backup"))) {
                throw new AssertionError("Le fichier devrait être présent dans la corbeille");
            }
        }).doesNotThrowAnyException();
    }
}
