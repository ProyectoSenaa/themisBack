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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Entity
@Table(name = "novelty")
@NoArgsConstructor
@AllArgsConstructor
@Data

public class Novelty implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "date", nullable = false)
    @Temporal(TemporalType.DATE)
    private LocalDate date;

    @Column(name = "observation", nullable = false)
    private String observation;

    @Column(name = "justification", nullable = false)
    private String justification;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "files", nullable = false)
    private byte[] noveltyFiles;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "study_sheet_id")
    private Long studySheetId;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "administrative_id")
    private Long administrativeId;

    @Column(name = "committee_event_id")
    private Long committeeEventId;

    @JsonManagedReference
    @ManyToOne(fetch = FetchType.EAGER)
    @ToString.Exclude
    @JoinColumn(name = "novelty_type_id", referencedColumnName = "id")
    private NoveltyType noveltyType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "novelty_status_id", referencedColumnName = "id")
    private NoveltyStatus noveltyStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "process_flow_status_id", referencedColumnName = "id")
    private ProcessFlowStatus processFlowStatus;

    @JsonBackReference
    @OneToMany(mappedBy = "novelty", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> noveltyList;

    public String getNoveltyFiles() {
        if (noveltyFiles == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(noveltyFiles);
    }

    public void setNoveltyFiles(String justificationFile) {
        this.noveltyFiles = Base64.getDecoder().decode(justificationFile);
    }
}