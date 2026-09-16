package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.Committee;
import org.springframework.data.jpa.repository.EntityGraph; // Importa EntityGraph
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // Importa Optional

@Repository
public interface CommitteeRepository extends JpaRepository<Committee, Long> {

    // Añade este método. Le dice a JPA que cargue las colecciones de IDs.
    @Override
    @EntityGraph(attributePaths = {"studentsIds", "teachersIds", "administrativesIds"})
    Optional<Committee> findById(Long id);

    boolean existsById(Long id);

    // Buscar comités activos
    List<Committee> findAllByIsActiveTrue();

    // Buscar comités por estado
    List<Committee> findAllByIsActive(Boolean isActive);

    // Buscar comités activos por coordinación
    List<Committee> findAllByCoordinationIdAndIsActiveTrue(Long coordinationId);

    // Buscar comités que contengan un estudiante específico en su lista
    @Query("SELECT c FROM Committee c WHERE :studentId MEMBER OF c.studentsIds")
    List<Committee> findCommitteesByStudentsIdsContaining(@Param("studentId") Long studentId);

    // Buscar comités que contengan un profesor específico en su lista
    @Query("SELECT c FROM Committee c WHERE :teacherId MEMBER OF c.teachersIds")
    List<Committee> findCommitteesByTeachersIdsContaining(@Param("teacherId") Long teacherId);

    // Buscar comités que contengan un administrativo específico en su lista
    @Query("SELECT c FROM Committee c WHERE :administrativeId MEMBER OF c.administrativesIds")
    List<Committee> findCommitteesByAdministrativesIdsContaining(@Param("administrativeId") Long administrativeId);


    // Buscar persona en cualquier rol dentro de los comités (aquí sí toca @Query)
    @Query("""
        SELECT c FROM Committee c 
        WHERE (:personId MEMBER OF c.studentsIds 
               OR :personId MEMBER OF c.teachersIds 
               OR :personId MEMBER OF c.administrativesIds)
        AND c.isActive = true
    """)
    List<Committee> findActiveCommitteesByPersonId(@Param("personId") Long personId);
}