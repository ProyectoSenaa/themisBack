package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.FollowUpTypeDto;
import co.sena.edu.themis.Entity.FollowUpType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FollowUpTypeMapper {

    FollowUpTypeMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(FollowUpTypeMapper.class);

    FollowUpTypeDto followUpTypeToFollowUpTypeDTO(FollowUpType entity);

    FollowUpType followUpTypeDTOToFollowUpType(FollowUpTypeDto dto);

    List<FollowUpTypeDto> followUpTypeToList(List<FollowUpType> entities);

    void updateEntityFromDto(FollowUpTypeDto dto, @MappingTarget FollowUpType entity);

    default Page<FollowUpTypeDto> followUpTypesToFollowUpTypeDTOPage(Page<FollowUpType> followUpTypePage) {
        List<FollowUpTypeDto> dtoList = followUpTypeToList(followUpTypePage.getContent());
        return new PageImpl<>(dtoList, followUpTypePage.getPageable(), followUpTypePage.getTotalElements());
    }
}
