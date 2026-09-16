package co.sena.edu.themis.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "minute")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Minute implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_content", nullable = false, columnDefinition = "TEXT")
    private String fileContent;

    @ManyToOne
    @JoinColumn(name = "committee_event_id")
    private CommitteeEvent committeeEvent;
}
