package co.sena.edu.themis.Strategy;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Strategy.NoveltyValidationStrategy;
import co.sena.edu.themis.Entity.NoveltyStatus;
import co.sena.edu.themis.Repository.NoveltyStatusRepository;
import co.sena.edu.themis.Service.ProcessFlowStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NoveltyValidationContext {

    private final List<NoveltyValidationStrategy> validationStrategies;
    private final NoveltyStatusRepository noveltyStatusRepository;
    private final ProcessFlowStatusService processFlowStatusService;

    @Autowired
    public NoveltyValidationContext(List<NoveltyValidationStrategy> validationStrategies,
                                  NoveltyStatusRepository noveltyStatusRepository,
                                  ProcessFlowStatusService processFlowStatusService) {
        this.validationStrategies = validationStrategies;
        this.noveltyStatusRepository = noveltyStatusRepository;
        this.processFlowStatusService = processFlowStatusService;
    }

    /**
     * Valida y procesa una novedad utilizando la estrategia apropiada
     * @param novelty La novedad a validar y procesar
     * @return La novedad procesada con el estado actualizado
     */
    public Novelty validateAndProcess(Novelty novelty) {
        if (novelty.getNoveltyType() == null) {
            throw new IllegalArgumentException("El tipo de novedad no puede ser nulo");
        }

        String noveltyTypeName = novelty.getNoveltyType().getNameNovelty();

        Optional<NoveltyValidationStrategy> strategy = validationStrategies.stream()
                .filter(s -> s.canHandle(noveltyTypeName))
                .findFirst();

        if (strategy.isPresent()) {
            return strategy.get().validateAndProcess(novelty);
        } else {
            // Estrategia por defecto si no se encuentra una específica
            return processDefault(novelty);
        }
    }

    /**
     * Obtiene la estrategia apropiada para un tipo de novedad
     * @param noveltyType El tipo de novedad
     * @return La estrategia correspondiente si existe
     */
    public Optional<NoveltyValidationStrategy> getStrategy(String noveltyType) {
        return validationStrategies.stream()
                .filter(s -> s.canHandle(noveltyType))
                .findFirst();
    }

    /**
     * Procesamiento por defecto cuando no se encuentra una estrategia específica
     */
    private Novelty processDefault(Novelty novelty) {
        // Asignar estado inicial PENDIENTE por defecto
        String pendingStatusName = "pendiente";
        NoveltyStatus pendingStatus = noveltyStatusRepository.findByName(pendingStatusName)
                .orElseThrow(() -> new IllegalStateException("Estado 'pendiente' no encontrado en la base de datos"));

        novelty.setNoveltyStatus(pendingStatus);
        // Asignar flujo de proceso por defecto: "pendiente"
        novelty.setProcessFlowStatus(processFlowStatusService.findByName("pendiente"));
        novelty.setObservation("Tipo de novedad no tiene validación específica implementada");
        return novelty;
    }
}
