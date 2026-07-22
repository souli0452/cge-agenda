package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.SchedulerConfigDto;
import gov.bf.ascelc.cge_agenda.service.SchedulerConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(ADMIN_ROOT_URL)
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfigController {

    private final SchedulerConfigService schedulerConfigService;

    @GetMapping(ADMIN_SCHEDULER)
    public ResponseEntity<SchedulerConfigDto> getConfig() {
        return ResponseEntity.ok(schedulerConfigService.getConfig());
    }

    @PutMapping(ADMIN_SCHEDULER)
    public ResponseEntity<SchedulerConfigDto> updateConfig(@RequestBody SchedulerConfigDto dto) {
        return ResponseEntity.ok(schedulerConfigService.updateConfig(dto));
    }

    @PostMapping(ADMIN_SCHEDULER_RUN_NOW)
    public ResponseEntity<Map<String, String>> runNow() {
        String message = schedulerConfigService.runNow();
        return ResponseEntity.ok(Map.of("status", "ok", "message", message));
    }
}
