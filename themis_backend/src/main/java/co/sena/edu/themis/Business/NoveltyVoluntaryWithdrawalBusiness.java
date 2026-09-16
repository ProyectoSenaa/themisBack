package co.sena.edu.themis.Business;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Strategy.Impl.VoluntaryWithdrawalValidationStrategy;
import co.sena.edu.themis.Service.NoveltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoveltyVoluntaryWithdrawalBusiness {

    @Autowired
    private VoluntaryWithdrawalValidationStrategy voluntaryWithdrawalStrategy;

    @Autowired
    private NoveltyService noveltyService;

    /**
     * Procesa una solicitud de retiro voluntario.
     * @param novelty La novedad de retiro voluntario a procesar.
     * @return La novedad procesada con el estado actualizado.
     */
    public Novelty processNovelty(Novelty novelty) {
        // Aplicar validaciones específicas de retiro voluntario.
        Novelty processedNovelty = voluntaryWithdrawalStrategy.validateAndProcess(novelty);

        // Guardar la novedad procesada.
        return noveltyService.save(processedNovelty);
    }
}