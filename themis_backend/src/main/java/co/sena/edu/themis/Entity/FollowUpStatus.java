package co.sena.edu.themis.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "follow_up_status")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class FollowUpStatus implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = true)
    private Long id;

    @Column(name = "name", nullable = true, length = 50)
    private String name;

    @Column(name = "description", length = 150)
    private String description;
}
