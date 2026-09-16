package co.sena.edu.themis.Business;

import co.sena.edu.themis.Entity.FollowUp;
import co.sena.edu.themis.Strategy.FollowUpValidationContext;
import co.sena.edu.themis.Service.FollowUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoveltySeriousOffensesBusiness {

    @Autowired
    private FollowUpValidationContext validationContext;

    @Autowired
    private FollowUpService followUpService;

    /**
     * Procesa un seguimiento de falta grave aplicando las validaciones de ficha
     * @param followUp El seguimiento a procesar
     * @return El seguimiento procesado con el estado actualizado
     */
    public FollowUp processFollowUp(FollowUp followUp) {
        // Aplicar validaciones específicas de faltas graves
        FollowUp processedFollowUp = validationContext.validateAndProcess(followUp);

        // Guardar el seguimiento procesado
        return followUpService.save(processedFollowUp);
    }

    /**
     * Valida si una ficha es antigua (empieza con 30) o nueva (empieza con 31)
     * @param followUp El seguimiento que contiene el ID del estudiante
     * @return true si la ficha es válida, false en caso contrario
     */
    public boolean validateFicha(FollowUp followUp) {
        try {
            // Usar el contexto de validación para validar
            FollowUp validatedFollowUp = validationContext.validateAndProcess(followUp);
            return !validatedFollowUp.getFollowUpStatus().getName().equals("rechazado");
        } catch (Exception e) {
            return false;
        }
    }
}
