package gov.bf.ascelc.cge_agenda.security;

import gov.bf.ascelc.cge_agenda.service.AdminUserService;
import gov.bf.ascelc.cge_agenda.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie que "Journal d'audit" et "Administration" restent bien réservés à ADMIN.
 * Avant correctif, une règle générale ("GET /api/** -> tout rôle métier") déclarée
 * AVANT les règles spécifiques ADMIN les rendait inatteignables : Spring Security
 * retient la première règle qui correspond, pas la plus précise. N'importe quel
 * rôle métier authentifié pouvait donc lire /admin/** et /audit/** en GET.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private AuditService auditService;

    @ParameterizedTest
    @ValueSource(strings = { "CGE", "SECRETAIRE", "PROTOCOLE", "DIRECTEUR_CABINET" })
    void adminUsers_refuseAuxRolesMetierNonAdmin(String role) throws Exception {
        mockMvc.perform(get("/api/v1/cge-agenda/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUsers_autoriseAdmin() throws Exception {
        when(adminUserService.getUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cge-agenda/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = { "CGE", "SECRETAIRE", "PROTOCOLE", "DIRECTEUR_CABINET" })
    void auditPaged_refuseAuxRolesMetierNonAdmin(String role) throws Exception {
        mockMvc.perform(get("/api/v1/cge-agenda/audit/paged")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditPaged_autoriseAdmin() throws Exception {
        when(auditService.getPaged(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/cge-agenda/audit/paged")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
