package co.sena.edu.themis.Strategy.Impl;

import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Entity.NoveltyStatus;
import co.sena.edu.themis.Entity.ProcessFlowStatus;
import co.sena.edu.themis.Service.NoveltyStatusService;
import co.sena.edu.themis.Service.ProcessFlowStatusService;
import co.sena.edu.themis.Strategy.NoveltyValidationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransfersValidationStrategy implements NoveltyValidationStrategy {

    @Autowired
    private NoveltyStatusService noveltyStatusService;

    @Autowired
    private ProcessFlowStatusService processFlowStatusService;

    @Override
    public Novelty validateAndProcess(Novelty novelty) {
        // Para traslados, solo validamos si se aprueba o no
        // Por defecto lo ponemos en proceso para revisión
        return processForReview(novelty);
    }

    @Override
    public boolean canHandle(String noveltyType) {
        return "TRANSFERS".equalsIgnoreCase(noveltyType) ||
               "TRASLADO".equalsIgnoreCase(noveltyType);
    }

    private Novelty processForReview(Novelty novelty) {
        NoveltyStatus enProcesoStatus = noveltyStatusService.findByName("en proceso");
        ProcessFlowStatus coordinationFlow = processFlowStatusService.findByName("coordination");

        novelty.setNoveltyStatus(enProcesoStatus);
        novelty.setProcessFlowStatus(coordinationFlow);
        novelty.setObservation("Solicitud de traslado en proceso de revisión");

        return novelty;
    }

    /**
     * Método para aprobar un traslado
     */
    public Novelty approveTransfer(Novelty novelty, String observation) {
        NoveltyStatus aprobadoStatus = noveltyStatusService.findByName("aprobado");

        novelty.setNoveltyStatus(aprobadoStatus);
        novelty.setObservation(observation != null ? observation : "Traslado aprobado");

        return novelty;
    }

    /**
     * Método para rechazar un traslado
     */
    public Novelty rejectTransfer(Novelty novelty, String observation) {
        NoveltyStatus noAprobadoStatus = noveltyStatusService.findByName("no aprobado");

        novelty.setNoveltyStatus(noAprobadoStatus);
        novelty.setObservation(observation != null ? observation : "Traslado no aprobado");

        return novelty;
    }
}
