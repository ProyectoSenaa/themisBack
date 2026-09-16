package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.MinuteBusiness;
import co.sena.edu.themis.Entity.Minute;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@DgsComponent
public class MinuteResolver {

    private static final Logger logger = LoggerFactory.getLogger(MinuteResolver.class);
    private final MinuteBusiness minuteBusiness;

    public MinuteResolver(MinuteBusiness minuteBusiness) {
        this.minuteBusiness = minuteBusiness;
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR','ADMINISTRADOR','ROLE_COORDINADOR','COORDINADOR')")
    @DgsQuery
    public Map<String, Object> allMinutes(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            // Default paging values if null
            int p = page != null ? page : 0;
            int s = size != null ? size : 10;
            Page<Map<String, Object>> minutePage = minuteBusiness.allMinutes(p, s);
            return ResponseHttpApi.responseHttpFindAll(
                    minutePage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Minutes retrieved successfully",
                    minutePage.getTotalPages(),
                    p,
                    (int) minutePage.getTotalElements()
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public String generateMinuteDocx(@InputArgument("committeeEventId") Long committeeEventId) {
        logger.info("[RESOLVER] Iniciando generación de acta DOCX para evento: {}", committeeEventId);
        System.out.println("[RESOLVER] Generando acta DOCX para evento: " + committeeEventId);
        try {
            String result = minuteBusiness.generateMinuteDocxBase64(committeeEventId);
            logger.info("[RESOLVER] Acta DOCX generada exitosamente para evento: {}", committeeEventId);
            System.out.println("[RESOLVER] Acta DOCX generada exitosamente para evento: " + committeeEventId);
            return result;
        } catch (Exception e) {
            logger.error("[RESOLVER] Error generando acta DOCX para evento: {}: {}", committeeEventId, e.getMessage(), e);
            System.out.println("[RESOLVER] Error generando acta DOCX para evento: " + committeeEventId + ": " + e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public String generateMinuteDocxUrl(@InputArgument("committeeEventId") Long committeeEventId) {
        logger.info("[RESOLVER] Iniciando generación de URL de acta DOCX para evento: {}", committeeEventId);
        System.out.println("[RESOLVER] Generando URL de acta DOCX para evento: " + committeeEventId);
        try {
            String result = minuteBusiness.generateMinuteDocxUrl(committeeEventId);
            logger.info("[RESOLVER] URL de acta DOCX generada exitosamente para evento: {}", committeeEventId);
            System.out.println("[RESOLVER] URL de acta DOCX generada exitosamente para evento: " + committeeEventId);
            return result;
        } catch (Exception e) {
            logger.error("[RESOLVER] Error generando URL de acta DOCX para evento: {}: {}", committeeEventId, e.getMessage(), e);
            System.out.println("[RESOLVER] Error generando URL de acta DOCX para evento: " + committeeEventId + ": " + e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public String minuteFileBase64(@InputArgument("filename") String filename) {
        logger.info("[RESOLVER] Iniciando descarga de archivo de acta: {}", filename);
        System.out.println("[RESOLVER] Descargando archivo de acta: " + filename);
        try {
            String result = minuteBusiness.getMinuteFileBase64(filename);
            logger.info("[RESOLVER] Archivo de acta descargado exitosamente: {}", filename);
            System.out.println("[RESOLVER] Archivo de acta descargado exitosamente: " + filename);
            return result;
        } catch (Exception e) {
            logger.error("[RESOLVER] Error descargando archivo de acta: {}: {}", filename, e.getMessage(), e);
            System.out.println("[RESOLVER] Error descargando archivo de acta: " + filename + ": " + e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Minute minuteByCommitteeEventId(@InputArgument("committeeEventId") Long committeeEventId) {
        logger.info("[RESOLVER] Buscando acta para evento de comité: {}", committeeEventId);
        System.out.println("[RESOLVER] Buscando acta para evento de comité: " + committeeEventId);
        try {
            Minute result = minuteBusiness.getMinuteByCommitteeEventId(committeeEventId);
            if (result != null) {
                logger.info("[RESOLVER] Acta encontrada para evento: {}. ID del acta: {}", committeeEventId, result.getId());
                System.out.println("[RESOLVER] Acta encontrada para evento: " + committeeEventId + ". ID del acta: " + result.getId());
            } else {
                logger.info("[RESOLVER] No se encontró acta para evento: {}", committeeEventId);
                System.out.println("[RESOLVER] No se encontró acta para evento: " + committeeEventId);
                // Intentar generar el DOCX y persistir el Minute de forma on-demand
                try {
                    logger.info("[RESOLVER] Intentando generar acta automáticamente para evento: {}", committeeEventId);
                    System.out.println("[RESOLVER] Intentando generar acta automáticamente para evento: " + committeeEventId);
                    // generateMinuteDocxUrl guarda o actualiza el Minute en BD cuando puede
                    minuteBusiness.generateMinuteDocxUrl(committeeEventId);
                    // Reconsultar
                    result = minuteBusiness.getMinuteByCommitteeEventId(committeeEventId);
                    if (result != null) {
                        logger.info("[RESOLVER] Acta generada y encontrada para evento: {}. ID del acta: {}", committeeEventId, result.getId());
                        System.out.println("[RESOLVER] Acta generada y encontrada para evento: " + committeeEventId + ". ID del acta: " + result.getId());
                    } else {
                        logger.warn("[RESOLVER] No fue posible generar acta para evento: {}", committeeEventId);
                        System.out.println("[RESOLVER] No fue posible generar acta para evento: " + committeeEventId);
                        // Si no se pudo generar, lanzar excepción para que el cliente reciba un error en vez de null
                        throw new IllegalStateException("Acta no encontrada y no pudo generarse para evento: " + committeeEventId);
                    }
                } catch (Exception ex) {
                    logger.warn("[RESOLVER] Error intentando generar acta on-demand para evento {}: {}", committeeEventId, ex.getMessage());
                    System.out.println("[RESOLVER] Error intentando generar acta on-demand para evento " + committeeEventId + ": " + ex.getMessage());
                    throw new RuntimeException("No fue posible generar la acta on-demand para evento: " + committeeEventId + ". Detalle: " + ex.getMessage(), ex);
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("[RESOLVER] Error buscando acta para evento: {}: {}", committeeEventId, e.getMessage(), e);
            System.out.println("[RESOLVER] Error buscando acta para evento: " + committeeEventId + ": " + e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Minute saveFinalMinute(@InputArgument("committeeEventId") Long committeeEventId,
                                  @InputArgument("fileContent") String fileContent) {
        logger.info("[RESOLVER] Iniciando guardado de acta final firmada para evento: {}", committeeEventId);
        System.out.println("[RESOLVER] Guardando acta final firmada para evento: " + committeeEventId);
        try {
            Minute result = minuteBusiness.saveFinalMinute(committeeEventId, fileContent);
            logger.info("[RESOLVER] Acta final guardada exitosamente para evento: {}. ID del acta: {}",
                       committeeEventId, result != null ? result.getId() : null);
            System.out.println("[RESOLVER] Acta final guardada exitosamente para evento: " + committeeEventId +
                             ". ID del acta: " + (result != null ? result.getId() : null));
            return result;
        } catch (Exception e) {
            logger.error("[RESOLVER] Error guardando acta final para evento: {}: {}", committeeEventId, e.getMessage(), e);
            System.out.println("[RESOLVER] Error guardando acta final para evento: " + committeeEventId + ": " + e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public String minuteFileBase64ByEventId(@InputArgument("committeeEventId") Long committeeEventId) {
        logger.info("[RESOLVER] Descarga Base64 de acta por evento: {}", committeeEventId);
        System.out.println("[RESOLVER] Descargando Base64 de acta por evento: " + committeeEventId);
        try {
            Minute minute = minuteBusiness.getMinuteByCommitteeEventId(committeeEventId);
            if (minute != null && minute.getFileContent() != null && !minute.getFileContent().isBlank()) {
                // Diagnostic logging: show length and prefix of fileContent safely
                String fc = minute.getFileContent();
                int len = fc.length();
                String prefix = fc.length() > 50 ? fc.substring(0, 50) : fc;
                boolean hasDataPrefix = fc.trim().startsWith("data:");
                logger.info("[RESOLVER][DEBUG] Acta encontrada en BD para evento: {}. minuteId={}, length={}, hasDataPrefix={}", committeeEventId, minute.getId(), len, hasDataPrefix);
                System.out.println("[RESOLVER][DEBUG] Acta encontrada en BD para evento: " + committeeEventId + ". minuteId=" + minute.getId() + ", length=" + len + ", hasDataPrefix=" + hasDataPrefix);
                logger.debug("[RESOLVER][DEBUG] Prefijo: {}", prefix);
                System.out.println("[RESOLVER][DEBUG] Prefijo: " + prefix);

                logger.info("[RESOLVER] Acta encontrada en BD para evento: {}. minuteId={}", committeeEventId, minute.getId());
                return minute.getFileContent();
            }

            // Intentar generar on-demand y persistir
            try {
                logger.info("[RESOLVER] Acta no encontrada; intentando generar on-demand para evento: {}", committeeEventId);
                minuteBusiness.generateMinuteDocxUrl(committeeEventId);
                Minute after = minuteBusiness.getMinuteByCommitteeEventId(committeeEventId);
                if (after != null && after.getFileContent() != null && !after.getFileContent().isBlank()) {
                    // Diagnostic logging after generation
                    String fc = after.getFileContent();
                    int len = fc.length();
                    boolean hasDataPrefix = fc.trim().startsWith("data:");
                    logger.info("[RESOLVER][DEBUG] Acta generada y encontrada para evento: {}. minuteId={}, length={}, hasDataPrefix={}", committeeEventId, after.getId(), len, hasDataPrefix);
                    System.out.println("[RESOLVER][DEBUG] Acta generada y encontrada para evento: " + committeeEventId + ". minuteId=" + after.getId() + ", length=" + len + ", hasDataPrefix=" + hasDataPrefix);

                    logger.info("[RESOLVER] Acta generada y encontrada para evento: {}. minuteId={}", committeeEventId, after.getId());
                    return after.getFileContent();
                }
                throw new IllegalStateException("Acta no encontrada y no pudo generarse para evento: " + committeeEventId);
            } catch (Exception ex) {
                logger.error("[RESOLVER] Error generando acta on-demand para evento {}: {}", committeeEventId, ex.getMessage(), ex);
                throw new RuntimeException("No fue posible generar el acta on-demand para evento: " + committeeEventId + ". Detalle: " + ex.getMessage(), ex);
            }
        } catch (Exception e) {
            logger.error("[RESOLVER] Error descargando acta por evento {}: {}", committeeEventId, e.getMessage(), e);
            System.out.println("[RESOLVER] Error descargando acta por evento " + committeeEventId + ": " + e.getMessage());
            throw e;
        }
    }
}
