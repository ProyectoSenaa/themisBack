package co.sena.edu.themis.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowUpDto {
    private Long id;

    // Módulo de Gestión de Casos
    private String creationDate;
    private String caseDescription;
    private String evidenceFiles;
    private Boolean isActive;

    // Campos de seguimiento general
    private Long studentId;
    private Long teacherId;
    private Long coordinatorId;
    private Long studySheetId;

    // Relaciones como objetos (para output/queries)
    private FollowUpTypeDto followUpType;
    private FollowUpStatusDto followUpStatus;
    private FollowUpFlowStatusDto followUpFlowStatus;
    private MinuteDto minute;
    private CommitteeEventDto committeeEvent; // Relación con el evento de comité

    // Relaciones como IDs (para input/mutations - requeridos por GraphQL)
    private Long followUpTypeId;
    private Long followUpStatusId;
    private Long followUpFlowStatusId;
    private Long minuteId;

    // Constructor para consultas básicas
    public FollowUpDto(Long id, String creationDate, FollowUpStatusDto followUpStatus, String typeName) {
        this.id = id;
        this.creationDate = creationDate;
        this.followUpStatus = followUpStatus;
        this.followUpType = new FollowUpTypeDto();
        this.followUpType.setName(typeName);
    }

    // Alias accessor used by GraphQL frontend: improvementPlanFiles maps to evidenceFiles
    public String getImprovementPlanFiles() {
        return this.evidenceFiles;
    }

    public void setImprovementPlanFiles(String improvementPlanFiles) {
        this.evidenceFiles = improvementPlanFiles;
    }
}