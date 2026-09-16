package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.FollowUpDto;
import co.sena.edu.themis.Entity.FollowUp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FollowUpMapper {

    FollowUpMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(FollowUpMapper.class);

    // ============================================================
    //  ENTITY → DTO
    // ============================================================
    @Mapping(target = "studentId", source = "studentId")
    @Mapping(target = "teacherId", source = "teacherId")
    @Mapping(target = "followUpTypeId", expression = "java(entity.getFollowUpType() != null ? entity.getFollowUpType().getId() : null)")
    @Mapping(target = "followUpStatusId", expression = "java(entity.getFollowUpStatus() != null ? entity.getFollowUpStatus().getId() : null)")
    @Mapping(target = "followUpFlowStatusId", expression = "java(entity.getFollowUpFlowStatus() != null ? entity.getFollowUpFlowStatus().getId() : null)")
    @Mapping(target = "minuteId", expression = "java(entity.getMinute() != null ? entity.getMinute().getId() : null)")
    @Mapping(target = "committeeEvent", source = "committeeEvent")
    FollowUpDto toDto(FollowUp entity);


    // ============================================================
    //  DTO → ENTITY (CREACIÓN)
    // ============================================================
    @Mapping(target = "notificationList", ignore = true)
    @Mapping(target = "minute", ignore = true)
    @Mapping(target = "committeeEvent", ignore = true)
    // Do not map relational entities here (business layer assigns them by id)
    @Mapping(target = "followUpType", ignore = true)
    @Mapping(target = "followUpStatus", ignore = true)
    @Mapping(target = "followUpFlowStatus", ignore = true)
    FollowUp toEntity(FollowUpDto dto);

    List<FollowUpDto> followUpToList(List<FollowUp> entities);


    // ============================================================
    //  DTO → EXISTING ENTITY (UPDATE)
    // ============================================================
    @Mapping(target = "notificationList", ignore = true)
    @Mapping(target = "minute", ignore = true)
    @Mapping(target = "committeeEvent", ignore = true)
    // business handles relations
    @Mapping(target = "followUpType", ignore = true)
    @Mapping(target = "followUpStatus", ignore = true)
    @Mapping(target = "followUpFlowStatus", ignore = true)
    void updateEntityFromDto(FollowUpDto dto, @MappingTarget FollowUp entity);


    // ============================================================
    //  PAGE MAPPING
    // ============================================================
    default Page<FollowUpDto> followUpsToFollowUpDTOPage(Page<FollowUp> followUpPage) {
        List<FollowUpDto> dtoList = followUpToList(followUpPage.getContent());
        return new PageImpl<>(dtoList, followUpPage.getPageable(), followUpPage.getTotalElements());
    }
}
