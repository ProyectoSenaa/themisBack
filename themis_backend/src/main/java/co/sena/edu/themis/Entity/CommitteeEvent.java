package co.sena.edu.themis.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "committee_event")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CommitteeEvent implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "hour", nullable = false)
    private Time hour;

    @Column(name = "session", nullable = false)
    private String session;

    @Column(name = "coordination_name")
    private String coordinationName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @OneToMany(mappedBy = "committeeEvent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Minute> minutes ;

    @ManyToOne
    @JoinColumn(name = "committee_id")
    private Committee committee;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}