package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.OrgConfigDto;
import gov.bf.ascelc.cge_agenda.service.OrgConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(ADMIN_ROOT_URL)
@RequiredArgsConstructor
@Slf4j
public class OrgConfigController {

    private final OrgConfigService orgConfigService;

    @GetMapping(ADMIN_CONFIG)
    public ResponseEntity<OrgConfigDto> getConfig() {
        return ResponseEntity.ok(orgConfigService.getConfig());
    }

    @PutMapping(ADMIN_CONFIG)
    public ResponseEntity<OrgConfigDto> updateConfig(@RequestBody OrgConfigDto dto) {
        return ResponseEntity.ok(orgConfigService.updateConfig(dto));
    }

    @GetMapping(value = ADMIN_CONFIG_PREVIEW, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewTemplate(@PathVariable String templateKey) {
        return ResponseEntity.ok(orgConfigService.previewTemplate(templateKey));
    }
}
