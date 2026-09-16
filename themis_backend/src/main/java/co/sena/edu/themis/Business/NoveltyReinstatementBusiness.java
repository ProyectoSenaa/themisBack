package co.sena.edu.themis.Business;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Strategy.Impl.ReinstatementValidationStrategy;
import co.sena.edu.themis.Service.NoveltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoveltyReinstatementBusiness {

    @Autowired
    private ReinstatementValidationStrategy reinstatementStrategy;

    @Autowired
    private NoveltyService noveltyService;

    /**
     * Procesa una solicitud de reintegro.
     * @param novelty La novedad de reintegro a procesar.
     * @return La novedad procesada con el estado actualizado.
     */
    public Novelty processNovelty(Novelty novelty) {

        Novelty processedNovelty = reinstatementStrategy.validateAndProcess(novelty);

        return noveltyService.save(processedNovelty);
    }
}