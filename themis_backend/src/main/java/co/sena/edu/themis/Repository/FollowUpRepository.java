package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.FollowUp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    
    // Consulta con JOIN FETCH para paginación
    @Query(value = """
        SELECT DISTINCT f FROM FollowUp f
        LEFT JOIN FETCH f.followUpType
        LEFT JOIN FETCH f.followUpStatus
        LEFT JOIN FETCH f.followUpFlowStatus
    """, countQuery = """
        SELECT COUNT(DISTINCT f) FROM FollowUp f
    """)
    Page<FollowUp> findAllWithRelations(Pageable pageable);

    // Consultas para el Módulo de Gestión de Casos
    @Query("""
        SELECT f FROM FollowUp f
        JOIN FETCH f.followUpType
        JOIN FETCH f.followUpStatus
        WHERE f.isActive = true
        ORDER BY f.creationDate DESC
    """)
    List<FollowUp> findAllActiveCases();
    
    @Query("""
        SELECT f FROM FollowUp f
        WHERE f.studentId = :studentId AND f.isActive = true
        ORDER BY f.creationDate DESC
    """)
    List<FollowUp> findByStudentIdAndActiveTrue(@Param("studentId") Long studentId);
    
    @Query("""
        SELECT f FROM FollowUp f
        WHERE f.teacherId = :teacherId AND f.isActive = true
        ORDER BY f.creationDate DESC
    """)
    List<FollowUp> findByTeacherIdAndActiveTrue(@Param("teacherId") Long teacherId);

    // Consultas por coordinador
    @Query("""
        SELECT f FROM FollowUp f
        WHERE f.coordinatorId = :coordinatorId
        AND f.isActive = true
        ORDER BY f.creationDate DESC
    """)
    List<FollowUp> findByCoordinatorId(@Param("coordinatorId") Long coordinatorId);
    
    // Consultas por evento de comité
    @Query("""
        SELECT f FROM FollowUp f
        WHERE f.committeeEvent.id = :committeeEventId
        AND f.isActive = true
        ORDER BY f.creationDate DESC
    """)
    List<FollowUp> findByCommitteeEventId(@Param("committeeEventId") Long committeeEventId);
    
    // Consultas con relación a Minute
    @Query("""
        SELECT f FROM FollowUp f
        LEFT JOIN FETCH f.minute
        WHERE f.minute IS NOT NULL
        AND f.isActive = true
        ORDER BY f.creationDate DESC
    """)
    List<FollowUp> findCasesWithMinutes();

    // Estadísticas y reportes
    @Query("""
        SELECT COUNT(f) FROM FollowUp f
        WHERE f.followUpType.id = :typeId
        AND f.isActive = true
        AND f.creationDate BETWEEN :startDate AND :endDate
    """)
    Long countByTypeAndDateRange(
        @Param("typeId") Long typeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("""
        SELECT f.followUpType.name, COUNT(f) 
        FROM FollowUp f
        WHERE f.isActive = true
        GROUP BY f.followUpType.name
        ORDER BY COUNT(f) DESC
    """)
    List<Object[]> findFollowUpTypeStatistics();
}
