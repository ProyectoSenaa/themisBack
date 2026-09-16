package co.sena.edu.themis.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Entity
@Table(name="novelty_type")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NoveltyType implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "novelty", nullable = false, length = 100)
    private String nameNovelty;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "procedureDescription", length = 120)
    private String procedureDescription;

    //Relaciones
    @JsonBackReference
    @OneToMany(mappedBy = "noveltyType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Novelty> noveltyList;

}