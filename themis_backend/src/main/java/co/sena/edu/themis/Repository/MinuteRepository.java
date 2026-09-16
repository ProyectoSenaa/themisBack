package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.Minute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface MinuteRepository extends JpaRepository<Minute, Long> {
    // Buscar acta por el ID del evento de comité
    @Query("SELECT m FROM Minute m WHERE m.committeeEvent.id = :committeeEventId")
    Optional<Minute> findByCommitteeEventId(@Param("committeeEventId") Long committeeEventId);
}
