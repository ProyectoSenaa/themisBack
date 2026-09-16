package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.FollowUpStatusDto;
import co.sena.edu.themis.Entity.FollowUpStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FollowUpStatusMapper {
    FollowUpStatusDto toDto(FollowUpStatus entity);
    FollowUpStatus toEntity(FollowUpStatusDto dto);
    void updateEntityFromDto(FollowUpStatusDto dto, @MappingTarget FollowUpStatus entity);
}
