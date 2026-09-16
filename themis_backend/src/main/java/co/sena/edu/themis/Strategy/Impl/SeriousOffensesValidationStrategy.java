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

@Component
public class SeriousOffensesValidationStrategy implements FollowUpValidationStrategy {

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
        return "SERIOUS_OFFENSES".equalsIgnoreCase(followUpTypeName) ||
               "FALTAS_GRAVES".equalsIgnoreCase(followUpTypeName) ||
               "Faltas Graves".equalsIgnoreCase(followUpTypeName);
    }

    private String getFichaFromOlympo(Long studentId) {
        try {
            PersonResponse personResponse = personGrpcService.getPersonById(studentId);
            if (personResponse.getFound()) {
                return String.valueOf(personResponse.getDocument());
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error obteniendo ficha desde Olympo para studentId: " + studentId + " - " + e.getMessage());
            return null;
        }
    }

    private FollowUp processDirectCoordinationFollowUp(FollowUp followUp) {
        FollowUpStatus enRevision = followUpStatusService.findByName("en revision").orElse(null);
        FollowUpFlowStatus coordinacion = followUpFlowStatusService.findByName("coordinacion");

        if (enRevision != null) followUp.setFollowUpStatus(enRevision);
        if (coordinacion != null) followUp.setFollowUpFlowStatus(coordinacion);

        // Usar caseDescription en lugar de setObservation
        if (followUp.getCaseDescription() == null || followUp.getCaseDescription().isEmpty()) {
            followUp.setCaseDescription("Ficha antigua detectada - procesamiento directo por coordinación para falta grave");
        } else {
            followUp.setCaseDescription(followUp.getCaseDescription() + " - Ficha antigua detectada - procesamiento directo por coordinación para falta grave");
        }

        return followUp;
    }

    private FollowUp processCommitteeReviewFollowUp(FollowUp followUp) {
        // Verificar también usando el método específico de gRPC
        boolean requiresCommittee = personGrpcService.fichaRequiresCommittee(followUp.getStudentId());

        FollowUpStatus enRevision = followUpStatusService.findByName("en revision").orElse(null);
        FollowUpFlowStatus flow;
        String caseDescriptionAddition;

        if (requiresCommittee) {
            flow = followUpFlowStatusService.findByName("comite");
            caseDescriptionAddition = "Ficha nueva detectada - requiere revisión por comité para falta grave (confirmado por Olympo)";
        } else {
            flow = followUpFlowStatusService.findByName("coordinacion");
            caseDescriptionAddition = "Procesamiento directo autorizado por Olympo para falta grave";
        }

        if (enRevision != null) followUp.setFollowUpStatus(enRevision);
        if (flow != null) followUp.setFollowUpFlowStatus(flow);

        // Usar caseDescription en lugar de setObservation
        if (followUp.getCaseDescription() == null || followUp.getCaseDescription().isEmpty()) {
            followUp.setCaseDescription(caseDescriptionAddition);
        } else {
            followUp.setCaseDescription(followUp.getCaseDescription() + " - " + caseDescriptionAddition);
        }

        return followUp;
    }

    private FollowUp processCoordinationReviewFollowUp(FollowUp followUp) {
        FollowUpStatus enRevision = followUpStatusService.findByName("en revision").orElse(null);
        FollowUpFlowStatus coordinacion = followUpFlowStatusService.findByName("coordinacion");

        if (enRevision != null) followUp.setFollowUpStatus(enRevision);
        if (coordinacion != null) followUp.setFollowUpFlowStatus(coordinacion);

        // Usar caseDescription en lugar de setObservation
        if (followUp.getCaseDescription() == null || followUp.getCaseDescription().isEmpty()) {
            followUp.setCaseDescription("Ficha no válida o error de acceso - procesamiento por coordinación para falta grave");
        } else {
            followUp.setCaseDescription(followUp.getCaseDescription() + " - Ficha no válida o error de acceso - procesamiento por coordinación para falta grave");
        }

        return followUp;
    }
}
