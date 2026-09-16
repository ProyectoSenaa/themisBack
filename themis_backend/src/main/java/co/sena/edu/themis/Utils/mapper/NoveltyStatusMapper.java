package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.NoveltyStatusDto;
import co.sena.edu.themis.Entity.NoveltyStatus;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NoveltyStatusMapper {
    NoveltyStatusDto toDto(NoveltyStatus entity);
    NoveltyStatus toEntity(NoveltyStatusDto dto);
    void updateEntityFromDto(NoveltyStatusDto dto, @MappingTarget NoveltyStatus entity);
}

