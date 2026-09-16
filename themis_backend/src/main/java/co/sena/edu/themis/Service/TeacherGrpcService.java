package co.sena.edu.themis.Service;

import co.sena.edu.olympo_back.TeacherServiceGrpc;
import co.sena.edu.olympo_back.Teacher.TeacherResponse;
import co.sena.edu.olympo_back.Teacher.GetTeacherByIdRequest;
import co.sena.edu.olympo_back.Teacher.GetTeacherByPersonDocumentRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

@Service
public class TeacherGrpcService {

    private static final Logger logger = LoggerFactory.getLogger(TeacherGrpcService.class);

    @GrpcClient("TeacherService")
    private TeacherServiceGrpc.TeacherServiceBlockingStub teacherServiceBlockingStub;

    @Value("${grpc.client.TeacherService.address:}")
    private String teacherServiceAddress;

    private boolean isServiceReachable(String address, int timeoutMs) {
        if (address == null || address.isBlank()) return false;
        try {
            String cleaned = address.replace("static://", "").replace("grpc://", "");
            if (cleaned.startsWith("//")) cleaned = cleaned.substring(2);
            String[] parts = cleaned.split(":");
            if (parts.length < 2) return false;
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMs);
                return true;
            }
        } catch (Exception e) {
            logger.debug("TeacherService reachable check failed for address {}: {}", address, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene un profesor por su ID desde el servicio externo.
     * @param teacherId ID del profesor.
     * @return TeacherResponse con la información del profesor.
     */
    public TeacherResponse getTeacherById(Long teacherId) {
        // Quick connectivity check
        try {
            if (!isServiceReachable(teacherServiceAddress, 300)) {
                logger.warn("TeacherService at {} seems unreachable, skipping gRPC call and returning not-found for id={}", teacherServiceAddress, teacherId);
                return TeacherResponse.newBuilder().setFound(false).build();
            }
        } catch (Exception ignored) {}

        try {
            GetTeacherByIdRequest request = GetTeacherByIdRequest.newBuilder()
                    .setId(teacherId)
                    .build();
            TeacherResponse response = teacherServiceBlockingStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getById(request);
            // Diagnostic logging
            try {
                logger.debug("[GRPC][Teacher] getTeacherById id={} -> found={}, hasCollaborator={}", teacherId,
                        response != null ? response.getFound() : false,
                        (response != null && response.getCollaborator() != null) ? response.getCollaborator().getFound() : false);
                if (response != null && response.getCollaborator() != null && response.getCollaborator().getPerson() != null && response.getCollaborator().getPerson().getFound()) {
                    try {
                        logger.debug("[GRPC][Teacher] person email={}", response.getCollaborator().getPerson().getEmail());
                    } catch (Exception ex) {
                        logger.debug("[GRPC][Teacher] no se pudo extraer email del person en response: {}", ex.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.debug("[GRPC][Teacher] Error al loguear respuesta getTeacherById id={}: {}", teacherId, e.getMessage());
            }
            return response;
        } catch (Exception e) {
            logger.warn("Error al obtener profesor por ID: {} - {}", teacherId, e.getMessage());
            return TeacherResponse.newBuilder().setFound(false).build();
        }
    }

    /**
     * Obtiene un profesor por el documento de la persona asociada.
     * @param personDocument Documento de la persona.
     * @return TeacherResponse con la información del profesor.
     */
    public TeacherResponse getTeacherByPersonDocument(Long personDocument) {
        // Quick connectivity check
        try {
            if (!isServiceReachable(teacherServiceAddress, 300)) {
                logger.warn("TeacherService at {} seems unreachable, skipping gRPC call and returning not-found for document={}", teacherServiceAddress, personDocument);
                return TeacherResponse.newBuilder().setFound(false).build();
            }
        } catch (Exception ignored) {}

        try {
            GetTeacherByPersonDocumentRequest request = GetTeacherByPersonDocumentRequest.newBuilder()
                    .setDocument(personDocument)
                    .build();
            TeacherResponse response = teacherServiceBlockingStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getTeacherByPersonDocument(request);
            // Diagnostic logging
            try {
                logger.debug("[GRPC][Teacher] getTeacherByPersonDocument document={} -> found={}", personDocument,
                        response != null ? response.getFound() : false);
            } catch (Exception e) {
                logger.debug("[GRPC][Teacher] Error logueando getTeacherByPersonDocument: {}", e.getMessage());
            }
            return response;
        } catch (Exception e) {
            logger.warn("Error al obtener profesor por documento: {} - {}", personDocument, e.getMessage());
            return TeacherResponse.newBuilder().setFound(false).build();
        }
    }

    /**
     * Verifica si un profesor existe por su ID.
     * @param teacherId ID del profesor.
     * @return true si existe, false en caso contrario.
     */
    public boolean teacherExistsById(Long teacherId) {
        TeacherResponse response = getTeacherById(teacherId);
        return response.getFound();
    }

    /**
     * Verifica si un profesor existe por el documento de la persona asociada.
     * @param personDocument Documento de la persona.
     * @return true si existe, false en caso contrario.
     */
    public boolean teacherExistsByPersonDocument(Long personDocument) {
        TeacherResponse response = getTeacherByPersonDocument(personDocument);
        return response.getFound();
    }
}

