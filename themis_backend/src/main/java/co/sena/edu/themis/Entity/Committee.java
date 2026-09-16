package co.sena.edu.themis.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "committee")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Committee implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // un comite por coordinacion
    @Column(name = "coordination_id")
    private Long coordinationId;

    // Lista de estudiantes
    @ElementCollection
    @CollectionTable(
            name = "committee_students",
            joinColumns = @JoinColumn(name = "committee_id") // referencia al comité
    )
    @Column(name = "student_id") // id del estudiante
    private List<Long> studentsIds;

    // Lista de instructores
    @ElementCollection
    @CollectionTable(
            name = "committee_teachers",
            joinColumns = @JoinColumn(name = "committee_id")
    )
    @Column(name = "teacher_id")
    private List<Long> teachersIds;

    // Lista de administrativos
    @ElementCollection
    @CollectionTable(
            name = "committee_administratives",
            joinColumns = @JoinColumn(name = "committee_id")
    )
    @Column(name = "administrative_id")
    private List<Long> administrativesIds;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent;
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // Un comité tiene muchos eventos
    @OneToMany(mappedBy = "committee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommitteeEvent> committeeEvents;
}