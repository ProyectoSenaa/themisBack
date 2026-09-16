package co.sena.edu.themis.Utils.mapper;

import co.sena.edu.themis.Dto.CommitteeDto;
import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Entity.Committee;
import co.sena.edu.themis.Entity.CommitteeEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CommitteeEventMapper {

    CommitteeEventMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(CommitteeEventMapper.class);

    @Mapping(target = "minutes", ignore = true)
    @Mapping(target = "committee", qualifiedByName = "toCommitteeMinimalDto")
    @Mapping(target = "committeeId", source = "committee.id")
    @Mapping(target = "date", source = "date", qualifiedByName = "localDateToString")
    @Mapping(target = "hour", source = "hour", qualifiedByName = "timeToString")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "ldtToString")
    @Mapping(target = "finishedAt", source = "finishedAt", qualifiedByName = "ldtToString")
    CommitteeEventDto toDto(CommitteeEvent entity);

    @Mapping(target = "minutes", ignore = true)
    @Mapping(target = "committee", qualifiedByName = "toCommitteeMinimalEntity")
    @Mapping(target = "date", source = "date", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "hour", source = "hour", qualifiedByName = "stringToTime")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "stringToLdt")
    @Mapping(target = "finishedAt", source = "finishedAt", qualifiedByName = "stringToLdt")
    CommitteeEvent toEntity(CommitteeEventDto dto);

    List<CommitteeEventDto> committeeEventToList(List<CommitteeEvent> entities);

    @Mapping(target = "minutes", ignore = true)
    @Mapping(target = "committee", qualifiedByName = "toCommitteeMinimalEntity")
    @Mapping(target = "date", source = "date", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "hour", source = "hour", qualifiedByName = "stringToTime")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "stringToLdt")
    @Mapping(target = "finishedAt", source = "finishedAt", qualifiedByName = "stringToLdt")
    void updateEntityFromDto(CommitteeEventDto dto, @MappingTarget CommitteeEvent entity);

    @Named("toDtoSummaryList")
    List<CommitteeEventDto> toDtoSummaryList(List<CommitteeEvent> entities);

    @Named("toEntityMinimalList")
    List<CommitteeEvent> toEntityMinimalList(List<CommitteeEventDto> dtos);

    default Page<CommitteeEventDto> committeeEventsToCommitteeEventDTOPage(Page<CommitteeEvent> committeeEventPage) {
        List<CommitteeEventDto> dtoList = committeeEventToList(committeeEventPage.getContent());
        System.out.println("CommitteeEvent dtoList: " + dtoList);
        return new PageImpl<>(dtoList, committeeEventPage.getPageable(), committeeEventPage.getTotalElements());
    }

    @Named("toCommitteeMinimalDto")
    default CommitteeDto toCommitteeMinimalDto(Committee entity) {
        if (entity == null) return null;
        CommitteeDto dto = new CommitteeDto();
        dto.setId(entity.getId());
        dto.setCoordinationId(entity.getCoordinationId());
        dto.setStudentsIds(entity.getStudentsIds());
        dto.setTeachersIds(entity.getTeachersIds());
        dto.setAdministrativesIds(entity.getAdministrativesIds());
        dto.setCurrent(entity.isCurrent());
        dto.setActive(entity.isActive());
        // evitar incluir committeeEvents para no crear referencias circulares
        dto.setCommitteeEvents(null);
        return dto;
    }

    @Named("toCommitteeMinimalEntity")
    default Committee toCommitteeMinimalEntity(CommitteeDto dto) {
        if (dto == null) return null;
        Committee entity = new Committee();
        entity.setId(dto.getId());
        return entity;
    }

    @Named("localDateToString")
    default String localDateToString(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String date) {
        return (date != null && !date.isBlank()) ? LocalDate.parse(date) : null;
    }

    @Named("timeToString")
    default String timeToString(Time time) {
        return time != null ? time.toString() : null;
    }

    @Named("stringToTime")
    default Time stringToTime(String value) {
        try {
            return (value != null && !value.isBlank()) ? Time.valueOf(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Named("ldtToString")
    default String ldtToString(LocalDateTime ldt) {
        return ldt != null ? ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    @Named("stringToLdt")
    default LocalDateTime stringToLdt(String value) {
        try {
            return (value != null && !value.isBlank()) ? LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
