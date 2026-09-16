package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.NoveltyDto;
import co.sena.edu.themis.Entity.Novelty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NoveltyMapper {

    NoveltyMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(NoveltyMapper.class);

    NoveltyDto toDto(Novelty entity);

    @Mapping(target = "date", ignore = true)
    Novelty toEntity(NoveltyDto dto);

    List<NoveltyDto> noveltyToList(List<Novelty> entities);

    @Mapping(target = "date", ignore = true)
    void updateEntityFromDto(NoveltyDto dto, @MappingTarget Novelty entity);


    default Page<NoveltyDto> macroRegionsToMacroRegionDTOPage(Page<Novelty> macroRegionPage) {

        List<NoveltyDto> dtoList = noveltyToList(macroRegionPage.getContent());
        System.out.println("dtoList"+dtoList);
        return new PageImpl<>(dtoList, macroRegionPage.getPageable(), macroRegionPage.getTotalElements());
    }

}

