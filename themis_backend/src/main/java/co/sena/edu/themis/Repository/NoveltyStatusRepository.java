package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.NoveltyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoveltyStatusRepository extends JpaRepository<NoveltyStatus, Long> {

    /**
     * Busca un estado de novedad por su nombre
     * @param name El nombre del estado
     * @return Optional con el estado encontrado
     */
    Optional<NoveltyStatus> findByName(String name);

    /**
     * Verifica si existe un estado con el nombre dado
     * @param name El nombre del estado
     * @return true si existe, false en caso contrario
     */
    boolean existsByName(String name);
}
