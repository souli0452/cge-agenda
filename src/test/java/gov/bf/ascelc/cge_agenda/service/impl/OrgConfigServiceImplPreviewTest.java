package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.entities.OrgConfig;
import gov.bf.ascelc.cge_agenda.repository.OrgConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgConfigServiceImplPreviewTest {

    private OrgConfigRepository orgConfigRepository;
    private OrgConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        orgConfigRepository = mock(OrgConfigRepository.class);
        when(orgConfigRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
        when(orgConfigRepository.save(org.mockito.ArgumentMatchers.any(OrgConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        templateEngine.setTemplateResolver(resolver);

        service = new OrgConfigServiceImpl(orgConfigRepository, templateEngine);
    }

    @Test
    void previewTemplate_invitation_containsRealEventTitleAndConfiguredBody() {
        String html = service.previewTemplate("invitation");

        assertThat(html).contains("Réunion de démonstration");
        assertThat(html).contains("Jean Dupont");
        // Valeur par défaut configurée (voir Task 3, findOrCreateConfig) doit apparaître
        // substituée, pas la syntaxe brute {evenement}.
        assertThat(html).doesNotContain("{evenement}");
        assertThat(html).doesNotContain("{participant}");
    }

    @Test
    void previewTemplate_unknownKey_throws404() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.previewTemplate("inconnu"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
