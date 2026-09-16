package co.sena.edu.themis.Service;

import co.sena.edu.olympo_back.proto.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class StudySheetGrpcService {

    @GrpcClient("StudySheetService")
    private StudySheetServiceGrpc.StudySheetServiceBlockingStub studySheetServiceBlockingStub;

    /**
     * Obtiene una ficha de estudio por su ID desde el servicio externo
     * @param studySheetId ID de la ficha de estudio
     * @return StudySheetResponse con la información de la ficha
     */
    public StudySheetResponse getStudySheetById(Long studySheetId) {
        try {
            StudySheetRequest request = StudySheetRequest.newBuilder()
                    .setStudySheetId(studySheetId)
                    .build();

            return studySheetServiceBlockingStub.getStudySheetById(request);
        } catch (Exception e) {
            System.err.println("Error obteniendo ficha de estudio desde Olympo para ID: " + studySheetId + " - " + e.getMessage());
            // Retornar respuesta vacía indicando que no se encontró
            return StudySheetResponse.newBuilder()
                    .setStudySheetId(studySheetId)
                    .setFound(false)
                    .build();
        }
    }

    /**
     * Verifica si una ficha de estudio existe
     * @param studySheetId ID de la ficha de estudio
     * @return true si existe, false en caso contrario
     */
    public boolean studySheetExists(Long studySheetId) {
        StudySheetResponse response = getStudySheetById(studySheetId);
        return response.getFound();
    }

    /**
     * Obtiene el número de estudiantes de una ficha
     * @param studySheetId ID de la ficha de estudio
     * @return número de estudiantes o 0 si no se encuentra
     */
    public int getNumberOfStudents(Long studySheetId) {
        StudySheetResponse response = getStudySheetById(studySheetId);
        return response.getFound() ? response.getNumberStudents() : 0;
    }

    /**
     * Verifica si una ficha está activa
     * @param studySheetId ID de la ficha de estudio
     * @return true si está activa, false en caso contrario
     */
    public boolean isStudySheetActive(Long studySheetId) {
        StudySheetResponse response = getStudySheetById(studySheetId);
        return response.getFound() && response.getState();
    }
}
