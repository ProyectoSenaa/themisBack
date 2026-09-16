package co.sena.edu.themis.Business;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Strategy.Impl.DropoutValidationStrategy; // Usamos la estrategia de deserción
import co.sena.edu.themis.Service.NoveltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoveltyDropoutBusiness {

    @Autowired
    private DropoutValidationStrategy dropoutStrategy;

    @Autowired
    private NoveltyService noveltyService;

    /**
     * Procesa una solicitud de deserción.
     * @param novelty La novedad de deserción a procesar.
     * @return La novedad procesada con el estado actualizado.
     */
    public Novelty processNovelty(Novelty novelty) {
        // Aplicar validaciones específicas de deserción.
        Novelty processedNovelty = dropoutStrategy.validateAndProcess(novelty);

        // Guardar la novedad procesada.
        return noveltyService.save(processedNovelty);
    }
}