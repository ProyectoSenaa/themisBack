package co.sena.edu.themis.Service;

import co.sena.edu.olympo_back.StudentServiceGrpc;
import co.sena.edu.olympo_back.Student.StudentResponse;
import co.sena.edu.olympo_back.Student.GetStudentByIdRequest;
import co.sena.edu.olympo_back.Student.GetStudentByPersonDocumentRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

@Service
public class StudentGrpcService {

    private static final Logger logger = LoggerFactory.getLogger(StudentGrpcService.class);

    @GrpcClient("StudentService")
    private StudentServiceGrpc.StudentServiceBlockingStub studentServiceBlockingStub;

    // Configurable deadline (milliseconds) for student service calls (default 3000ms to preserve current behavior)
    @Value("${themis.grpc.student.deadline-ms:3000}")
    private long studentDeadlineMs;

    // One-time longer retry deadline (milliseconds) to attempt before giving up
    private static final long RETRY_DEADLINE_MS = 10000L;

    @Value("${grpc.client.StudentService.address:}")
    private String studentServiceAddress;

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
            logger.debug("StudentService reachable check failed for address {}: {}", address, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene un estudiante por su ID desde el servicio externo.
     * @param studentId ID del estudiante.
     * @return StudentResponse con la información del estudiante.
     */
    public StudentResponse getStudentById(Long studentId) {
        // Quick connectivity check
        try {
            if (!isServiceReachable(studentServiceAddress, 300)) {
                logger.warn("StudentService at {} seems unreachable, skipping gRPC call and returning not-found for id={}", studentServiceAddress, studentId);
                return StudentResponse.newBuilder().setFound(false).build();
            }
        } catch (Exception ignored) {}

        boolean retried = false;
        long deadlineToUse = studentDeadlineMs;

        while (true) {
            try {
                GetStudentByIdRequest request = GetStudentByIdRequest.newBuilder()
                        .setId(studentId)
                        .build();

                StudentResponse response = studentServiceBlockingStub
                        .withDeadlineAfter(deadlineToUse, TimeUnit.MILLISECONDS)
                        .getById(request);

                // Diagnostic logging
                try {
                    logger.debug("[GRPC][Student] getStudentById id={} -> found={}, hasPerson={}", studentId,
                            response != null ? response.getFound() : false,
                            (response != null && response.getPerson() != null) ? response.getPerson().getFound() : false);
                    if (response != null && response.getPerson() != null && response.getPerson().getFound()) {
                        try {
                            logger.debug("[GRPC][Student] person email={}", response.getPerson().getEmail());
                        } catch (Exception ex) {
                            logger.debug("[GRPC][Student] no se pudo extraer email del person en response: {}", ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.debug("[GRPC][Student] Error al loguear respuesta getStudentById id={}: {}", studentId, e.getMessage());
                }
                return response;
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                logger.warn("Error al obtener estudiante por ID {}: {}", studentId, message);
                logger.debug("Exception stack:", e);

                if (!retried) {
                    retried = true;
                    deadlineToUse = RETRY_DEADLINE_MS;
                    logger.info("Retrying getStudentById for id={} with longer deadline {}ms", studentId, deadlineToUse);
                    continue;
                }

                return StudentResponse.newBuilder().setFound(false).build();
            }
        }
    }

    /**
     * Obtiene un estudiante por el documento de la persona asociada.
     * @param personDocument Documento de la persona (debe ser numérico).
     * @return StudentResponse con la información del estudiante.
     */
    public StudentResponse getStudentByPersonDocument(String personDocument) {
        boolean retried = false;
        long deadlineToUse = studentDeadlineMs;

        while (true) {
            try {
                GetStudentByPersonDocumentRequest request = GetStudentByPersonDocumentRequest.newBuilder()
                        .setDocument(personDocument) // Se asume que el proto espera un String
                        .build();
                StudentResponse response = studentServiceBlockingStub
                        .withDeadlineAfter(deadlineToUse, TimeUnit.MILLISECONDS)
                        .getStudentByPersonDocument(request);
                // Diagnostic logging
                try {
                    logger.debug("[GRPC][Student] getStudentByPersonDocument document={} -> found={}", personDocument,
                            response != null ? response.getFound() : false);
                } catch (Exception e) {
                    logger.debug("[GRPC][Student] Error logueando getStudentByPersonDocument: {}", e.getMessage());
                }
                return response;
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                logger.warn("Error al obtener estudiante por documento: {} - {}", personDocument, message);
                logger.debug("Exception stack:", e);

                if (!retried) {
                    retried = true;
                    deadlineToUse = RETRY_DEADLINE_MS;
                    logger.info("Retrying getStudentByPersonDocument for document={} with longer deadline {}ms", personDocument, deadlineToUse);
                    continue;
                }

                return StudentResponse.newBuilder().setFound(false).build();
            }
        }
    }

    /**
     * Verifica si un estudiante existe por su ID.
     * @param studentId ID del estudiante.
     * @return true si existe, false en caso contrario.
     */
    public boolean studentExistsById(Long studentId) {
        StudentResponse response = getStudentById(studentId);
        return response.getFound();
    }

    /**
     * Verifica si un estudiante existe por el documento de la persona asociada.
     * @param personDocument Documento de la persona.
     * @return true si existe, false en caso contrario.
     */
    public boolean studentExistsByPersonDocument(String personDocument) {
        StudentResponse response = getStudentByPersonDocument(personDocument);
        return response.getFound();
    }
}

