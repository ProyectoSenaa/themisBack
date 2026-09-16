package co.sena.edu.themis.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitteeNoveltyResponseInput {
    private Long studentId;
    private Long noveltyId;
    private Boolean approved;
    private String observation;
}
