package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.UserSettingsDto;
import gov.bf.ascelc.cge_agenda.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.SETTINGS_ROOT_URL;

@RestController
@RequestMapping(SETTINGS_ROOT_URL)
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<UserSettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PatchMapping
    public ResponseEntity<UserSettingsDto> updateSettings(@RequestBody UserSettingsDto dto) {
        return ResponseEntity.ok(settingsService.updateSettings(dto));
    }
}
