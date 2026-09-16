package co.sena.edu.themis.Dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommitteeDto {
    private Long id;
    private Long coordinationId;
    private List<Long> studentsIds;
    private List<Long> teachersIds;
    private List<Long> administrativesIds;
    private boolean isCurrent;
    private boolean isActive;
    private List<CommitteeEventDto> committeeEvents;
    private List<Long> eventIds; // Campo para asociar eventos específicos
}
