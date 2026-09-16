package gov.bf.ascelc.cge_agenda.mapper;

import gov.bf.ascelc.cge_agenda.dto.AuditLogDto;
import gov.bf.ascelc.cge_agenda.entities.AuditLog;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogDto toDto(AuditLog auditLog);

    List<AuditLogDto> toDtos(List<AuditLog> auditLogs);
}
