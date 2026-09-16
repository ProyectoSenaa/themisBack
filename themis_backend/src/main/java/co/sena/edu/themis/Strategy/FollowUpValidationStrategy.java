package co.sena.edu.themis.Strategy;

import co.sena.edu.themis.Entity.FollowUp;

public interface FollowUpValidationStrategy {
    
    /**
     * Valida y procesa un seguimiento académico según las reglas específicas del tipo
     * @param followUp El seguimiento a validar y procesar
     * @return El seguimiento procesado con los estados actualizados
     */
    FollowUp validateAndProcess(FollowUp followUp);
    
    /**
     * Determina si esta estrategia puede manejar el tipo de seguimiento dado
     * @param followUpTypeName El nombre del tipo de seguimiento
     * @return true si la estrategia puede manejar este tipo
     */
    boolean canHandle(String followUpTypeName);
}
