package co.sena.edu.themis.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinuteDto {
    private Long id;
    private String fileContent;
    private CommitteeEventDto committeeEvent;
}
