package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.CommitteeEvent;
import co.sena.edu.themis.Entity.Novelty;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommitteeEventRepository extends JpaRepository<CommitteeEvent, Long> {

    // Métodos básicos con Spring Data JPA
    boolean existsById(Long id);

    List<CommitteeEvent> findAllByCommitteeId(Long committeeId);

    List<CommitteeEvent> findAllByDate(LocalDate date);

    List<CommitteeEvent> findAllByDateBetween(LocalDate startDate, LocalDate endDate);

    List<CommitteeEvent> findAllByDateAfter(LocalDate date);

    List<CommitteeEvent> findAllByDateGreaterThanEqual(LocalDate date);

    List<CommitteeEvent> findAllByDateBefore(LocalDate date);

    List<CommitteeEvent> findAllByCommitteeIdAndDate(Long committeeId, LocalDate date);

    List<CommitteeEvent> findAllByCommitteeIdAndDateBetween(Long committeeId, LocalDate startDate, LocalDate endDate);


    @Query("SELECT ce FROM CommitteeEvent ce WHERE ce.committee.id = :committeeId AND ce.date >= :currentDate ORDER BY ce.date ASC")
    List<CommitteeEvent> findUpcomingEventsByCommittee(@Param("committeeId") Long committeeId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT ce FROM CommitteeEvent ce WHERE ce.date = :date ORDER BY ce.hour ASC")
    List<CommitteeEvent> findEventsByDateOrderByTime(@Param("date") LocalDate date);

    @Query("SELECT ce FROM CommitteeEvent ce WHERE ce.committee.id = :committeeId AND ce.date = :date ORDER BY ce.hour ASC")
    List<CommitteeEvent> findCommitteeEventsByDateOrderByTime(@Param("committeeId") Long committeeId, @Param("date") LocalDate date);

    @Query("SELECT ce FROM CommitteeEvent ce WHERE ce.date BETWEEN :startOfWeek AND :endOfWeek ORDER BY ce.date ASC")
    List<CommitteeEvent> findEventsInWeek(@Param("startOfWeek") LocalDate startOfWeek, @Param("endOfWeek") LocalDate endOfWeek);

    @Query("SELECT COUNT(ce) FROM CommitteeEvent ce WHERE ce.committee.id = :committeeId")
    Long countEventsByCommittee(@Param("committeeId") Long committeeId);

    @Query("SELECT ce FROM CommitteeEvent ce WHERE ce.committee.id IN :committeeIds ORDER BY ce.date ASC")
    List<CommitteeEvent> findEventsByCommitteeIds(@Param("committeeIds") List<Long> committeeIds);


    @Query("SELECT ce FROM CommitteeEvent ce WHERE ce.committee.id = :committeeId AND ce.date = :date")
    List<CommitteeEvent> findByCommitteeIdAndDate(@Param("committeeId") Long committeeId, @Param("date") LocalDate date);


    @EntityGraph(attributePaths = {"committee", "committee.studentsIds", "committee.teachersIds", "committee.administrativesIds"})
    Optional<CommitteeEvent> findById(Long id);

    // Validación de colisión: mismo comité, misma fecha y hora
    @Query("SELECT CASE WHEN COUNT(ce) > 0 THEN true ELSE false END FROM CommitteeEvent ce WHERE ce.committee.id = :committeeId AND ce.date = :date AND ce.hour = :hour")
    boolean existsByCommitteeAndDateAndHour(@Param("committeeId") Long committeeId,
                                            @Param("date") LocalDate date,
                                            @Param("hour") Time hour);

    // Validación de colisión excluyendo un evento (para updates)
    @Query("SELECT CASE WHEN COUNT(ce) > 0 THEN true ELSE false END FROM CommitteeEvent ce WHERE ce.committee.id = :committeeId AND ce.date = :date AND ce.hour = :hour AND ce.id <> :eventId")
    boolean existsAnotherByCommitteeAndDateAndHour(@Param("eventId") Long eventId,
                                                   @Param("committeeId") Long committeeId,
                                                   @Param("date") LocalDate date,
                                                   @Param("hour") Time hour);

    // Nuevo: obtener todos los eventos en misma fecha y hora (para validar personas repetidas)
    @Query("SELECT ce FROM CommitteeEvent ce WHERE ce.date = :date AND ce.hour = :hour")
    List<CommitteeEvent> findAllByDateAndHour(@Param("date") LocalDate date, @Param("hour") Time hour);

    // Método para obtener novedades asociadas a un evento del comité
    @Query("SELECT n FROM Novelty n WHERE n.committeeEventId = :eventId")
    List<Novelty> findNoveltiesByEventId(@Param("eventId") Long eventId);

    // Método para encontrar eventos sin comité asignado
    List<CommitteeEvent> findByCommitteeIsNull();

    // Método optimizado para obtener múltiples eventos por IDs
    @Query("SELECT ce FROM CommitteeEvent ce WHERE ce.id IN :eventIds")
    List<CommitteeEvent> findAllByIdIn(@Param("eventIds") List<Long> eventIds);
}
