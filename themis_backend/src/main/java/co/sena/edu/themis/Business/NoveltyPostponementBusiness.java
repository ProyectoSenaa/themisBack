package co.sena.edu.themis.Business;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Strategy.Impl.PostponementValidationStrategy;
import co.sena.edu.themis.Service.NoveltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoveltyPostponementBusiness {

    @Autowired
    private PostponementValidationStrategy postponementStrategy;

    @Autowired
    private NoveltyService noveltyService;

    /**
     * Procesa una solicitud de aplazamiento
     * @param novelty La novedad de aplazamiento a procesar
     * @return La novedad procesada con el estado actualizado
     */
    public Novelty processNovelty(Novelty novelty) {
        // Aplicar validaciones específicas de aplazamiento
        Novelty processedNovelty = postponementStrategy.validateAndProcess(novelty);

        // Guardar la novedad procesada
        return noveltyService.save(processedNovelty);
    }

    /**
     * Aprueba una solicitud de aplazamiento
     * @param noveltyId ID de la novedad
     * @param observation Observación de la aprobación
     * @return La novedad aprobada
     */
    public Novelty approvePostponement(Long noveltyId, String observation) {
        Novelty novelty = noveltyService.getById(noveltyId);
        Novelty approvedNovelty = postponementStrategy.approvePostponement(novelty, observation);
        return noveltyService.save(approvedNovelty);
    }

    /**
     * Rechaza una solicitud de aplazamiento
     * @param noveltyId ID de la novedad
     * @param observation Observación del rechazo
     * @return La novedad rechazada
     */
    public Novelty rejectPostponement(Long noveltyId, String observation) {
        Novelty novelty = noveltyService.getById(noveltyId);
        Novelty rejectedNovelty = postponementStrategy.rejectPostponement(novelty, observation);
        return noveltyService.save(rejectedNovelty);
    }
}
