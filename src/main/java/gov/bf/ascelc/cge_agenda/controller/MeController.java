package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(ME_ROOT_URL)
@RequiredArgsConstructor
public class MeController {

    private final PermissionService permissionService;

    @GetMapping(ME_PERMISSIONS)
    public ResponseEntity<Set<String>> getMesPermissions() {
        return ResponseEntity.ok(permissionService.getPermissionsCourantes());
    }
}
