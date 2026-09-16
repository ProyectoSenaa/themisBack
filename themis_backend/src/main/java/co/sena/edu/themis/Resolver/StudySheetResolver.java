package co.sena.edu.themis.Resolver;

import co.sena.edu.olympo_back.proto.StudySheetResponse;
import co.sena.edu.themis.Service.StudySheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-sheets")
@CrossOrigin(origins = "*")
@PreAuthorize("isAuthenticated()")
public class StudySheetResolver {

    @Autowired
    private StudySheetService studySheetService;

    /**
     * Obtiene información completa de una ficha de estudio por ID
     */
    @GetMapping("/{studySheetId}")
    public ResponseEntity<?> getStudySheetById(@PathVariable Long studySheetId) {
        try {
            StudySheetResponse studySheet = studySheetService.getStudySheetById(studySheetId);

            if (!studySheet.getFound()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(studySheet);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al obtener la ficha de estudio: " + e.getMessage());
        }
    }

    /**
     * Valida si una ficha es válida para crear novedades
     */
    @GetMapping("/{studySheetId}/validate-for-novelties")
    public ResponseEntity<?> validateForNovelties(@PathVariable Long studySheetId) {
        try {
            boolean isValid = studySheetService.isValidForNovelties(studySheetId);
            return ResponseEntity.ok(new ValidationResponse(isValid,
                    isValid ? "Ficha válida para novedades" : "Ficha no válida para novedades"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al validar la ficha: " + e.getMessage());
        }
    }

    /**
     * Obtiene el número formateado de una ficha
     */
    @GetMapping("/{studySheetId}/formatted-number")
    public ResponseEntity<?> getFormattedNumber(@PathVariable Long studySheetId) {
        try {
            String formattedNumber = studySheetService.getFormattedStudySheetNumber(studySheetId);
            return ResponseEntity.ok(new FormattedNumberResponse(formattedNumber));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al obtener el número formateado: " + e.getMessage());
        }
    }

    /**
     * Verifica si una ficha requiere comité
     */
    @GetMapping("/{studySheetId}/requires-committee")
    public ResponseEntity<?> requiresCommittee(@PathVariable Long studySheetId) {
        try {
            boolean requiresCommittee = studySheetService.requiresCommittee(studySheetId);
            return ResponseEntity.ok(new CommitteeRequirementResponse(requiresCommittee,
                    requiresCommittee ? "Requiere comité" : "Proceso directo"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al verificar requerimiento de comité: " + e.getMessage());
        }
    }

    /**
     * Obtiene resumen de múltiples fichas
     */
    @PostMapping("/summary")
    public ResponseEntity<?> getStudySheetsSummary(@RequestBody List<Long> studySheetIds) {
        try {
            List<StudySheetService.StudySheetSummary> summaries =
                    studySheetService.getStudySheetsSummary(studySheetIds);
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al obtener resumen de fichas: " + e.getMessage());
        }
    }

    // DTOs para las respuestas
    public static class ValidationResponse {
        private boolean valid;
        private String message;

        public ValidationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class FormattedNumberResponse {
        private String formattedNumber;

        public FormattedNumberResponse(String formattedNumber) {
            this.formattedNumber = formattedNumber;
        }

        public String getFormattedNumber() { return formattedNumber; }
        public void setFormattedNumber(String formattedNumber) { this.formattedNumber = formattedNumber; }
    }

    public static class CommitteeRequirementResponse {
        private boolean requiresCommittee;
        private String message;

        public CommitteeRequirementResponse(boolean requiresCommittee, String message) {
            this.requiresCommittee = requiresCommittee;
            this.message = message;
        }

        public boolean isRequiresCommittee() { return requiresCommittee; }
        public void setRequiresCommittee(boolean requiresCommittee) { this.requiresCommittee = requiresCommittee; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
