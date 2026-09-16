package co.sena.edu.themis.Strategy;

import co.sena.edu.themis.Entity.FollowUp;
import co.sena.edu.themis.Entity.FollowUpStatus;
import co.sena.edu.themis.Repository.FollowUpStatusRepository;
import co.sena.edu.themis.Service.FollowUpFlowStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class FollowUpValidationContext {
    
    private final List<FollowUpValidationStrategy> validationStrategies;
    private final FollowUpStatusRepository followUpStatusRepository;
    private final FollowUpFlowStatusService followUpFlowStatusService;

    @Autowired
    public FollowUpValidationContext(List<FollowUpValidationStrategy> validationStrategies,
                                   FollowUpStatusRepository followUpStatusRepository,
                                   FollowUpFlowStatusService followUpFlowStatusService) {
        this.validationStrategies = validationStrategies;
        this.followUpStatusRepository = followUpStatusRepository;
        this.followUpFlowStatusService = followUpFlowStatusService;
    }

    /**
     * Valida y procesa un seguimiento utilizando la estrategia apropiada
     * @param followUp El seguimiento a validar y procesar
     * @return El seguimiento procesado con el estado actualizado
     */
    public FollowUp validateAndProcess(FollowUp followUp) {
        if (followUp.getFollowUpType() == null) {
            throw new IllegalArgumentException("El tipo de seguimiento no puede ser nulo");
        }

        String followUpTypeName = followUp.getFollowUpType().getName();

        Optional<FollowUpValidationStrategy> strategy = validationStrategies.stream()
                .filter(s -> s.canHandle(followUpTypeName))
                .findFirst();

        if (strategy.isPresent()) {
            return strategy.get().validateAndProcess(followUp);
        } else {
            // Estrategia por defecto si no se encuentra una específica
            return processDefault(followUp);
        }
    }

    /**
     * Busca y ejecuta la estrategia apropiada para el tipo de seguimiento
     * @param followUp El seguimiento a procesar
     * @return El seguimiento procesado con estados actualizados
     * @deprecated Use validateAndProcess instead
     */
    @Deprecated
    public FollowUp processFollowUp(FollowUp followUp) {
        return validateAndProcess(followUp);
    }

    /**
     * Obtiene la estrategia apropiada para un tipo de seguimiento
     * @param followUpTypeName El nombre del tipo de seguimiento
     * @return La estrategia correspondiente si existe
     */
    public Optional<FollowUpValidationStrategy> getStrategy(String followUpTypeName) {
        return validationStrategies.stream()
                .filter(s -> s.canHandle(followUpTypeName))
                .findFirst();
    }

    /**
     * Procesamiento por defecto cuando no se encuentra una estrategia específica
     */
    private FollowUp processDefault(FollowUp followUp) {
        // Asignar estado inicial "en revision" por defecto
        FollowUpStatus defaultStatus = followUpStatusRepository.findByName("en revision").orElse(null);
        if (defaultStatus != null) {
            followUp.setFollowUpStatus(defaultStatus);
        }

        // Asignar flujo de coordinación por defecto
        followUp.setFollowUpFlowStatus(followUpFlowStatusService.findByName("coordinacion"));

        // Usar caseDescription en lugar de setObservation
        if (followUp.getCaseDescription() == null || followUp.getCaseDescription().isEmpty()) {
            followUp.setCaseDescription("Procesado con estrategia por defecto - requiere revisión manual");
        }

        return followUp;
    }
}
