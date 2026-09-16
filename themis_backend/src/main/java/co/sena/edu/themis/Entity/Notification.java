package co.sena.edu.themis.Entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "notification")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Notification implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "notiMessage", nullable = false)
    private String notiMessage;
    @Column(name = "notiStatus", nullable = false, length = 55)
    private String notiStatus;
    @Column(name = "dateAttention", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dateAttention;
    @Column(name = "registrationDate", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date registrationDate;

    //Relaciones
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_novelty", referencedColumnName = "id")
    private Novelty novelty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_follow_up", referencedColumnName = "id")
    private FollowUp followUp;
}
