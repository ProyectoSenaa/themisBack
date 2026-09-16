package co.sena.edu.themis.Dto;

import co.sena.edu.themis.Dto.CommitteeDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitteeEventDto {
    private Long id;
    private String date;
    private String hour;
    private String session;
    private String coordinationName;
    private String createdAt;
    private String finishedAt;
    private CommitteeDto committee;
    private Long committeeId;
    private List<MinuteDto> minutes;
}