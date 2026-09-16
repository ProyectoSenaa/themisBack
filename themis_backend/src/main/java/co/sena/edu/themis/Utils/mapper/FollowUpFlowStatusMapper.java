package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.FollowUpFlowStatusDto;
import co.sena.edu.themis.Entity.FollowUpFlowStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FollowUpFlowStatusMapper {
    FollowUpFlowStatusDto toDto(FollowUpFlowStatus entity);
    FollowUpFlowStatus toEntity(FollowUpFlowStatusDto dto);
    void updateEntityFromDto(FollowUpFlowStatusDto dto, @MappingTarget FollowUpFlowStatus entity);
}
