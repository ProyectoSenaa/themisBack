package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.ProcessFlowStatusDto;
import co.sena.edu.themis.Entity.ProcessFlowStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProcessFlowStatusMapper {
    ProcessFlowStatusDto toDto(ProcessFlowStatus entity);
    ProcessFlowStatus toEntity(ProcessFlowStatusDto dto);
    void updateEntityFromDto(ProcessFlowStatusDto dto, @MappingTarget ProcessFlowStatus entity);
}

