package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.CommitteeDto;
import co.sena.edu.themis.Entity.Committee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(componentModel = "spring", uses = CommitteeEventMapper.class)
public interface CommitteeMapper {

    CommitteeMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(CommitteeMapper.class);

    @Mapping(target = "committeeEvents", qualifiedByName = "toDtoSummaryList")
    CommitteeDto toDto(Committee entity);

    @Mapping(target = "committeeEvents", qualifiedByName = "toEntityMinimalList")
    Committee toEntity(CommitteeDto dto);

    List<CommitteeDto> committeeToList(List<Committee> entities);

    void updateEntityFromDto(CommitteeDto dto, @MappingTarget Committee entity);

    @Named("toDtoSummary")
    @Mapping(target = "committeeEvents", ignore = true)
    CommitteeDto toDtoSummary(Committee entity);

    @Named("toEntityMinimal")
    @Mapping(target = "committeeEvents", ignore = true)
    Committee toEntityMinimal(CommitteeDto dto);

    default Page<CommitteeDto> committeesToCommitteeDTOPage(Page<Committee> committeePage) {
        List<CommitteeDto> dtoList = committeeToList(committeePage.getContent());
        System.out.println("Committee dtoList: " + dtoList);
        return new PageImpl<>(dtoList, committeePage.getPageable(), committeePage.getTotalElements());
    }
}
