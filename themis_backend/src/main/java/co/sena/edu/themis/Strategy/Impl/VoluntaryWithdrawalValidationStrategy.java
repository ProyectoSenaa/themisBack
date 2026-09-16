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
public class VoluntaryWithdrawalValidationStrategy implements NoveltyValidationStrategy {

    private static final Logger logger = Logger.getLogger(VoluntaryWithdrawalValidationStrategy.class);

    @Autowired
    private PersonGrpcService personGrpcService; // Inyección del servicio correcto

    @Autowired
    private NoveltyStatusService noveltyStatusService;

    @Autowired
    private ProcessFlowStatusService processFlowStatusService;

    @Override
    public Novelty validateAndProcess(Novelty novelty) {
        logger.info("Iniciando validación de novedad de retiro voluntario.");

        String fichaNumber = getFichaFromOlympo(novelty.getStudentId());

        if (fichaNumber == null || fichaNumber.isEmpty()) {
            return processRejection(novelty, "No se pudo obtener el número de ficha del estudiante para retiro voluntario.");
        }

        if (fichaNumber.startsWith("30")) {
            logger.info("Ficha antigua detectada. El retiro voluntario será procesado por coordinación.");
            return processDirectApproval(novelty);
        } else if (fichaNumber.startsWith("31")) {
            logger.info("Ficha nueva detectada. El retiro voluntario requiere revisión del comité.");
            return processCommitteeReview(novelty);
        } else {

            return processRejection(novelty, "Número de ficha no válido para retiro voluntario.");
        }
    }

    @Override
    public boolean canHandle(String noveltyType) {
        return "VOLUNTARY_WITHDRAWAL".equalsIgnoreCase(noveltyType);
    }

    private String getFichaFromOlympo(Long studentId) {
        try {
            return personGrpcService.getStudentFichaNumber(studentId);
        } catch (Exception e) {
            logger.error("Error obteniendo ficha desde Olympo para studentId: " + studentId, e);
            return null;
        }
    }


    private Novelty processDirectApproval(Novelty novelty) {
        NoveltyStatus enProcesoStatus = noveltyStatusService.findByName("en proceso");
        ProcessFlowStatus coordinationFlow = processFlowStatusService.findByName("coordinacion");

        novelty.setNoveltyStatus(enProcesoStatus);
        novelty.setProcessFlowStatus(coordinationFlow);
        novelty.setObservation("Retiro voluntario en ficha antigua - procesado directamente por coordinación.");

        return novelty;
    }

    private Novelty processCommitteeReview(Novelty novelty) {
        NoveltyStatus enProcesoStatus = noveltyStatusService.findByName("en proceso");
        ProcessFlowStatus committeeFlow = processFlowStatusService.findByName("comite");

        novelty.setNoveltyStatus(enProcesoStatus);
        novelty.setProcessFlowStatus(committeeFlow);
        novelty.setObservation("Retiro voluntario en ficha nueva - requiere revisión por comité.");

        return novelty;
    }

    private Novelty processAlternativeReview(Novelty novelty) {
        NoveltyStatus enProcesoStatus = noveltyStatusService.findByName("en proceso");
        ProcessFlowStatus alternativeFlow = processFlowStatusService.findByName("revision_alternativa");

        novelty.setNoveltyStatus(enProcesoStatus);
        novelty.setProcessFlowStatus(alternativeFlow);
        novelty.setObservation("Retiro voluntario - requiere revisión alternativa.");

        return novelty;
    }

    private Novelty processRejection(Novelty novelty, String reason) {
        NoveltyStatus denegadoStatus = noveltyStatusService.findByName("denegado");

        novelty.setNoveltyStatus(denegadoStatus);
        novelty.setObservation(reason);

        return novelty;
    }
}