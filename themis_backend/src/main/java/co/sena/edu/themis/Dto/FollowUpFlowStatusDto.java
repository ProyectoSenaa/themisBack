package co.sena.edu.themis.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un estado dentro del flujo de seguimiento (FollowUp).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowUpFlowStatusDto {
    /** Identificador único del estado en la tabla */
    private Long id;

    /**
     Nombre legible del estado.
     Ejemplos: "En progreso", "Completado", "Pendiente".
     */
    private String name;

    /**
     Descripción opcional para detallar el significado del estado.
     */
    private String description;
}