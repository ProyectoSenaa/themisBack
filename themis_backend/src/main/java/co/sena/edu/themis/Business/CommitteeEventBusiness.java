package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Entity.Committee;
import co.sena.edu.themis.Entity.CommitteeEvent;
import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Entity.NoveltyStatus;
import co.sena.edu.themis.Entity.ProcessFlowStatus;
import co.sena.edu.themis.Service.CommitteeEventService;
import co.sena.edu.themis.Service.CommitteeService;
import co.sena.edu.themis.Service.EmailCommitteeService;
import co.sena.edu.themis.Service.NoveltyService;
import co.sena.edu.themis.Service.NoveltyStatusService;
import co.sena.edu.themis.Service.ProcessFlowStatusService;
import co.sena.edu.themis.Utils.mapper.CommitteeEventMapper;
import co.sena.edu.themis.Utils.mapper.CommitteeMapper;
import co.sena.edu.themis.Utils.validation.ValidationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CommitteeEventBusiness {
    private static final Logger logger = LoggerFactory.getLogger(CommitteeEventBusiness.class);

    private final CommitteeEventService committeeEventService;
    private final ValidationUtils validationUtils;
    private final EmailCommitteeService emailCommitteeService;
    private final CommitteeEventMapper committeeEventMapper;
    private final CommitteeService committeeService;
    private final NoveltyService noveltyService;
    private final CommitteeMapper committeeMapper;
    private final NoveltyStatusService noveltyStatusService;
    private final ProcessFlowStatusService processFlowStatusService;

    public CommitteeEventBusiness(
            CommitteeEventService committeeEventService,
            ValidationUtils validationUtils,
            EmailCommitteeService emailCommitteeService,
            CommitteeEventMapper committeeEventMapper,
            CommitteeService committeeService,
            CommitteeMapper committeeMapper,
            NoveltyService noveltyService,
            NoveltyStatusService noveltyStatusService,
            ProcessFlowStatusService processFlowStatusService) {
        this.committeeEventService = committeeEventService;
        this.validationUtils = validationUtils;
        this.emailCommitteeService = emailCommitteeService;
        this.committeeEventMapper = committeeEventMapper;
        this.committeeService = committeeService;
        this.committeeMapper = committeeMapper;
        this.noveltyService = noveltyService;
        this.noveltyStatusService = noveltyStatusService;
        this.processFlowStatusService = processFlowStatusService;
    }

    public Page<CommitteeEventDto> allEvents(int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<CommitteeEvent> eventPage = committeeEventService.findAll(pageRequest);
            return committeeEventMapper.committeeEventsToCommitteeEventDTOPage(eventPage);
        }, "Error retrieving committee events");
    }

    public CommitteeEventDto eventById(Long id) {
        logger.info("[EVENTO] Buscando evento por ID: {}", id);
        System.out.println("[EVENTO] Buscando evento por ID: " + id);
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Event ID must not be null");
            CommitteeEvent event = committeeEventService.getById(id);
            logger.info("[EVENTO] Evento obtenido: {}", event);
            System.out.println("[EVENTO] Evento obtenido: " + event);
            validationUtils.validateExists(event, "Event not found");
            CommitteeEventDto dto = committeeEventMapper.toDto(event);
            logger.info("[EVENTO] DTO mapeado: {}", dto);
            System.out.println("[EVENTO] DTO mapeado: " + dto);
            if (dto.getCommittee() != null && dto.getCommittee().getId() != null) {
                logger.info("[EVENTO] Cargando comité completo para ID: {}", dto.getCommittee().getId());
                System.out.println("[EVENTO] Cargando comité completo para ID: " + dto.getCommittee().getId());
                Committee fullCommittee = committeeService.getById(dto.getCommittee().getId());
                logger.info("[EVENTO] Comité completo obtenido: {}", fullCommittee);
                System.out.println("[EVENTO] Comité completo obtenido: " + fullCommittee);
                dto.setCommittee(committeeMapper.toDto(fullCommittee));
                logger.info("[EVENTO] DTO con comité completo: {}", dto);
                System.out.println("[EVENTO] DTO con comité completo: " + dto);
            } else {
                logger.warn("[EVENTO] El evento no tiene comité asociado o el ID es nulo");
                System.out.println("[EVENTO] El evento no tiene comité asociado o el ID es nulo");
            }

            return dto;
        }, "Error retrieving event by ID");
    }

    public CommitteeEventDto addEvent(CommitteeEventDto input) {
        return validationUtils.tryExecute(() -> {
            CommitteeEvent entity = committeeEventMapper.toEntity(input);

            validationUtils.validateNotNull(entity, "Input mapping resulted in a null entity.");

            validationUtils.validateNotNull(entity.getDate(), "Date must not be null");
            validationUtils.validateNotNull(entity.getHour(), "Hour must not be null");
            if (entity.getSession() == null || entity.getSession().trim().isEmpty()) {
                throw new IllegalArgumentException("Session must not be null or empty");
            }

            // Si se proporciona un committee ID, cargar el comité completo desde la base de datos
            if (entity.getCommittee() != null && entity.getCommittee().getId() != null) {
                Committee committee = committeeService.getById(entity.getCommittee().getId());
                validationUtils.validateExists(committee, "Committee not found");
                if (!committee.isActive()) {
                    throw new IllegalArgumentException("Cannot schedule events for inactive committees");
                }
                boolean collision = committeeEventService.existsEventCollision(committee.getId(), entity.getDate(), entity.getHour());
                if (collision) {
                    throw new IllegalStateException("El comité ya tiene un evento en la misma fecha y hora");
                }
                // Asignar el comité completo cargado desde la base de datos
                entity.setCommittee(committee);
            }

            CommitteeEvent saved = committeeEventService.save(entity);

            CommitteeEventDto savedDto = this.committeeEventMapper.toDto(saved);

            // Cargar comité completo para que el frontend reciba studentsIds, teachersIds y isActive
            if (savedDto.getCommittee() != null && savedDto.getCommittee().getId() != null) {
                Committee fullCommittee = committeeService.getById(savedDto.getCommittee().getId());
                savedDto.setCommittee(committeeMapper.toDto(fullCommittee));
            }

            if (savedDto.getCommittee() != null && savedDto.getCommittee().getId() != null) {
                emailCommitteeService.sendCommitteeEventCreationEmail(savedDto);
            }

            return savedDto;
        }, "Error al registrar el comitè");
    }

    public CommitteeEventDto updateEvent(Long id, CommitteeEventDto input) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Event ID must not be null");
            CommitteeEvent existing = committeeEventService.getById(id);
            validationUtils.validateExists(existing, "Event not found");

            Long beforeCommitteeId = (existing.getCommittee() != null) ? existing.getCommittee().getId() : null;

            committeeEventMapper.updateEntityFromDto(input, existing);

            validationUtils.validateNotNull(existing.getDate(), "Date must not be null");
            validationUtils.validateNotNull(existing.getHour(), "Hour must not be null");
            if (existing.getSession() == null || existing.getSession().trim().isEmpty()) {
                throw new IllegalArgumentException("Session must not be null or empty");
            }

            if (existing.getCommittee() != null && existing.getCommittee().getId() != null) {
                Committee committee = committeeService.getById(existing.getCommittee().getId());
                validationUtils.validateExists(committee, "Committee not found");
                if (!committee.isActive()) {
                    throw new IllegalArgumentException("Cannot schedule events for inactive committees");
                }
                boolean collision = committeeEventService.existsEventCollisionExcluding(existing.getId(), committee.getId(), existing.getDate(), existing.getHour());
                if (collision) {
                    throw new IllegalStateException("El comité ya tiene un evento en la misma fecha y hora");
                }
            }

            CommitteeEvent saved = committeeEventService.save(existing);
            CommitteeEventDto savedDto = committeeEventMapper.toDto(saved);

            // Si hay comité, cargarlo completo
            if (saved.getCommittee() != null && saved.getCommittee().getId() != null) {
                Committee fullCommittee = committeeService.getById(saved.getCommittee().getId());
                savedDto.setCommittee(committeeMapper.toDto(fullCommittee));
            }

            Long afterCommitteeId = (saved.getCommittee() != null) ? saved.getCommittee().getId() : null;

            if (beforeCommitteeId == null && afterCommitteeId != null) {
                emailCommitteeService.sendCommitteeEventCreationEmail(savedDto);
            }

            return savedDto;
        }, "Error updating event");
    }

    public void deleteEvent(Long id) {
        validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Event ID must not be null");
            CommitteeEvent existing = committeeEventService.getById(id);
            validationUtils.validateExists(existing, "Event not found");
            committeeEventService.deleteById(id);
            return null;
        }, "Error deleting event");
    }

    public List<CommitteeEventDto> eventsByCommittee(Long committeeId) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(committeeId, "Committee ID must not be null");
            List<CommitteeEvent> events = committeeEventService.getCommitteeMeetings(committeeId);
            return committeeEventMapper.committeeEventToList(events);
        }, "Error retrieving events by committee ID");
    }

    public List<CommitteeEventDto> eventsByDate(Long committeeId, LocalDate date) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(committeeId, "Committee ID must not be null");
            validationUtils.validateNotNull(date, "Date must not be null");
            List<CommitteeEvent> events = committeeEventService.getCommitteeMeetingsByDate(committeeId, date);
            return committeeEventMapper.committeeEventToList(events);
        }, "Error retrieving events by date");
    }

    public int addEventsBulk(List<CommitteeEventDto> inputs) {
        return validationUtils.tryExecute(() -> {
            if (inputs == null || inputs.isEmpty()) {
                return 0;
            }

            List<CommitteeEvent> toSave = new ArrayList<>();
            for (CommitteeEventDto dto : inputs) {
                CommitteeEvent e = committeeEventMapper.toEntity(dto);

                validationUtils.validateNotNull(e.getDate(), "Date must not be null");
                validationUtils.validateNotNull(e.getHour(), "Hour must not be null");
                if (e.getSession() == null || e.getSession().trim().isEmpty()) {
                    throw new IllegalArgumentException("Session must not be null or empty");
                }

                // En la carga masiva, el committee_id inicialmente es null
                // Se asignará posteriormente con una mutación separada
                toSave.add(e);
            }

            int count = 0;
            for (CommitteeEvent e : toSave) {
                committeeEventService.save(e);
                count++;
            }
            return count;
        }, "Error adding events in bulk");
    }

    public List<CommitteeEventDto> addEventsBulkWithReturn(List<CommitteeEventDto> inputs) {
        return validationUtils.tryExecute(() -> {
            if (inputs == null || inputs.isEmpty()) {
                return new ArrayList<>();
            }

            List<CommitteeEventDto> createdEvents = new ArrayList<>();
            for (CommitteeEventDto dto : inputs) {
                CommitteeEvent e = committeeEventMapper.toEntity(dto);

                validationUtils.validateNotNull(e.getDate(), "Date must not be null");
                validationUtils.validateNotNull(e.getHour(), "Hour must not be null");
                if (e.getSession() == null || e.getSession().trim().isEmpty()) {
                    throw new IllegalArgumentException("Session must not be null or empty");
                }

                // Si se proporciona un committee ID, cargar el comité completo desde la base de datos
                if (e.getCommittee() != null && e.getCommittee().getId() != null) {
                    Committee committee = committeeService.getById(e.getCommittee().getId());
                    validationUtils.validateExists(committee, "Committee not found");
                    if (!committee.isActive()) {
                        throw new IllegalArgumentException("Cannot schedule events for inactive committees");
                    }
                    boolean collision = committeeEventService.existsEventCollision(committee.getId(), e.getDate(), e.getHour());
                    if (collision) {
                        throw new IllegalStateException("El comité ya tiene un evento en la misma fecha y hora");
                    }
                    // Asignar el comité completo cargado desde la base de datos
                    e.setCommittee(committee);
                }

                CommitteeEvent savedEvent = committeeEventService.save(e);
                CommitteeEventDto savedDto = committeeEventMapper.toDto(savedEvent);

                // Cargar comité completo para que el frontend reciba studentsIds, teachersIds y isActive
                if (savedDto.getCommittee() != null && savedDto.getCommittee().getId() != null) {
                    Committee fullCommittee = committeeService.getById(savedDto.getCommittee().getId());
                    savedDto.setCommittee(committeeMapper.toDto(fullCommittee));
                }

                createdEvents.add(savedDto);
            }

            return createdEvents;
        }, "Error adding events in bulk with return");
    }

    public int finalizeCommitteeEvent(Long eventId, List<Map<String, Object>> responses) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(eventId, "eventId must not be null");
            CommitteeEvent event = committeeEventService.getById(eventId);
            validationUtils.validateExists(event, "Committee event not found");

            int updated = 0;

            if (event.getCommittee() != null && event.getCommittee().getId() != null) {
                Committee committee = committeeService.getById(event.getCommittee().getId());
                if (committee != null && committee.getStudentsIds() != null && !committee.getStudentsIds().isEmpty()) {
                    List<Long> committeeStudentIds = committee.getStudentsIds();
                    if (responses != null && !responses.isEmpty()) {
                        Map<Long, String> obsMap = new HashMap<>();

                        for (Map<String, Object> response : responses) {
                            Object studentIdObj = response.get("studentId");
                            Object observationObj = response.get("observation");

                            if (studentIdObj != null && observationObj != null) {
                                Long studentId = null;
                                if (studentIdObj instanceof Number) {
                                    studentId = ((Number) studentIdObj).longValue();
                                } else if (studentIdObj instanceof String) {
                                    try {
                                        studentId = Long.parseLong((String) studentIdObj);
                                    } catch (NumberFormatException e) {
                                        continue;
                                    }
                                }

                                if (studentId != null && committeeStudentIds.contains(studentId)) {
                                    String observation = observationObj.toString();
                                    obsMap.put(studentId, observation);
                                }
                            }
                        }

                        if (!obsMap.isEmpty()) {
                            List<Novelty> candidateNovelties = noveltyService.findAllByStudentIds(new ArrayList<>(obsMap.keySet()));
                            if (!candidateNovelties.isEmpty()) {
                                List<String> openStatuses = Arrays.asList("pendiente", "en proceso", "denegado");
                                for (Novelty n : candidateNovelties) {
                                    if (n.getProcessFlowStatus() == null || n.getProcessFlowStatus().getName() == null) {
                                        continue;
                                    }
                                    if (!"comite".equalsIgnoreCase(n.getProcessFlowStatus().getName())) {
                                        continue;
                                    }
                                    String statusName = (n.getNoveltyStatus() != null && n.getNoveltyStatus().getName() != null) ? n.getNoveltyStatus().getName().toLowerCase() : "";
                                    if (!openStatuses.contains(statusName)) {
                                        continue;
                                    }
                                    String obs = obsMap.get(n.getStudentId());
                                    if (obs != null && !obs.trim().isEmpty()) {
                                        n.setObservation(obs.trim());
                                        // Asociar la novedad con el evento del comité
                                        n.setCommitteeEventId(eventId);
                                        noveltyService.save(n);
                                        updated++;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            committeeEventService.markFinished(eventId);

            return updated;
        }, "Error finalizing committee event");
    }

    /**
     * Finaliza un evento de comité sin procesar novedades
     */
    public void finalizeCommitteeEventOnly(Long eventId) {
        validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(eventId, "eventId must not be null");
            CommitteeEvent event = committeeEventService.getById(eventId);
            validationUtils.validateExists(event, "Committee event not found");

            committeeEventService.markFinished(eventId);
            return null;
        }, "Error finalizing committee event");
    }

    /**
     * Procesa respuestas de novedades desde el comité con estados aprobado/no aprobado
     */
    public int respondNoveltiesFromCommittee(Long committeeId, List<Map<String, Object>> responses) {
        return validationUtils.tryExecute(() -> {
            System.out.println("🔍 [RESPOND_NOVELTIES] === INICIANDO PROCESO ===");
            System.out.println("🔍 [RESPOND_NOVELTIES] committeeId: " + committeeId);
            System.out.println("🔍 [RESPOND_NOVELTIES] responses recibidas: " + responses);

            validationUtils.validateNotNull(committeeId, "committeeId must not be null");
            Committee committee = committeeService.getById(committeeId);
            validationUtils.validateExists(committee, "Committee not found");

            int updated = 0;

            if (responses != null && !responses.isEmpty()) {
                System.out.println("🔍 [RESPOND_NOVELTIES] Procesando " + responses.size() + " respuestas");

                for (Map<String, Object> response : responses) {
                    System.out.println("🔍 [RESPOND_NOVELTIES] Procesando respuesta: " + response);

                    Object noveltyIdObj = response.get("noveltyId");
                    Object nameObj = response.get("name");
                    Object observationObj = response.get("observation");
                    Object statusIdObj = response.get("statusId");

                    if (noveltyIdObj != null && nameObj != null) {
                        Long noveltyId = null;
                        if (noveltyIdObj instanceof Number) {
                            noveltyId = ((Number) noveltyIdObj).longValue();
                        } else if (noveltyIdObj instanceof String) {
                            try {
                                noveltyId = Long.parseLong((String) noveltyIdObj);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                        }

                        if (noveltyId != null) {
                            System.out.println("🔍 [RESPOND_NOVELTIES] Buscando novedad ID: " + noveltyId);

                            // Buscar la novedad específica por ID
                            Novelty novelty = noveltyService.getById(noveltyId);
                            if (novelty != null) {
                                System.out.println("🔍 [RESPOND_NOVELTIES] Novedad encontrada - Estado actual: " +
                                        (novelty.getNoveltyStatus() != null ? novelty.getNoveltyStatus().getName() : "null"));

                                // Actualizamos el flujo de proceso a "comité"
                                ProcessFlowStatus comiteStatus = processFlowStatusService.findByName("comite");
                                if (comiteStatus != null) {
                                    novelty.setProcessFlowStatus(comiteStatus);
                                }

                                // Actualizar directamente al estado final según el statusId recibido
                                if (statusIdObj != null) {
                                    Long statusId = ((Number) statusIdObj).longValue();
                                    NoveltyStatus finalStatus = noveltyStatusService.getById(statusId);
                                    if (finalStatus != null) {
                                        System.out.println("🔍 [RESPOND_NOVELTIES] Actualizando a estado final: " + finalStatus.getName());
                                        novelty.setNoveltyStatus(finalStatus);
                                    }
                                }

                                // Agregar observación si se proporcionó
                                if (observationObj != null && !observationObj.toString().trim().isEmpty()) {
                                    novelty.setObservation(observationObj.toString().trim());
                                    System.out.println("🔍 [RESPOND_NOVELTIES] Observación agregada: " + observationObj.toString().trim());
                                }

                                noveltyService.save(novelty);
                                updated++;
                                System.out.println("✅ [RESPOND_NOVELTIES] Novedad " + noveltyId + " actualizada correctamente");
                            } else {
                                System.out.println("❌ [RESPOND_NOVELTIES] Novedad " + noveltyId + " no encontrada");
                            }
                        }
                    } else {
                        System.out.println("⚠️ [RESPOND_NOVELTIES] Respuesta incompleta - noveltyId o name faltante: " + response);
                    }
                }
            } else {
                System.out.println("⚠️ [RESPOND_NOVELTIES] No se recibieron respuestas para procesar");
            }

            System.out.println("✅ [RESPOND_NOVELTIES] Proceso completado. Novedades actualizadas: " + updated);
            return updated;
        }, "Error responding novelties from committee");
    }

    /**
     * Asocia un comité a eventos existentes que no tienen comité asignado - VERSIÓN OPTIMIZADA
     * GARANTIZA que solo se procesen los eventos con los IDs específicamente seleccionados
     */
    public List<CommitteeEventDto> assignCommitteeToEvents(Long committeeId, List<Long> eventIds) {
        return validationUtils.tryExecute(() -> {
            // 📊 STEP 1: Validar parámetros de entrada
            System.out.println("🔍 [ASSIGN_COMMITTEE] === INICIO DEL PROCESO OPTIMIZADO ===");
            System.out.println("🔍 [ASSIGN_COMMITTEE] Parámetros recibidos:");
            System.out.println("  - committeeId: " + committeeId);
            System.out.println("  - eventIds EXACTOS seleccionados: " + eventIds);
            System.out.println("  - eventIds.size(): " + (eventIds != null ? eventIds.size() : "NULL"));

            validationUtils.validateNotNull(committeeId, "Committee ID must not be null");
            validationUtils.validateNotNull(eventIds, "Event IDs list must not be null");

            if (eventIds == null || eventIds.isEmpty()) {
                System.out.println("⚠️ [ASSIGN_COMMITTEE] Lista de eventIds está vacía, retornando lista vacía");
                return new ArrayList<>();
            }

            // 📊 STEP 2: Validar que el comité existe y está activo
            System.out.println("🔍 [ASSIGN_COMMITTEE] Validando comité...");
            Committee committee = committeeService.getById(committeeId);
            validationUtils.validateExists(committee, "Committee not found");
            if (!committee.isActive()) {
                throw new IllegalArgumentException("Cannot assign inactive committee to events");
            }
            System.out.println("✅ [ASSIGN_COMMITTEE] Comité válido: " + committee.getId());

            // Procesar eventos y asignar comité
            List<CommitteeEventDto> result = new ArrayList<>();
            for (Long eventId : eventIds) {
                try {
                    CommitteeEvent event = committeeEventService.getById(eventId);
                    if (event != null) {
                        event.setCommittee(committee);
                        CommitteeEvent savedEvent = committeeEventService.save(event);
                        CommitteeEventDto dto = committeeEventMapper.toDto(savedEvent);
                        if (dto.getCommittee() != null && dto.getCommittee().getId() != null) {
                            Committee fullCommittee = committeeService.getById(dto.getCommittee().getId());
                            dto.setCommittee(committeeMapper.toDto(fullCommittee));
                        }
                        result.add(dto);
                    }
                } catch (Exception e) {
                    System.out.println("Error procesando evento " + eventId + ": " + e.getMessage());
                }
            }

            return result;
        }, "Error assigning committee to events");
    }
}