package co.sena.edu.themis.Strategy.Impl;

import co.sena.edu.olympo_back.proto.PersonResponse;
import co.sena.edu.themis.Entity.FollowUp;
import co.sena.edu.themis.Entity.FollowUpStatus;
import co.sena.edu.themis.Entity.FollowUpFlowStatus;
import co.sena.edu.themis.Service.FollowUpStatusService;
import co.sena.edu.themis.Service.FollowUpFlowStatusService;
import co.sena.edu.themis.Service.PersonGrpcService;
import co.sena.edu.themis.Strategy.FollowUpValidationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BadPerformanceValidationStrategy implements FollowUpValidationStrategy {

    @Autowired
    private PersonGrpcService personGrpcService;

    @Autowired
    private FollowUpStatusService followUpStatusService;

    @Autowired
    private FollowUpFlowStatusService followUpFlowStatusService;

    @Override
    public FollowUp validateAndProcess(FollowUp followUp) {
        String ficha = getFichaFromOlympo(followUp.getStudentId());

        if (ficha != null) {
            if (ficha.startsWith("30")) {
                return processDirectCoordinationFollowUp(followUp);
            } else if (ficha.startsWith("31")) {
                return processCommitteeReviewFollowUp(followUp);
            } else {
                return processCoordinationReviewFollowUp(followUp);
            }
        } else {
            return processCoordinationReviewFollowUp(followUp);
        }
    }

    @Override
    public boolean canHandle(String followUpTypeName) {
        return "Bajo Rendimiento Académico".equalsIgnoreCase(followUpTypeName) ||
               "BAD_PERFORMANCE".equalsIgnoreCase(followUpTypeName) ||
               "BAJO_RENDIMIENTO".equalsIgnoreCase(followUpTypeName);
    }

    private String getFichaFromOlympo(Long studentId) {
        try {
            PersonResponse personResponse = personGrpcService.getPersonById(studentId);
            if (personResponse.getFound()) {
                return String.valueOf(personResponse.getDocument());
            }
            return null;
        } catch (Exception e) {
            System.out.println("Error obteniendo ficha del estudiante: " + e.getMessage());
            return null;
        }
    }

    private FollowUp processDirectCoordinationFollowUp(FollowUp followUp) {
        Optional<FollowUpStatus> enRevision = followUpStatusService.findByName("en revision");
        Optional<FollowUpFlowStatus> coordinacion = Optional.ofNullable(followUpFlowStatusService.findByName("coordinacion"));

        enRevision.ifPresent(followUp::setFollowUpStatus);
        coordinacion.ifPresent(followUp::setFollowUpFlowStatus);

        // Usar caseDescription en lugar de setObservation
        if (followUp.getCaseDescription() == null || followUp.getCaseDescription().isEmpty()) {
            followUp.setCaseDescription("Ficha antigua detectada - procesamiento directo por coordinación");
        } else {
            followUp.setCaseDescription(followUp.getCaseDescription() + " - Ficha antigua detectada - procesamiento directo por coordinación");
        }

        return followUp;
    }

    private FollowUp processCommitteeReviewFollowUp(FollowUp followUp) {
        boolean requiresCommittee = personGrpcService.fichaRequiresCommittee(followUp.getStudentId());

        Optional<FollowUpStatus> enRevision = followUpStatusService.findByName("en revision");
        Optional<FollowUpFlowStatus> flow;
        String caseDescriptionAddition;

        if (requiresCommittee) {
            flow = Optional.ofNullable(followUpFlowStatusService.findByName("comite"));
            caseDescriptionAddition = "Ficha nueva detectada - requiere revisión por comité (confirmado por Olympo)";
        } else {
            flow = Optional.ofNullable(followUpFlowStatusService.findByName("coordinacion"));
            caseDescriptionAddition = "Procesamiento directo autorizado por Olympo";
        }

        enRevision.ifPresent(followUp::setFollowUpStatus);
        flow.ifPresent(followUp::setFollowUpFlowStatus);

        if (followUp.getCaseDescription() == null || followUp.getCaseDescription().isEmpty()) {
            followUp.setCaseDescription(caseDescriptionAddition);
        } else {
            followUp.setCaseDescription(followUp.getCaseDescription() + " - " + caseDescriptionAddition);
        }

        return followUp;
    }

    private FollowUp processCoordinationReviewFollowUp(FollowUp followUp) {
        Optional<FollowUpStatus> enRevision = followUpStatusService.findByName("en revision");
        Optional<FollowUpFlowStatus> coordinacion = Optional.ofNullable(followUpFlowStatusService.findByName("coordinacion"));

        enRevision.ifPresent(followUp::setFollowUpStatus);
        coordinacion.ifPresent(followUp::setFollowUpFlowStatus);

        if (followUp.getCaseDescription() == null || followUp.getCaseDescription().isEmpty()) {
            followUp.setCaseDescription("Ficha no válida o error de acceso - procesamiento por coordinación");
        } else {
            followUp.setCaseDescription(
                    followUp.getCaseDescription() + " - Ficha no válida o error de acceso - procesamiento por coordinación"
            );
        }

        return followUp;
    }
}
