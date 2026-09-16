package co.sena.edu.themis.Dto;

import co.sena.edu.themis.Dto.OlympoFederated.Administrative;
import co.sena.edu.themis.Dto.OlympoFederated.Student;
import co.sena.edu.themis.Dto.OlympoFederated.Teacher;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoveltyDto {
    private Long id;
    @NotBlank(message = "La fecha es obligatoria")
    private String date;

    @NotBlank(message = "La observación es obligatoria")
    @Size(max = 1000, message = "La observación no puede exceder los 1000 caracteres")
    private String observation;

    private String justification;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean isActive;
    private String noveltyFiles;
    private NoveltyTypeDto noveltyType;
    private NoveltyStatusDto noveltyStatus;
    private ProcessFlowStatusDto processFlowStatus;
    private Long studentId;
    private Long studySheetId;
    private Long teacherId;
    private Long administrativeId;
    // Campos adicionales para enriquecer la respuesta (rellenados por el resolver)
    private String studentName;
    private String teacherName;
    private String studentEmail;
    private String teacherEmail;
    private String administrativeEmail;

    public NoveltyDto(Long id, String date, NoveltyStatusDto noveltyStatus, String nameNovelty) {
        this.id = id;
        this.date = date;
        this.noveltyStatus = noveltyStatus;
        this.noveltyType = new NoveltyTypeDto();
        this.noveltyType.setNameNovelty(nameNovelty);
    }
}
