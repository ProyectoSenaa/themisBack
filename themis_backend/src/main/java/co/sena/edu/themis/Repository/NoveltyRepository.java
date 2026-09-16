package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Dto.NoveltyDto;
import co.sena.edu.themis.Entity.Novelty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;

@Repository
public interface NoveltyRepository extends JpaRepository<Novelty, Long> {

    Boolean existsById(long novelty);



    @Query("SELECT n.noveltyType.nameNovelty FROM Novelty n")
    List<String> findAllNoveltyTypesOnly();

    @Query("""
        SELECT n FROM Novelty n
        JOIN FETCH n.noveltyType
    """)
    List<Novelty> findAllWithType();

    @Query("""
    SELECT n 
    FROM Novelty n 
    LEFT JOIN FETCH n.noveltyType
""")
    List<Novelty> findAllWithTypeForced();

    @Query("SELECT n FROM Novelty n WHERE n.studentId = :studentId")
    List<Novelty> findAllByIdStudent(@Param("studentId") Long studentId);

    @Query("SELECT n FROM Novelty n WHERE n.teacherId = :teacherId")
    List<Novelty> findAllByIdTeacher(@Param("teacherId") Long teacherId);

    @Query("SELECT n FROM Novelty n WHERE n.administrativeId = :administrativeId")
    List<Novelty> findAllByIdAdministrative(@Param("administrativeId") Long administrativeId);

    @Query("""
        SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
        FROM Novelty n
        LEFT JOIN n.noveltyStatus s
        WHERE n.studentId = :studentId
          AND (n.isActive = true OR LOWER(s.name) IN :blockingStatuses)
    """)
    boolean existsBlockingForStudent(@Param("studentId") Long studentId,
                                     @Param("blockingStatuses") List<String> blockingStatuses);

    List<Novelty> findByStudentIdIn(List<Long> studentIds);

    @Query("""
        SELECT n FROM Novelty n
        JOIN n.noveltyStatus s
        WHERE UPPER(s.name) = 'PENDIENTE'
          AND n.date <= :maxDate
    """)
    List<Novelty> findPendingOlderThan(@Param("maxDate") LocalDate maxDate);

    // Métodos para asociación con eventos del comité
    @Query("SELECT n FROM Novelty n WHERE n.committeeEventId = :committeeEventId")
    List<Novelty> findByCommitteeEventId(@Param("committeeEventId") Long committeeEventId);

    @Query("SELECT n FROM Novelty n WHERE n.studentId IN :studentIds AND n.committeeEventId IS NULL")
    List<Novelty> findByStudentIdsWithoutCommitteeEvent(@Param("studentIds") List<Long> studentIds);
}
