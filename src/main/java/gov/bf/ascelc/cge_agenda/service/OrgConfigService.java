package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.OrgConfigDto;

public interface OrgConfigService {

    OrgConfigDto getConfig();

    OrgConfigDto updateConfig(OrgConfigDto dto);

    String previewTemplate(String templateKey);
}
