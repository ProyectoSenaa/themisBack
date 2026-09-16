package co.sena.edu.themis.Strategy.Impl;


import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Entity.NoveltyStatus;
import co.sena.edu.themis.Entity.ProcessFlowStatus;
import co.sena.edu.themis.Service.NoveltyStatusService;
import co.sena.edu.themis.Service.ProcessFlowStatusService;
import co.sena.edu.themis.Service.PersonGrpcService;
import co.sena.edu.themis.Strategy.NoveltyValidationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.log4j.Logger;

@Component
public class ReinstatementValidationStrategy implements NoveltyValidationStrategy {

    private static final Logger logger = Logger.getLogger(ReinstatementValidationStrategy.class);

    @Autowired
    private PersonGrpcService personGrpcService;

    @Autowired
    private NoveltyStatusService noveltyStatusService;

    @Autowired
    private ProcessFlowStatusService processFlowStatusService;

    @Override
    public Novelty validateAndProcess(Novelty novelty) {
        logger.info("Iniciando validación de novedad de reintegro.");

        String fichaNumber = getFichaFromOlympo(novelty.getStudentId());

        if (fichaNumber != null) {
            logger.info("Ficha del estudiante para reintegro obtenida: " + fichaNumber);
        } else {
            logger.warn("No se pudo obtener el número de ficha para la novedad de reintegro.");

        }

        return processCommitteeReview(novelty);
    }

    @Override
    public boolean canHandle(String noveltyType) {
        return "REINSTATEMENT".equalsIgnoreCase(noveltyType);
    }

    private String getFichaFromOlympo(Long studentId) {
        try {
            return personGrpcService.getStudentFichaNumber(studentId);
        } catch (Exception e) {
            logger.error("Error obteniendo ficha desde Olympo para studentId: " + studentId, e);
            return null;
        }
    }

    private Novelty processCommitteeReview(Novelty novelty) {
        NoveltyStatus enProcesoStatus = noveltyStatusService.findByName("en proceso");
        ProcessFlowStatus coordinationFlow = processFlowStatusService.findByName("coordinacion");

        novelty.setNoveltyStatus(enProcesoStatus);
        novelty.setProcessFlowStatus(coordinationFlow);
        novelty.setObservation("Novedad de reintegro - requiere revisión de Coordinación Académica.");

        return novelty;
    }

    private Novelty processRejection(Novelty novelty, String reason) {
        NoveltyStatus denegadoStatus = noveltyStatusService.findByName("denegado");

        novelty.setNoveltyStatus(denegadoStatus);
        novelty.setObservation(reason);

        return novelty;
    }
}