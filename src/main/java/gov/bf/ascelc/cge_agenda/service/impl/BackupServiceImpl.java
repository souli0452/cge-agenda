package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.BackupConfigDto;
import gov.bf.ascelc.cge_agenda.dto.BackupInfoDto;
import gov.bf.ascelc.cge_agenda.entities.BackupConfig;
import gov.bf.ascelc.cge_agenda.repository.BackupConfigRepository;
import gov.bf.ascelc.cge_agenda.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final BackupConfigRepository backupConfigRepository;

    @Value("${app.backup.dir:backups}")
    private String backupDir;

    @Value("${pg.dump.path:pg_dump}")
    private String pgDumpPath;

    @Value("${pg.restore.path:pg_restore}")
    private String pgRestorePath;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    private static final DateTimeFormatter FILENAME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    // ==========================================
    // LISTE
    // ==========================================
    @Override
    public List<BackupInfoDto> list() {
        return listDirectory(mainDir());
    }

    @Override
    public List<BackupInfoDto> getCorbeille() {
        return listDirectory(corbeilleDir());
    }

    private List<BackupInfoDto> listDirectory(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> p.toString().endsWith(".backup"))
                    .map(this::toBackupInfo)
                    .sorted(Comparator.comparing(BackupInfoDto::getCreatedAt).reversed())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("❌ Erreur listage sauvegardes : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de lister les sauvegardes");
        }
    }

    private BackupInfoDto toBackupInfo(Path file) {
        try {
            String filename = file.getFileName().toString();
            long sizeBytes = Files.size(file);
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(file).toInstant(), ZoneId.systemDefault());
            String type = filename.contains("_manual_") ? "MANUAL" : "AUTO";
            String createdBy = readMeta(file).getProperty("createdBy");

            return BackupInfoDto.builder()
                    .filename(filename)
                    .createdAt(createdAt)
                    .sizeBytes(sizeBytes)
                    .sizeFormatted(formatSize(sizeBytes))
                    .type(type)
                    .createdBy(createdBy)
                    .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lecture métadonnées de sauvegarde : " + e.getMessage());
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        if (bytes < 1024 * 1024) return String.format("%.1f Ko", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f Mo", bytes / (1024.0 * 1024));
        return String.format("%.2f Go", bytes / (1024.0 * 1024 * 1024));
    }

    // ==========================================
    // CRÉATION
    // ==========================================
    @Override
    public BackupInfoDto create() {
        BackupInfoDto info = runDump("manual", currentUserEmail());
        log.info("✓ Sauvegarde manuelle créée : {}", info.getFilename());
        return info;
    }

    private BackupInfoDto runDump(String label, String createdBy) {
        String filename = "backup_" + label + "_" + LocalDateTime.now().format(FILENAME_FMT) + ".backup";
        Path target = mainDir().resolve(filename);
        DbConnInfo conn = parseJdbcUrl();

        List<String> command = List.of(
                pgDumpPath, "-Fc", "--no-owner",
                "-h", conn.host, "-p", conn.port,
                "-U", datasourceUsername,
                "-d", conn.database,
                "-f", target.toString()
        );

        int exitCode = runProcess(command);
        if (exitCode != 0 || !Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de pg_dump (code " + exitCode + ")");
        }

        if (createdBy != null) {
            writeMeta(target, createdBy);
        }

        return toBackupInfo(target);
    }

    // ==========================================
    // RESTAURATION
    // ==========================================
    @Override
    public String restore(String filename) {
        Path source = mainDir().resolve(sanitize(filename));
        if (!Files.exists(source)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sauvegarde introuvable : " + filename);
        }

        // Sauvegarde de sécurité avant restauration : même une restauration ratée reste réversible
        BackupInfoDto safety = runDump("auto", currentUserEmail());
        log.info("✓ Sauvegarde de sécurité créée avant restauration : {}", safety.getFilename());

        DbConnInfo conn = parseJdbcUrl();
        List<String> command = List.of(
                pgRestorePath, "--clean", "--if-exists", "--no-owner",
                "-h", conn.host, "-p", conn.port,
                "-U", datasourceUsername,
                "-d", conn.database,
                source.toString()
        );

        int exitCode = runProcess(command);
        if (exitCode != 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la restauration (code " + exitCode
                            + "). Une sauvegarde de sécurité a été créée avant l'opération : " + safety.getFilename());
        }

        log.info("✓ Base de données restaurée depuis : {}", filename);
        return "Base de données restaurée avec succès depuis " + filename;
    }

    // ==========================================
    // SUPPRESSION (CORBEILLE) / TÉLÉCHARGEMENT
    // ==========================================
    @Override
    public void delete(String filename) {
        moveFile(mainDir(), corbeilleDir(), sanitize(filename));
        log.info("✓ Sauvegarde mise à la corbeille : {}", filename);
    }

    @Override
    public void restoreFromCorbeille(String filename) {
        moveFile(corbeilleDir(), mainDir(), sanitize(filename));
        log.info("✓ Sauvegarde restaurée depuis la corbeille : {}", filename);
    }

    @Override
    public void deletePermanently(String filename) {
        deleteFileAndMeta(corbeilleDir().resolve(sanitize(filename)));
        log.info("✓ Sauvegarde supprimée définitivement : {}", filename);
    }

    @Override
    public byte[] download(String filename) {
        Path file = mainDir().resolve(sanitize(filename));
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sauvegarde introuvable : " + filename);
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lecture du fichier : " + e.getMessage());
        }
    }

    private void moveFile(Path fromDir, Path toDir, String filename) {
        Path from = fromDir.resolve(filename);
        if (!Files.exists(from)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sauvegarde introuvable : " + filename);
        }
        try {
            Files.move(from, toDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            Path fromMeta = metaPath(from);
            if (Files.exists(fromMeta)) {
                Files.move(fromMeta, metaPath(toDir.resolve(filename)), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur déplacement du fichier : " + e.getMessage());
        }
    }

    private void deleteFileAndMeta(Path file) {
        try {
            Files.deleteIfExists(file);
            Files.deleteIfExists(metaPath(file));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur suppression du fichier : " + e.getMessage());
        }
    }

    // ==========================================
    // CONFIGURATION
    // ==========================================
    @Override
    public BackupConfigDto getConfig() {
        return toDto(findOrCreateConfig());
    }

    @Override
    public BackupConfigDto saveConfig(BackupConfigDto dto) {
        BackupConfig config = findOrCreateConfig();
        config.setAutoEnabled(dto.isAutoEnabled());
        config.setBackupHour(dto.getBackupHour());
        config.setBackupMinute(dto.getBackupMinute());
        config.setRetentionCount(dto.getRetentionCount());
        return toDto(backupConfigRepository.save(config));
    }

    private BackupConfig findOrCreateConfig() {
        return backupConfigRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> backupConfigRepository.save(
                        BackupConfig.builder()
                                .autoEnabled(true)
                                .backupHour(2)
                                .backupMinute(0)
                                .retentionCount(30)
                                .build()
                ));
    }

    private BackupConfigDto toDto(BackupConfig c) {
        return BackupConfigDto.builder()
                .autoEnabled(c.isAutoEnabled())
                .backupHour(c.getBackupHour())
                .backupMinute(c.getBackupMinute())
                .retentionCount(c.getRetentionCount())
                .build();
    }

    // ==========================================
    // SAUVEGARDE PLANIFIÉE + RÉTENTION
    // ==========================================
    @Override
    public void runScheduledBackup() {
        BackupConfig config = findOrCreateConfig();
        if (!config.isAutoEnabled()) {
            return;
        }

        runDump("auto", null);
        log.info("✓ Sauvegarde automatique effectuée");

        try (Stream<Path> files = Files.list(mainDir())) {
            List<Path> autoBackups = files
                    .filter(p -> p.getFileName().toString().contains("_auto_"))
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .collect(Collectors.toList());

            if (autoBackups.size() > config.getRetentionCount()) {
                List<Path> toDelete = autoBackups.subList(config.getRetentionCount(), autoBackups.size());
                for (Path p : toDelete) {
                    deleteFileAndMeta(p);
                    log.info("✓ Sauvegarde auto purgée (rétention dépassée) : {}", p.getFileName());
                }
            }
        } catch (IOException e) {
            log.error("❌ Erreur purge des sauvegardes : {}", e.getMessage());
        }
    }

    private LocalDateTime lastModifiedSafe(Path p) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            return LocalDateTime.MIN;
        }
    }

    // ==========================================
    // PROCESSUS SYSTÈME (pg_dump / pg_restore)
    // ==========================================
    private int runProcess(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (var reader = process.inputReader()) {
                reader.lines().forEach(line -> output.append(line).append('\n'));
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("❌ Processus pg_dump/pg_restore expiré (timeout)");
                return -1;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("❌ pg_dump/pg_restore a échoué (code {}) : {}", exitCode, output);
            } else if (!output.isEmpty()) {
                log.debug("pg_dump/pg_restore : {}", output);
            }
            return exitCode;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Erreur exécution processus : {}", e.getMessage());
            return -1;
        }
    }

    private record DbConnInfo(String host, String port, String database) {}

    private DbConnInfo parseJdbcUrl() {
        Matcher m = JDBC_URL_PATTERN.matcher(datasourceUrl);
        if (!m.find()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible d'interpréter l'URL de la base de données");
        }
        return new DbConnInfo(m.group(1), m.group(2), m.group(3));
    }

    // ==========================================
    // MÉTADONNÉES (créateur)
    // ==========================================
    private void writeMeta(Path backupFile, String createdBy) {
        Properties props = new Properties();
        props.setProperty("createdBy", createdBy);
        try (var out = Files.newOutputStream(metaPath(backupFile))) {
            props.store(out, null);
        } catch (IOException e) {
            log.warn("⚠ Impossible d'écrire les métadonnées : {}", e.getMessage());
        }
    }

    private Properties readMeta(Path backupFile) {
        Properties props = new Properties();
        Path meta = metaPath(backupFile);
        if (Files.exists(meta)) {
            try (var in = Files.newInputStream(meta)) {
                props.load(in);
            } catch (IOException ignored) {
            }
        }
        return props;
    }

    private Path metaPath(Path backupFile) {
        return Paths.get(backupFile.toString() + ".meta");
    }

    // ==========================================
    // UTILITAIRES
    // ==========================================
    private Path mainDir() {
        return Paths.get(backupDir);
    }

    private Path corbeilleDir() {
        return Paths.get(backupDir).resolve("corbeille");
    }

    /** Empêche toute tentative de path traversal via le nom de fichier fourni par le client. */
    /**
     * Rejette explicitement toute tentative de path traversal (segments "..", séparateurs
     * de répertoire) plutôt que de les neutraliser silencieusement via getFileName() —
     * une entrée suspecte doit échouer bruyamment, pas être discrètement corrigée.
     */
    private String sanitize(String filename) {
        if (filename == null
                || filename.contains("..")
                || filename.contains("/")
                || filename.contains("\\")
                || !filename.endsWith(".backup")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom de fichier invalide");
        }
        return filename;
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaim("email");
            return email != null ? email : jwt.getClaim("preferred_username");
        }
        return null;
    }
}
