package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Entity.Committee;
import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Entity.Minute;
import co.sena.edu.themis.Entity.CommitteeEvent;
import co.sena.edu.themis.Repository.MinuteRepository;
import co.sena.edu.themis.Service.CommitteeEventService;
import co.sena.edu.themis.Service.*;
import co.sena.edu.themis.Utils.validation.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Component
public class MinuteBusiness {

    private static final Logger logger = LoggerFactory.getLogger(MinuteBusiness.class);

    private final CommitteeEventBusiness committeeEventBusiness;
    private final CommitteeService committeeService;
    private final StudentGrpcService studentGrpcService;
    private final TeacherGrpcService teacherGrpcService;
    private final AdministrativeGrpcService administrativeGrpcService;
    private final PersonGrpcService personGrpcService;
    private final NoveltyBusiness noveltyBusiness;
    private final MinuteDocxService minuteDocxService;
    private final MinuteFileService minuteFileService;
    private final ValidationUtils validationUtils;
    private final MinuteRepository minuteRepository;
    private final CommitteeEventService committeeEventService;

    public MinuteBusiness(@Lazy CommitteeEventBusiness committeeEventBusiness,
                          CommitteeService committeeService,
                          StudentGrpcService studentGrpcService,
                          TeacherGrpcService teacherGrpcService,
                          AdministrativeGrpcService administrativeGrpcService,
                          PersonGrpcService personGrpcService,
                          NoveltyBusiness noveltyBusiness,
                          MinuteDocxService minuteDocxService,
                          MinuteFileService minuteFileService,
                          ValidationUtils validationUtils,
                          MinuteRepository minuteRepository,
                          CommitteeEventService committeeEventService) {
        this.committeeEventBusiness = committeeEventBusiness;
        this.committeeService = committeeService;
        this.studentGrpcService = studentGrpcService;
        this.teacherGrpcService = teacherGrpcService;
        this.administrativeGrpcService = administrativeGrpcService;
        this.personGrpcService = personGrpcService;
        this.noveltyBusiness = noveltyBusiness;
        this.minuteDocxService = minuteDocxService;
        this.minuteFileService = minuteFileService;
        this.validationUtils = validationUtils;
        this.minuteRepository = minuteRepository;
        this.committeeEventService = committeeEventService;
    }

    public Page<Map<String, Object>> allMinutes(int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Minute> minutePage = minuteRepository.findAll(pageRequest);
            Page<Map<String, Object>> mapped = minutePage.map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("fileContent", m.getFileContent());
                map.put("committeeEventId", m.getCommitteeEvent() != null ? m.getCommitteeEvent().getId() : null);
                return map;
            });
            return mapped;
        }, "Error retrieving minutes");
    }

    public String generateMinuteDocxBase64(Long committeeEventId) {
        logger.info("[ACTA] Iniciando generación de acta DOCX en Base64 para evento: {}", committeeEventId);
        System.out.println("[ACTA] Generando acta DOCX en Base64 para evento: " + committeeEventId);
        String result = validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(committeeEventId, "committeeEventId requerido");

            CommitteeEventDto event = committeeEventBusiness.eventById(committeeEventId);
            validationUtils.validateExists(event, "Evento no encontrado");

            // Validar que el evento tenga un comité asignado
            if (event.getCommittee() == null || event.getCommittee().getId() == null) {
                throw new IllegalStateException("No se puede generar el acta: el evento no tiene un comité asignado. Por favor, asigne un comité al evento antes de generar el acta.");
            }

            // Obtener participantes del comité
            var participants = getCommitteeParticipants(event.getCommittee().getId());

            // Obtener novedades de estudiantes
            List<Novelty> novelties = noveltyBusiness.findAllByStudentIds(participants.studentIds);

            // Generar documento DOCX
            try {
                byte[] bytes = minuteDocxService.generateMinuteDocx(event, participants.students, participants.teachers, participants.administratives, novelties);
                return Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                throw new RuntimeException("Error generando documento DOCX: " + e.getMessage(), e);
            }

        }, "Error generando acta DOCX");
        logger.info("[ACTA] Finalizada generación de acta DOCX en Base64 para evento: {}", committeeEventId);
        System.out.println("[ACTA] Acta DOCX en Base64 generada para evento: " + committeeEventId);
        return result;
    }

    public String generateMinuteDocxUrl(Long committeeEventId) {
        logger.info("[ACTA] Iniciando generación de acta DOCX y URL para evento: {}", committeeEventId);
        System.out.println("[ACTA] Generando acta DOCX y URL para evento: " + committeeEventId);
        String url = validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(committeeEventId, "committeeEventId requerido");

            CommitteeEventDto event = committeeEventBusiness.eventById(committeeEventId);
            validationUtils.validateExists(event, "Evento no encontrado");

            // Validar que el evento tenga un comité asignado
            if (event.getCommittee() == null || event.getCommittee().getId() == null) {
                throw new IllegalStateException("No se puede generar el acta: el evento no tiene un comité asignado. Por favor, asigne un comité al evento antes de generar el acta.");
            }

            // Obtener participantes del comité
            var participants = getCommitteeParticipants(event.getCommittee().getId());

            // Obtener novedades de estudiantes
            List<Novelty> novelties = noveltyBusiness.findAllByStudentIds(participants.studentIds);

            try {
                // Generar documento DOCX
                byte[] bytes = minuteDocxService.generateMinuteDocx(event, participants.students, participants.teachers, participants.administratives, novelties);

                // Guardar archivo y generar URL
                String baseName = "acta-comite-" + committeeEventId + (event.getDate() != null ? ("-" + event.getDate()) : "");
                String filename = minuteFileService.saveDocx(bytes, baseName);

                // Guardar o actualizar un registro Minute asociado al evento con el contenido DOCX en Base64
                try {
                    String base64Docx = Base64.getEncoder().encodeToString(bytes);
                    var existingMinute = minuteRepository.findByCommitteeEventId(committeeEventId).orElse(null);
                    if (existingMinute == null) {
                        Minute newMinute = new Minute();
                        newMinute.setCommitteeEvent(committeeEventService.getById(committeeEventId));
                        newMinute.setFileContent(base64Docx);
                        minuteRepository.save(newMinute);
                        logger.info("[ACTA] Minute creado para evento {} tras generar DOCX. minuteId={}", committeeEventId, newMinute.getId());
                        System.out.println("[ACTA] Minute creado para evento " + committeeEventId + ". minuteId=" + newMinute.getId());
                    } else {
                        existingMinute.setFileContent(base64Docx);
                        minuteRepository.save(existingMinute);
                        logger.info("[ACTA] Minute existente actualizado para evento {} tras generar DOCX. minuteId={}", committeeEventId, existingMinute.getId());
                        System.out.println("[ACTA] Minute actualizado para evento " + committeeEventId + ". minuteId=" + existingMinute.getId());
                    }
                } catch (Exception e) {
                    logger.error("[ACTA] No se pudo crear/actualizar Minute en BD tras generar DOCX: {}", e.getMessage(), e);
                    System.out.println("[ACTA] No se pudo crear/actualizar Minute en BD tras generar DOCX: " + e.getMessage());
                    // Rethrow so callers know that persisting the minute failed and avoid returning a misleading URL
                    throw new RuntimeException("No se pudo crear/actualizar Minute en BD tras generar DOCX: " + e.getMessage(), e);
                }

                return ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/minutes/")
                        .path(filename)
                        .toUriString();
            } catch (Exception e) {
                throw new RuntimeException("Error generando documento DOCX o guardando archivo: " + e.getMessage(), e);
            }

        }, "Error generando URL de acta DOCX");
        logger.info("[ACTA] Finalizada generación de acta DOCX y URL para evento: {}. URL: {}", committeeEventId, url);
        System.out.println("[ACTA] Acta DOCX y URL generada para evento: " + committeeEventId + ". URL: " + url);
        return url;
    }

    public String getMinuteFileBase64(String filename) {
        logger.info("[ACTA] Iniciando descarga de archivo de acta: {}", filename);
        System.out.println("[ACTA] Descargando archivo de acta: " + filename);
        String base64 = validationUtils.tryExecute(() -> {
            // Validar filename manualmente ya que ValidationUtils no tiene validateNotBlank
            if (filename == null || filename.isBlank()) {
                throw new IllegalArgumentException("filename requerido");
            }

            String input = filename.trim();

            // Diagnostic logging: show trimmed input and length
            logger.info("[ACTA][DEBUG] Input trimmed para descarga: '{}' (length={})", input, input.length());
            System.out.println("[ACTA][DEBUG] Input trimmed para descarga: '" + input + "' (length=" + input.length() + ")");

            // Caso 1: si el input parece una data URL (data:...;base64,xxxx) devolver la parte base64
            if (input.startsWith("data:") && input.contains(",")) {
                logger.info("[ACTA][DEBUG] Input parece una data URL");
                System.out.println("[ACTA][DEBUG] Input parece una data URL");
                String[] parts = input.split(",", 2);
                String possibleBase64 = parts[1];
                // validar que sea base64 decodificable
                try {
                    Base64.getDecoder().decode(possibleBase64);
                    logger.info("[ACTA][DEBUG] Data URL decodeable, devolviendo base64 (length={})", possibleBase64.length());
                    System.out.println("[ACTA][DEBUG] Data URL decodeable, devolviendo base64 (length=" + possibleBase64.length() + ")");
                    return possibleBase64;
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Contenido base64 inválido en data URL");
                }
            }

            // Caso 2: si el input parece ya un Base64 (longitud razonable y sólo chars base64)
            String base64Candidate = input.replaceAll("\\s+", "");
            if (base64Candidate.length() > 100) {
                logger.info("[ACTA][DEBUG] Input candidate considerado como posible base64 (length={})", base64Candidate.length());
                System.out.println("[ACTA][DEBUG] Input candidate considerado como posible base64 (length=" + base64Candidate.length() + ")");
                try {
                    Base64.getDecoder().decode(base64Candidate);
                    // Parece ser contenido base64 ya; devolver tal cual
                    logger.info("[ACTA][DEBUG] Input es base64 decodificable, devolviendo base64");
                    System.out.println("[ACTA][DEBUG] Input es base64 decodificable, devolviendo base64");
                    return base64Candidate;
                } catch (IllegalArgumentException ignored) {
                    // no es base64 válido, seguir procesando como filename
                    logger.info("[ACTA][DEBUG] Input NO es base64 válido pese a su longitud; se tratará como filename");
                    System.out.println("[ACTA][DEBUG] Input NO es base64 válido pese a su longitud; se tratará como filename");
                }
            }

            // Caso 3: si viene una URL o path, extraer el nombre del archivo
            if (input.contains("/")) {
                int idx = input.lastIndexOf('/');
                if (idx >= 0 && idx < input.length() - 1) {
                    input = input.substring(idx + 1);
                    logger.info("[ACTA][DEBUG] Se extrajo nombre de archivo de path/url: {}", input);
                    System.out.println("[ACTA][DEBUG] Se extrajo nombre de archivo de path/url: " + input);
                }
            }

            try {
                logger.info("[ACTA][DEBUG] Intentando cargar recurso desde storage con filename: {}", input);
                System.out.println("[ACTA][DEBUG] Intentando cargar recurso desde storage con filename: " + input);
                Resource resource = minuteFileService.loadAsResource(input);
                byte[] bytes = resource.getInputStream().readAllBytes();
                logger.info("[ACTA][DEBUG] Recurso cargado exitosamente (bytes={})", bytes.length);
                System.out.println("[ACTA][DEBUG] Recurso cargado exitosamente (bytes=" + bytes.length + ")");
                return Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                logger.error("[ACTA] Error leyendo archivo de acta: {}", e.getMessage(), e);
                System.out.println("[ACTA] Error leyendo archivo de acta: " + e.getMessage());
                throw new RuntimeException("Error leyendo archivo de acta: " + e.getMessage(), e);
            }

        }, "Error leyendo archivo de acta");
        logger.info("[ACTA] Archivo de acta descargado en Base64: {}", filename);
        System.out.println("[ACTA] Archivo de acta descargado en Base64: " + filename);
        return base64;
    }

    public Minute saveFinalMinute(Long committeeEventId, String fileContent) {
        logger.info("[ACTA] Iniciando guardado de acta final para evento: {}", committeeEventId);
        System.out.println("[ACTA] Guardando acta final para evento: " + committeeEventId);
        Minute minute = validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(committeeEventId, "committeeEventId requerido");
            validationUtils.validateNotNull(fileContent, "fileContent requerido");

            // Validar que el archivo sea un PDF
            if (!isPdfFile(fileContent)) {
                throw new IllegalArgumentException("El archivo debe ser un PDF. Solo se permiten archivos en formato PDF para el acta final.");
            }

            CommitteeEvent committeeEvent = committeeEventService.getById(committeeEventId);
            validationUtils.validateExists(committeeEvent, "Evento no encontrado");

            Minute minute1 = new Minute();
            minute1.setCommitteeEvent(committeeEvent);
            minute1.setFileContent(fileContent);
            // Si ya existe un Minute para este evento, actualizar su contenido en lugar de crear uno nuevo
            var existing = minuteRepository.findByCommitteeEventId(committeeEventId).orElse(null);
            if (existing != null) {
                existing.setFileContent(fileContent);
                return minuteRepository.save(existing);
            } else {
                return minuteRepository.save(minute1);
            }
        }, "Error guardando el acta final");
        logger.info("[ACTA] Acta final guardada para evento: {}. MinuteId: {}", committeeEventId, minute != null ? minute.getId() : null);
        System.out.println("[ACTA] Acta final guardada para evento: " + committeeEventId + ". MinuteId: " + (minute != null ? minute.getId() : null));
        return minute;
    }

    public Minute getMinuteByCommitteeEventId(Long committeeEventId) {
        logger.info("[ACTA] Buscando acta para evento: {}", committeeEventId);
        System.out.println("[ACTA] Buscando acta para evento: " + committeeEventId);
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(committeeEventId, "committeeEventId requerido");
            return minuteRepository.findByCommitteeEventId(committeeEventId).orElse(null);
        }, "Error buscando acta por committeeEventId");
    }

    /**
     * Valida que el contenido Base64 corresponda a un archivo PDF.
     * Un archivo PDF comienza con los bytes: %PDF (0x25 0x50 0x44 0x46)
     */
    private boolean isPdfFile(String base64Content) {
        try {
            // Remover el prefijo data URL si existe (data:application/pdf;base64,)
            String base64Data = base64Content;
            if (base64Content.contains(",")) {
                base64Data = base64Content.split(",")[1];
            }

            // Decodificar el Base64
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

            // Verificar que tenga al menos 4 bytes
            if (decodedBytes.length < 4) {
                return false;
            }

            // Verificar que comience con %PDF (25 50 44 46 en hexadecimal)
            return decodedBytes[0] == 0x25 &&
                   decodedBytes[1] == 0x50 &&
                   decodedBytes[2] == 0x44 &&
                   decodedBytes[3] == 0x46;
        } catch (Exception e) {
            logger.error("[ACTA] Error validando formato PDF: {}", e.getMessage());
            return false;
        }
    }

    private CommitteeParticipants getCommitteeParticipants(Long committeeId) {
        logger.info("[ACTA] Iniciando obtención de participantes del comité: {}", committeeId);
        System.out.println("[ACTA] Obteniendo participantes del comité: " + committeeId);

        Committee committee = committeeService.getById(committeeId);
        if (committee == null) {
            throw new IllegalStateException("Comité no encontrado");
        }

        List<Long> studentIds = committee.getStudentsIds() != null ? committee.getStudentsIds() : new ArrayList<>();
        List<Long> teacherIds = committee.getTeachersIds() != null ? committee.getTeachersIds() : new ArrayList<>();
        List<Long> administrativeIds = committee.getAdministrativesIds() != null ? committee.getAdministrativesIds() : new ArrayList<>();

        logger.info("[ACTA] DETALLE COMITÉ - ID: {}, CoordinationId: {}", committeeId, committee.getCoordinationId());
        logger.info("[ACTA] IDs de estudiantes del comité {}: {} (total: {})", committeeId, studentIds, studentIds.size());
        logger.info("[ACTA] IDs de profesores del comité {}: {} (total: {})", committeeId, teacherIds, teacherIds.size());
        logger.info("[ACTA] IDs de administrativos del comité {}: {} (total: {})", committeeId, administrativeIds, administrativeIds.size());

        System.out.println("[ACTA] ========== DIAGNÓSTICO DETALLADO ==========");
        System.out.println("[ACTA] COMITÉ - ID: " + committeeId + ", CoordinationId: " + committee.getCoordinationId());
        System.out.println("[ACTA] IDs de estudiantes: " + studentIds + " (total: " + studentIds.size() + ")");
        System.out.println("[ACTA] IDs de profesores: " + teacherIds + " (total: " + teacherIds.size() + ")");
        System.out.println("[ACTA] IDs de administrativos: " + administrativeIds + " (total: " + administrativeIds.size() + ")");

        // Si no hay IDs, el comité no tiene participantes asignados
        if (studentIds.isEmpty() && teacherIds.isEmpty() && administrativeIds.isEmpty()) {
            logger.warn("[ACTA] ⚠️  EL COMITÉ {} NO TIENE PARTICIPANTES ASIGNADOS!", committeeId);
            System.out.println("[ACTA] ⚠️  EL COMITÉ " + committeeId + " NO TIENE PARTICIPANTES ASIGNADOS!");
            System.out.println("[ACTA] Esto significa que el comité existe pero está vacío.");
            System.out.println("[ACTA] Para resolver esto, asigna estudiantes, profesores o administrativos al comité.");
        }

        // Obtener estudiantes via gRPC
        List<co.sena.edu.olympo_back.Student.StudentResponse> students = new ArrayList<>();
        System.out.println("[ACTA] ===== OBTENIENDO ESTUDIANTES =====");
        for (Long id : studentIds) {
            logger.info("[ACTA] Intentando obtener estudiante con ID: {}", id);
            System.out.println("[ACTA] 🔍 Buscando estudiante con ID: " + id);
            try {
                // Verificar que el servicio gRPC esté disponible
                if (studentGrpcService == null) {
                    logger.error("[ACTA] ❌ StudentGrpcService es NULL!");
                    System.out.println("[ACTA] ❌ StudentGrpcService es NULL!");
                    continue;
                }

                var studentResponse = studentGrpcService.getStudentById(id);
                logger.info("[ACTA] Respuesta gRPC para estudiante {}: found={}, hasPersonId={}",
                    id, studentResponse != null ? studentResponse.getFound() : "null",
                    studentResponse != null && studentResponse.hasPerson() ? studentResponse.getPerson().getPersonId() : "no person");

                System.out.println("[ACTA] 📡 Respuesta gRPC estudiante " + id + ": " +
                    (studentResponse != null ? ("found=" + studentResponse.getFound() +
                    ", hasPersonId=" + (studentResponse.hasPerson() ? studentResponse.getPerson().getPersonId() : "no person")) : "NULL"));

                if (studentResponse != null && studentResponse.getFound()) {
                    students.add(studentResponse);
                    logger.info("[ACTA] ✅ Estudiante {} obtenido exitosamente: {}", id,
                        studentResponse.hasPerson() ? studentResponse.getPerson().getName() + " " + studentResponse.getPerson().getLastname() : "sin datos de persona");
                    System.out.println("[ACTA] ✅ Estudiante " + id + " obtenido exitosamente: " +
                        (studentResponse.hasPerson() ? studentResponse.getPerson().getName() + " " + studentResponse.getPerson().getLastname() : "sin datos de persona"));
                } else {
                    logger.warn("[ACTA] ⚠️  Estudiante {} no encontrado en gRPC, intentando como persona directa", id);
                    System.out.println("[ACTA] ⚠️  Estudiante " + id + " no encontrado en gRPC, intentando como persona directa");

                    // Intentar obtener como persona si no es estudiante
                    if (personGrpcService != null) {
                        var personResponse = personGrpcService.getPersonById(id);
                        logger.info("[ACTA] Respuesta gRPC para persona {}: found={}, name={}",
                            id, personResponse != null ? personResponse.getFound() : "null",
                            personResponse != null && personResponse.getFound() ? personResponse.getName() : "no name");

                        System.out.println("[ACTA] 📡 Respuesta gRPC persona " + id + ": " +
                            (personResponse != null ? ("found=" + personResponse.getFound() +
                            ", name=" + (personResponse.getFound() ? personResponse.getName() : "no name")) : "NULL"));

                        if (personResponse != null && personResponse.getFound()) {
                            students.add(co.sena.edu.olympo_back.Student.StudentResponse.newBuilder()
                                    .setId(id)
                                    .setFound(true)
                                    .setPerson(personResponse)
                                    .build());
                            logger.info("[ACTA] ✅ Persona {} obtenida exitosamente como estudiante: {}",
                                id, personResponse.getName() + " " + personResponse.getLastname());
                            System.out.println("[ACTA] ✅ Persona " + id + " obtenida exitosamente como estudiante: " +
                                personResponse.getName() + " " + personResponse.getLastname());
                        } else {
                            logger.warn("[ACTA] ❌ Persona {} tampoco encontrada en gRPC", id);
                            System.out.println("[ACTA] ❌ Persona " + id + " tampoco encontrada en gRPC");
                        }
                    } else {
                        logger.error("[ACTA] ❌ PersonGrpcService es NULL!");
                        System.out.println("[ACTA] ❌ PersonGrpcService es NULL!");
                    }
                }
            } catch (Exception ex) {
                logger.error("[ACTA] ❌ ERROR CRÍTICO obteniendo estudiante {}: {}", id, ex.getMessage(), ex);
                System.out.println("[ACTA] ❌ ERROR CRÍTICO obteniendo estudiante " + id + ": " + ex.getMessage());
                // Reemplazado ex.printStackTrace() por logging detallado
                logger.debug("[ACTA] Stack trace del error en estudiante {}: ", id, ex);
            }
        }

        // Obtener profesores via gRPC
        List<co.sena.edu.olympo_back.Teacher.TeacherResponse> teachers = new ArrayList<>();
        System.out.println("[ACTA] ===== OBTENIENDO PROFESORES =====");
        for (Long id : teacherIds) {
            logger.info("[ACTA] Intentando obtener profesor con ID: {}", id);
            System.out.println("[ACTA] 🔍 Buscando profesor con ID: " + id);
            try {
                if (teacherGrpcService == null) {
                    logger.error("[ACTA] ❌ TeacherGrpcService es NULL!");
                    System.out.println("[ACTA] ❌ TeacherGrpcService es NULL!");
                    continue;
                }

                var teacherResponse = teacherGrpcService.getTeacherById(id);
                logger.info("[ACTA] Respuesta gRPC para profesor {}: found={}", id,
                    teacherResponse != null ? teacherResponse.getFound() : "null");
                System.out.println("[ACTA] 📡 Respuesta gRPC profesor " + id + ": " +
                    (teacherResponse != null ? ("found=" + teacherResponse.getFound()) : "NULL"));

                if (teacherResponse != null && teacherResponse.getFound()) {
                    teachers.add(teacherResponse);
                    logger.info("[ACTA] ✅ Profesor {} obtenido exitosamente", id);
                    System.out.println("[ACTA] ✅ Profesor " + id + " obtenido exitosamente");
                } else {
                    logger.warn("[ACTA] ❌ Profesor {} no encontrado en gRPC", id);
                    System.out.println("[ACTA] ❌ Profesor " + id + " no encontrado en gRPC");
                }
            } catch (Exception ex) {
                logger.error("[ACTA] ❌ ERROR CRÍTICO obteniendo profesor {}: {}", id, ex.getMessage(), ex);
                System.out.println("[ACTA] ❌ ERROR CRÍTICO obteniendo profesor " + id + ": " + ex.getMessage());
                // Reemplazado ex.printStackTrace() por logging detallado
                logger.debug("[ACTA] Stack trace del error en profesor {}: ", id, ex);
            }
        }

        // Obtener administrativos via gRPC
        List<co.sena.edu.olympo_back.Administrative.AdministrativeResponse> administratives = new ArrayList<>();
        System.out.println("[ACTA] ===== OBTENIENDO ADMINISTRATIVOS =====");
        for (Long id : administrativeIds) {
            logger.info("[ACTA] Intentando obtener administrativo con ID: {}", id);
            System.out.println("[ACTA] 🔍 Buscando administrativo con ID: " + id);
            try {
                if (administrativeGrpcService == null) {
                    logger.error("[ACTA] ❌ AdministrativeGrpcService es NULL!");
                    System.out.println("[ACTA] ❌ AdministrativeGrpcService es NULL!");
                    continue;
                }

                var adminResponse = administrativeGrpcService.getAdministrativeById(id);
                logger.info("[ACTA] Respuesta gRPC para administrativo {}: found={}", id,
                    adminResponse != null ? adminResponse.getFound() : "null");
                System.out.println("[ACTA] 📡 Respuesta gRPC administrativo " + id + ": " +
                    (adminResponse != null ? ("found=" + adminResponse.getFound()) : "NULL"));

                if (adminResponse != null && adminResponse.getFound()) {
                    administratives.add(adminResponse);
                    logger.info("[ACTA] ✅ Administrativo {} obtenido exitosamente", id);
                    System.out.println("[ACTA] ✅ Administrativo " + id + " obtenido exitosamente");
                } else {
                    logger.warn("[ACTA] ❌ Administrativo {} no encontrado en gRPC", id);
                    System.out.println("[ACTA] ❌ Administrativo " + id + " no encontrado en gRPC");
                }
            } catch (Exception ex) {
                logger.error("[ACTA] ❌ ERROR CRÍTICO obteniendo administrativo {}: {}", id, ex.getMessage(), ex);
                System.out.println("[ACTA] ❌ ERROR CRÍTICO obteniendo administrativo " + id + ": " + ex.getMessage());
                // Reemplazado ex.printStackTrace() por logging detallado
                logger.debug("[ACTA] Stack trace del error en administrativo {}: ", id, ex);
            }
        }

        logger.info("[ACTA] ========== RESUMEN FINAL ==========");
        logger.info("[ACTA] Comité {}: Estudiantes obtenidos: {}/{}, Profesores obtenidos: {}/{}, Administrativos obtenidos: {}/{}",
                   committeeId, students.size(), studentIds.size(), teachers.size(), teacherIds.size(), administratives.size(), administrativeIds.size());

        System.out.println("[ACTA] ========== RESUMEN FINAL ==========");
        System.out.println("[ACTA] Comité " + committeeId + ":");
        System.out.println("[ACTA] 📊 Estudiantes obtenidos: " + students.size() + "/" + studentIds.size());
        System.out.println("[ACTA] 📊 Profesores obtenidos: " + teachers.size() + "/" + teacherIds.size());
        System.out.println("[ACTA] 📊 Administrativos obtenidos: " + administratives.size() + "/" + administrativeIds.size());

        if (students.isEmpty() && teachers.isEmpty() && administratives.isEmpty()) {
            logger.warn("[ACTA] ⚠️  NO SE OBTUVIERON PARTICIPANTES! Esto puede deberse a:");
            logger.warn("[ACTA] 1. Los IDs en el comité no existen en los microservicios");
            logger.warn("[ACTA] 2. Los servicios gRPC no están funcionando");
            logger.warn("[ACTA] 3. Los IDs no están correctamente asignados al comité");

            System.out.println("[ACTA] ⚠️  NO SE OBTUVIERON PARTICIPANTES! Esto puede deberse a:");
            System.out.println("[ACTA] 1. Los IDs en el comité no existen en los microservicios");
            System.out.println("[ACTA] 2. Los servicios gRPC no están funcionando");
            System.out.println("[ACTA] 3. Los IDs no están correctamente asignados al comité");
        }

        return new CommitteeParticipants(studentIds, students, teachers, administratives);
    }

    private static class CommitteeParticipants {
        final List<Long> studentIds;
        final List<co.sena.edu.olympo_back.Student.StudentResponse> students;
        final List<co.sena.edu.olympo_back.Teacher.TeacherResponse> teachers;
        final List<co.sena.edu.olympo_back.Administrative.AdministrativeResponse> administratives;

        CommitteeParticipants(List<Long> studentIds,
                             List<co.sena.edu.olympo_back.Student.StudentResponse> students,
                             List<co.sena.edu.olympo_back.Teacher.TeacherResponse> teachers,
                             List<co.sena.edu.olympo_back.Administrative.AdministrativeResponse> administratives) {
            this.studentIds = studentIds;
            this.students = students;
            this.teachers = teachers;
            this.administratives = administratives;
        }
    }
}
