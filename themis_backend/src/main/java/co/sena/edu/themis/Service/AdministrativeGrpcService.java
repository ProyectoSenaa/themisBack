package co.sena.edu.themis.Service;

import co.sena.edu.olympo_back.AdministrativeServiceGrpc;
import co.sena.edu.olympo_back.Administrative.AdministrativeResponse;
import co.sena.edu.olympo_back.Administrative.GetAdministrativeByIdRequest;
import co.sena.edu.olympo_back.Administrative.GetAdministrativeByPersonDocumentRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

@Service
public class AdministrativeGrpcService {

    private static final Logger logger = LoggerFactory.getLogger(AdministrativeGrpcService.class);

    @GrpcClient("AdministrativeService")
    private AdministrativeServiceGrpc.AdministrativeServiceBlockingStub administrativeServiceBlockingStub;

    @Value("${grpc.client.AdministrativeService.address:}")
    private String administrativeServiceAddress;

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
            logger.debug("AdministrativeService reachable check failed for address {}: {}", address, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene un administrativo por su ID.
     * @param administrativeId ID del administrativo.
     * @return AdministrativeResponse con los datos del administrativo.
     */
    public AdministrativeResponse getAdministrativeById(Long administrativeId) {
        // Quick connectivity check
        try {
            if (!isServiceReachable(administrativeServiceAddress, 300)) {
                logger.warn("AdministrativeService at {} seems unreachable, skipping gRPC call and returning not-found for id={}", administrativeServiceAddress, administrativeId);
                return AdministrativeResponse.newBuilder().setFound(false).build();
            }
        } catch (Exception ignored) {}

        try {
            GetAdministrativeByIdRequest request = GetAdministrativeByIdRequest.newBuilder()
                    .setId(administrativeId)
                    .build();
            AdministrativeResponse response = administrativeServiceBlockingStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getById(request);
            // Diagnostic logging
            try {
                logger.debug("[GRPC][Administrative] getAdministrativeById id={} -> found={}, hasCollaborator={}", administrativeId,
                        response != null ? response.getFound() : false,
                        (response != null && response.getCollaborator() != null) ? response.getCollaborator().getFound() : false);
                if (response != null && response.getCollaborator() != null && response.getCollaborator().getPerson() != null && response.getCollaborator().getPerson().getFound()) {
                    try {
                        logger.debug("[GRPC][Administrative] person email={}", response.getCollaborator().getPerson().getEmail());
                    } catch (Exception ex) {
                        logger.debug("[GRPC][Administrative] no se pudo extraer email del person en response: {}", ex.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.debug("[GRPC][Administrative] Error al loguear respuesta getAdministrativeById id={}: {}", administrativeId, e.getMessage());
            }
            return response;
        } catch (Exception e) {
            logger.warn("Error al obtener administrativo por ID: {} - {}", administrativeId, e.getMessage());
            return AdministrativeResponse.newBuilder().setFound(false).build();
        }
    }

    /**
     * Obtiene un administrativo por el documento de la persona asociada.
     * @param personDocument Documento de la persona.
     * @return AdministrativeResponse con los datos del administrativo.
     */
    public AdministrativeResponse getAdministrativeByPersonDocument(Long personDocument) {
        // Quick connectivity check
        try {
            if (!isServiceReachable(administrativeServiceAddress, 300)) {
                logger.warn("AdministrativeService at {} seems unreachable, skipping gRPC call and returning not-found for document={}", administrativeServiceAddress, personDocument);
                return AdministrativeResponse.newBuilder().setFound(false).build();
            }
        } catch (Exception ignored) {}

        try {
            GetAdministrativeByPersonDocumentRequest request = GetAdministrativeByPersonDocumentRequest.newBuilder()
                    .setDocument(personDocument)
                    .build();
            AdministrativeResponse response = administrativeServiceBlockingStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getAdministrativeByPersonDocument(request);
            // Diagnostic logging
            try {
                logger.debug("[GRPC][Administrative] getAdministrativeByPersonDocument document={} -> found={}", personDocument,
                        response != null ? response.getFound() : false);
            } catch (Exception e) {
                logger.debug("[GRPC][Administrative] Error logueando getAdministrativeByPersonDocument: {}", e.getMessage());
            }
            return response;
        } catch (Exception e) {
            logger.warn("Error al obtener administrativo por documento: {} - {}", personDocument, e.getMessage());
            return AdministrativeResponse.newBuilder().setFound(false).build();
        }
    }

    /**
     * Verifica si un administrativo existe por su ID.
     * @param administrativeId ID del administrativo.
     * @return true si existe, false en caso contrario.
     */
    public boolean administrativeExistsById(Long administrativeId) {
        AdministrativeResponse response = getAdministrativeById(administrativeId);
        return response.getFound();
    }

    /**
     * Verifica si un administrativo existe por el documento de la persona asociada.
     * @param personDocument Documento de la persona.
     * @return true si existe, false en caso contrario.
     */
    public boolean administrativeExistsByPersonDocument(Long personDocument) {
        AdministrativeResponse response = getAdministrativeByPersonDocument(personDocument);
        return response.getFound();
    }
}

