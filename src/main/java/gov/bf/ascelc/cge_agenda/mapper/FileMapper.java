package gov.bf.ascelc.cge_agenda.mapper;

import gov.bf.ascelc.cge_agenda.dto.FileDto;
import gov.bf.ascelc.cge_agenda.entities.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FileMapper {

    @Mapping(source = "event.id", target = "eventId")
    FileDto toDto(File file);

    @Mapping(target = "event", ignore = true)
    File toEntity(FileDto fileDto);

    List<FileDto> toDtos(List<File> files);

    List<File> toEntities(List<FileDto> fileDtos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    void updateEntityFromDto(FileDto fileDto, @MappingTarget File file);
}