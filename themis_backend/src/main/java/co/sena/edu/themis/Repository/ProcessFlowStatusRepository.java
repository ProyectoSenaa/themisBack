package co.sena.edu.themis.Repository;

import co.sena.edu.themis.Entity.ProcessFlowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessFlowStatusRepository extends JpaRepository<ProcessFlowStatus, Long> {

    /**
     * Busca un estado de flujo de proceso por su nombre
     * @param name El nombre del estado del flujo
     * @return Optional con el estado encontrado
     */
    Optional<ProcessFlowStatus> findByName(String name);

    /**
     * Verifica si existe un estado de flujo con el nombre dado
     * @param name El nombre del estado del flujo
     * @return true si existe, false en caso contrario
     */
    boolean existsByName(String name);
}
