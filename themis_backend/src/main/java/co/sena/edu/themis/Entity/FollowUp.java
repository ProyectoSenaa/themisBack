package co.sena.edu.themis.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

@Entity
@Table(name = "follow_up")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class FollowUp implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    // Módulo de Gestión de Casos
    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @Column(name = "case_description", nullable = false, length = 1000)
    private String caseDescription; // Descripción detallada del caso

    @Column(name = "evidence_files")
    private byte[] evidenceFiles; // Capturas de pantalla, informes

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    // Campos de seguimiento general
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId; // Teacher que crea el caso

    @Column(name = "coordinator_id")
    private Long coordinatorId; // Coordinador que revisa


    @Column(name = "study_sheet_id")
    private Long studySheetId;

    // Relación con CommitteeEvent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "committee_event_id", insertable = false, updatable = false)
    private CommitteeEvent committeeEvent; // Relación con el evento de comité

    // Relaciones
    @JsonManagedReference
    @ManyToOne(fetch = FetchType.EAGER)
    @ToString.Exclude
    @JoinColumn(name = "follow_up_type_id", referencedColumnName = "id")
    private FollowUpType followUpType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "follow_up_status_id", referencedColumnName = "id")
    private FollowUpStatus followUpStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "follow_up_flow_status_id", referencedColumnName = "id")
    private FollowUpFlowStatus followUpFlowStatus;

    // Relación con Minute para el acta del comité
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "minute_id", referencedColumnName = "id")
    private Minute minute; // Acta del comité usando la entidad existente

    @JsonBackReference
    @OneToMany(mappedBy = "followUp", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notificationList;

    // Métodos para manejar archivos de evidencia
    public String getEvidenceFiles() {
        if (evidenceFiles == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(evidenceFiles);
    }

    public void setEvidenceFiles(String evidenceFilesBase64) {
        if (evidenceFilesBase64 != null) {
            this.evidenceFiles = Base64.getDecoder().decode(evidenceFilesBase64);
        }
    }

    // Métodos de utilidad simplificados
    public boolean isResolved() {
        return minute != null && followUpStatus != null &&
                "resuelto".equals(followUpStatus.getName());
    }
}