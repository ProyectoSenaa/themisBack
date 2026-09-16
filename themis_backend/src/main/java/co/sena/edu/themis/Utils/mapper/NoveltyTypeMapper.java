package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.NoveltyTypeDto;
import co.sena.edu.themis.Entity.NoveltyType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NoveltyTypeMapper {
    NoveltyTypeDto toDto(NoveltyType entity);
    NoveltyType toEntity(NoveltyTypeDto dto);
    void updateEntityFromDto(NoveltyTypeDto dto, @MappingTarget NoveltyType entity);
}

