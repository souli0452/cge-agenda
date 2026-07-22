package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.BackupConfigDto;
import gov.bf.ascelc.cge_agenda.dto.BackupInfoDto;
import gov.bf.ascelc.cge_agenda.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(ADMIN_ROOT_URL)
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final BackupService backupService;

    @GetMapping(ADMIN_BACKUP)
    public ResponseEntity<List<BackupInfoDto>> list() {
        return ResponseEntity.ok(backupService.list());
    }

    @PostMapping(ADMIN_BACKUP)
    public ResponseEntity<BackupInfoDto> create() {
        return ResponseEntity.ok(backupService.create());
    }

    @PostMapping(ADMIN_BACKUP_RESTORE)
    public ResponseEntity<Map<String, String>> restore(@PathVariable String filename) {
        String message = backupService.restore(filename);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @DeleteMapping(ADMIN_BACKUP_BY_FILENAME)
    public ResponseEntity<Void> delete(@PathVariable String filename) {
        backupService.delete(filename);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(ADMIN_BACKUP_DOWNLOAD)
    public ResponseEntity<byte[]> download(@PathVariable String filename) {
        byte[] content = backupService.download(filename);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @GetMapping(ADMIN_BACKUP_CONFIG)
    public ResponseEntity<BackupConfigDto> getConfig() {
        return ResponseEntity.ok(backupService.getConfig());
    }

    @PutMapping(ADMIN_BACKUP_CONFIG)
    public ResponseEntity<BackupConfigDto> saveConfig(@RequestBody BackupConfigDto dto) {
        return ResponseEntity.ok(backupService.saveConfig(dto));
    }

    @GetMapping(ADMIN_BACKUP_CORBEILLE)
    public ResponseEntity<List<BackupInfoDto>> getCorbeille() {
        return ResponseEntity.ok(backupService.getCorbeille());
    }

    @PostMapping(ADMIN_BACKUP_CORBEILLE_RESTORE)
    public ResponseEntity<Map<String, String>> restoreFromCorbeille(@PathVariable String filename) {
        backupService.restoreFromCorbeille(filename);
        return ResponseEntity.ok(Map.of("message", "Sauvegarde restaurée depuis la corbeille"));
    }

    @DeleteMapping(ADMIN_BACKUP_CORBEILLE_DELETE)
    public ResponseEntity<Void> deletePermanently(@PathVariable String filename) {
        backupService.deletePermanently(filename);
        return ResponseEntity.noContent().build();
    }
}
