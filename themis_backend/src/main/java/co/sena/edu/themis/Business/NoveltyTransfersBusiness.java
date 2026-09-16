package co.sena.edu.themis.Business;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Strategy.Impl.TransfersValidationStrategy;
import co.sena.edu.themis.Service.NoveltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoveltyTransfersBusiness {

    @Autowired
    private TransfersValidationStrategy transfersStrategy;

    @Autowired
    private NoveltyService noveltyService;

    /**
     * Procesa una solicitud de traslado
     * @param novelty La novedad de traslado a procesar
     * @return La novedad procesada con el estado actualizado
     */
    public Novelty processNovelty(Novelty novelty) {
        // Aplicar validaciones específicas de traslado
        Novelty processedNovelty = transfersStrategy.validateAndProcess(novelty);

        // Guardar la novedad procesada
        return noveltyService.save(processedNovelty);
    }

    /**
     * Aprueba una solicitud de traslado
     * @param noveltyId ID de la novedad
     * @param observation Observación de la aprobación
     * @return La novedad aprobada
     */
    public Novelty approveTransfer(Long noveltyId, String observation) {
        Novelty novelty = noveltyService.getById(noveltyId);
        Novelty approvedNovelty = transfersStrategy.approveTransfer(novelty, observation);
        return noveltyService.save(approvedNovelty);
    }

    /**
     * Rechaza una solicitud de traslado
     * @param noveltyId ID de la novedad
     * @param observation Observación del rechazo
     * @return La novedad rechazada
     */
    public Novelty rejectTransfer(Long noveltyId, String observation) {
        Novelty novelty = noveltyService.getById(noveltyId);
        Novelty rejectedNovelty = transfersStrategy.rejectTransfer(novelty, observation);
        return noveltyService.save(rejectedNovelty);
    }
}
