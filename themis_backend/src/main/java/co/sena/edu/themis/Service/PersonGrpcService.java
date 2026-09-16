package co.sena.edu.themis.Service;

import co.sena.edu.olympo_back.proto.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

@Service
public class PersonGrpcService {

    private static final Logger logger = LoggerFactory.getLogger(PersonGrpcService.class);

    @GrpcClient("PersonService")
    private PersonServiceGrpc.PersonServiceBlockingStub personServiceBlockingStub;

    @Autowired
    private StudySheetGrpcService studySheetGrpcService;

    // Configurable deadline (milliseconds) for PersonService gRPC calls. Default 3000ms.
    @Value("${themis.grpc.person.deadline-ms:3000}")
    private long personDeadlineMs;

    // One-time longer retry deadline (milliseconds) to attempt before giving up
    private static final long RETRY_DEADLINE_MS = 10000L;

    // Address configured for the PersonService client (e.g. static://host:port)
    @Value("${grpc.client.PersonService.address:}")
    private String personServiceAddress;

    private boolean isServiceReachable(String address, int timeoutMs) {
        if (address == null || address.isBlank()) return false;
        try {
            // address expected like "static://host:port" or "host:port"
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
            logger.debug("PersonService reachable check failed for address {}: {}", address, e.getMessage());
            return false;
        }
    }

    public PersonResponse getPersonById(long id) {
        // Quick connectivity check: if the configured target is unreachable, avoid long gRPC waits
        try {
            if (!isServiceReachable(personServiceAddress, 300)) {
                // Pre-check failed: log but still attempt a gRPC call once. The blocking stub will
                // handle the real deadline and retries. Returning early here caused false negatives
                // when the network check was too strict.
                logger.warn("PersonService at {} seems unreachable (pre-check failed) — will still attempt gRPC call for id={}", personServiceAddress, id);
            }
        } catch (Exception ignored) {}

        boolean retried = false;
        long deadlineToUse = personDeadlineMs;

        while (true) {
            try {
                PersonIdRequest request = PersonIdRequest.newBuilder()
                        .setPersonId(id)
                        .build();

                PersonResponse response = personServiceBlockingStub
                        .withDeadlineAfter(deadlineToUse, TimeUnit.MILLISECONDS)
                        .getPersonById(request);

                // Diagnostic logging: indicar si la persona fue encontrada y email (si existe)
                try {
                    logger.debug("[GRPC][Person] getPersonById id={} -> found={}, email={}", id,
                            response != null ? response.getFound() : false,
                            (response != null && response.getFound() && response.getEmail() != null) ? response.getEmail() : "null");
                } catch (Exception e) {
                    logger.debug("[GRPC][Person] No se pudo extraer datos del PersonResponse para id={}: {}", id, e.getMessage());
                }
                return response;
            } catch (Exception e) {
                // If deadline exceeded and we haven't retried yet, try once with a longer timeout
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                logger.warn("Error obteniendo persona por ID {}: {}", id, message);
                logger.debug("Exception stack:", e);

                if (!retried) {
                    retried = true;
                    deadlineToUse = RETRY_DEADLINE_MS;
                    logger.info("Retrying getPersonById for id={} with longer deadline {}ms", id, deadlineToUse);
                    // loop and retry once
                    continue;
                }

                // After retry or for other errors, return not found response
                return PersonResponse.newBuilder().setFound(false).build();
            }
        }
    }

    public PersonResponse getPersonByDocument(long document) {
        boolean retried = false;
        long deadlineToUse = personDeadlineMs;

        while (true) {
            try {
                PersonRequest request = PersonRequest.newBuilder()
                        .setDocument(document)
                        .build();

                PersonResponse response = personServiceBlockingStub
                        .withDeadlineAfter(deadlineToUse, TimeUnit.MILLISECONDS)
                        .getPersonByDocument(request);

                try {
                    logger.debug("[GRPC][Person] getPersonByDocument document={} -> found={}, email={}", document,
                            response != null ? response.getFound() : false,
                            (response != null && response.getFound() && response.getEmail() != null) ? response.getEmail() : "null");
                } catch (Exception e) {
                    logger.debug("[GRPC][Person] No se pudo extraer datos del PersonResponse (document) {}: {}", document, e.getMessage());
                }

                return response;
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : e.toString();
                logger.warn("Error obteniendo persona por DOCUMENT {}: {}", document, message);
                logger.debug("Exception stack:", e);

                if (!retried) {
                    retried = true;
                    deadlineToUse = RETRY_DEADLINE_MS;
                    logger.info("Retrying getPersonByDocument for document={} with longer deadline {}ms", document, deadlineToUse);
                    continue;
                }

                return PersonResponse.newBuilder().setFound(false).build();
            }
        }
    }

    /**
     * Obtiene el número de ficha del estudiante basado en información de persona
     * Ahora integrado con el servicio de StudySheet
     * @param studentId ID del estudiante (puede ser personId)
     * @return String con el número de ficha o null si no se encuentra
     */
    public String getStudentFichaNumber(Long studentId) {
        try {
            // Primero intentar obtener la información desde StudySheet service
            StudySheetResponse studySheetResponse = studySheetGrpcService.getStudySheetById(studentId);
            if (studySheetResponse.getFound()) {
                return String.format("FICHA-%06d", studySheetResponse.getNumber());
            }

            // Fallback: usar la lógica anterior basada en PersonService
            PersonResponse personResponse = getPersonById(studentId);
            if (personResponse.getFound()) {
                String document = String.valueOf(personResponse.getDocument());

                if (document.length() > 6) {
                    String lastDigits = document.substring(document.length() - 2);
                    int numericValue = Integer.parseInt(lastDigits);
                    String fichaPrefix = (numericValue % 2 == 0) ? "30" : "31";
                    return fichaPrefix + String.format("%04d", studentId % 10000);
                }
            }

            return generateFallbackFicha(studentId);

        } catch (Exception e) {
            logger.warn("Error obteniendo ficha desde servicios para studentId: {} - {}", studentId, e.getMessage());
            return generateFallbackFicha(studentId);
        }
    }

    /**
     * Verifica si una ficha requiere comité basado en su número
     * Ahora integrado con StudySheetService
     * @param studentId ID del estudiante
     * @return true si requiere comité, false si es proceso directo
     */
    public boolean fichaRequiresCommittee(Long studentId) {
        try {
            // Primero verificar desde StudySheet service
            StudySheetResponse studySheetResponse = studySheetGrpcService.getStudySheetById(studentId);
            if (studySheetResponse.getFound()) {
                // Lógica basada en información real de la ficha
                return studySheetResponse.getNumberStudents() > 30 ||
                       String.valueOf(studySheetResponse.getNumber()).startsWith("31");
            }

            // Fallback: usar lógica anterior
            String fichaNumber = getStudentFichaNumber(studentId);
            if (fichaNumber != null) {
                return fichaNumber.startsWith("31") || fichaNumber.contains("31");
            }

            return true; // Por defecto requerir comité
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Simula obtener información del estudiante por personId
     * Versión temporal hasta que el Student.proto esté completamente implementado
     */
    public PersonResponse getStudentByPersonId(Long personId) {
        try {
            return getPersonById(personId);
        } catch (Exception e) {
            return PersonResponse.newBuilder()
                    .setFound(false)
                    .build();
        }
    }

    /**
     * Genera una ficha de fallback cuando no se puede obtener desde Olympo
     */
    private String generateFallbackFicha(Long studentId) {
        // Generar ficha basada en studentId
        // Los IDs pares generan fichas antiguas (30), los impares fichas nuevas (31)
        String prefix = (studentId % 2 == 0) ? "30" : "31";
        return prefix + String.format("%04d", studentId % 10000);
    }
}
