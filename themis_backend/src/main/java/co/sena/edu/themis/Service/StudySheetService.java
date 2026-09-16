package co.sena.edu.themis.Service;

import co.sena.edu.olympo_back.proto.StudySheetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;

@Service
public class StudySheetService {

    @Autowired
    private StudySheetGrpcService studySheetGrpcService;

    @Autowired
    private PersonGrpcService personGrpcService;

    /**
     * Obtiene información completa de una ficha de estudio
     * @param studySheetId ID de la ficha
     * @return StudySheetResponse con la información
     */
    public StudySheetResponse getStudySheetById(Long studySheetId) {
        return studySheetGrpcService.getStudySheetById(studySheetId);
    }

    /**
     * Valida si una ficha de estudio es válida para crear novedades
     * @param studySheetId ID de la ficha
     * @return true si es válida, false en caso contrario
     */
    public boolean isValidForNovelties(Long studySheetId) {
        StudySheetResponse studySheet = studySheetGrpcService.getStudySheetById(studySheetId);

        if (!studySheet.getFound() || !studySheet.getState()) {
            return false;
        }

        // Verificar si está en período lectivo activo
        return isInActiveLectivePeriod(studySheet);
    }

    /**
     * Obtiene el número de ficha formateado para mostrar
     * @param studySheetId ID de la ficha
     * @return String con el número formateado o "N/A" si no existe
     */
    public String getFormattedStudySheetNumber(Long studySheetId) {
        StudySheetResponse studySheet = studySheetGrpcService.getStudySheetById(studySheetId);

        if (!studySheet.getFound()) {
            return "N/A";
        }

        return String.format("FICHA-%06d", studySheet.getNumber());
    }

    /**
     * Verifica si una ficha requiere comité basado en su configuración
     * @param studySheetId ID de la ficha
     * @return true si requiere comité, false si es proceso directo
     */
    public boolean requiresCommittee(Long studySheetId) {
        StudySheetResponse studySheet = studySheetGrpcService.getStudySheetById(studySheetId);

        if (!studySheet.getFound()) {
            return true; // Por defecto requerir comité si no se encuentra
        }

        // Lógica de negocio: fichas con más de 30 estudiantes requieren comité
        // o fichas con números específicos (los que empiecen con 31)
        return studySheet.getNumberStudents() > 30 ||
               String.valueOf(studySheet.getNumber()).startsWith("31");
    }

    /**
     * Obtiene información resumida de múltiples fichas
     * @param studySheetIds Lista de IDs de fichas
     * @return Lista con información resumida
     */
    public List<StudySheetSummary> getStudySheetsSummary(List<Long> studySheetIds) {
        List<StudySheetSummary> summaries = new ArrayList<>();

        for (Long studySheetId : studySheetIds) {
            StudySheetResponse studySheet = studySheetGrpcService.getStudySheetById(studySheetId);

            StudySheetSummary summary = new StudySheetSummary();
            summary.setStudySheetId(studySheetId);
            summary.setFound(studySheet.getFound());

            if (studySheet.getFound()) {
                summary.setNumber(studySheet.getNumber());
                summary.setFormattedNumber(getFormattedStudySheetNumber(studySheetId));
                summary.setNumberStudents(studySheet.getNumberStudents());
                summary.setActive(studySheet.getState());
                summary.setRequiresCommittee(requiresCommittee(studySheetId));
                summary.setValidForNovelties(isValidForNovelties(studySheetId));
            }

            summaries.add(summary);
        }

        return summaries;
    }

    /**
     * Verifica si la ficha está en período lectivo activo
     * @param studySheet Respuesta de la ficha
     * @return true si está activa, false en caso contrario
     */
    private boolean isInActiveLectivePeriod(StudySheetResponse studySheet) {
        try {
            LocalDate now = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

            LocalDate startDate = LocalDate.parse(studySheet.getStartLective(), formatter);
            LocalDate endDate = LocalDate.parse(studySheet.getEndLective(), formatter);

            return !now.isBefore(startDate) && !now.isAfter(endDate);
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing dates for study sheet " + studySheet.getStudySheetId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Clase interna para el resumen de fichas
     */
    public static class StudySheetSummary {
        private Long studySheetId;
        private boolean found;
        private int number;
        private String formattedNumber;
        private int numberStudents;
        private boolean active;
        private boolean requiresCommittee;
        private boolean validForNovelties;

        // Getters y Setters
        public Long getStudySheetId() { return studySheetId; }
        public void setStudySheetId(Long studySheetId) { this.studySheetId = studySheetId; }

        public boolean isFound() { return found; }
        public void setFound(boolean found) { this.found = found; }

        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }

        public String getFormattedNumber() { return formattedNumber; }
        public void setFormattedNumber(String formattedNumber) { this.formattedNumber = formattedNumber; }

        public int getNumberStudents() { return numberStudents; }
        public void setNumberStudents(int numberStudents) { this.numberStudents = numberStudents; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public boolean isRequiresCommittee() { return requiresCommittee; }
        public void setRequiresCommittee(boolean requiresCommittee) { this.requiresCommittee = requiresCommittee; }

        public boolean isValidForNovelties() { return validForNovelties; }
        public void setValidForNovelties(boolean validForNovelties) { this.validForNovelties = validForNovelties; }
    }
}
