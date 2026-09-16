package co.sena.edu.themis.Strategy.Impl;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Entity.NoveltyStatus;
import co.sena.edu.themis.Service.NoveltyStatusService;
import co.sena.edu.themis.Strategy.NoveltyValidationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PostponementValidationStrategy implements NoveltyValidationStrategy {

    @Autowired
    private NoveltyStatusService noveltyStatusService;

    @Override
    public Novelty validateAndProcess(Novelty novelty) {
        // Para aplazamientos, generalmente se maneja por coordinación
        return processForReview(novelty);
    }

    @Override
    public boolean canHandle(String noveltyType) {
        return "POSTPONEMENT".equalsIgnoreCase(noveltyType) ||
                "APLAZAMIENTO".equalsIgnoreCase(noveltyType);
    }

    private Novelty processForReview(Novelty novelty) {
        NoveltyStatus enProcesoStatus = noveltyStatusService.findByName("en proceso");

        novelty.setNoveltyStatus(enProcesoStatus);
        novelty.setObservation("Aplazamiento en proceso de validación por coordinación");

        return novelty;
    }

    /**
     * Método para aprobar un aplazamiento
     */
    public Novelty approvePostponement(Novelty novelty, String observation) {
        NoveltyStatus aprobadoStatus = noveltyStatusService.findByName("aprobado");

        novelty.setNoveltyStatus(aprobadoStatus);
        novelty.setObservation(observation != null ? observation : "Aplazamiento aprobado");

        return novelty;
    }

    /**
     * Método para rechazar un aplazamiento
     */
    public Novelty rejectPostponement(Novelty novelty, String observation) {
        NoveltyStatus noAprobadoStatus = noveltyStatusService.findByName("no aprobado");

        novelty.setNoveltyStatus(noAprobadoStatus);
        novelty.setObservation(observation != null ? observation : "Aplazamiento no aprobado");

        return novelty;
    }
}