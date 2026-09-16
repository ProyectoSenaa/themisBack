package co.sena.edu.themis.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowUpTypeDto {
    private Long id;
    private String name;
    private boolean isActive;
    private String description;
}

