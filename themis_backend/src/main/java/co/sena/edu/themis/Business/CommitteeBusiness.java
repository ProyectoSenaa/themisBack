package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.CommitteeDto;
import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Entity.Committee;
import co.sena.edu.themis.Entity.CommitteeEvent;
import co.sena.edu.themis.Service.CommitteeEventService;
import co.sena.edu.themis.Service.CommitteeService;
import co.sena.edu.themis.Utils.mapper.CommitteeMapper;
import co.sena.edu.themis.Utils.validation.ValidationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CommitteeBusiness {

    private final CommitteeService committeeService;
    private final CommitteeEventService committeeEventService;
    private final CommitteeEventBusiness committeeEventBusiness;
    private final ValidationUtils validationUtils;
    private final CommitteeMapper committeeMapper;

    public CommitteeBusiness(CommitteeService committeeService, CommitteeEventService committeeEventService, CommitteeEventBusiness committeeEventBusiness, ValidationUtils validationUtils, CommitteeMapper committeeMapper) {
        this.committeeService = committeeService;
        this.committeeEventService = committeeEventService;
        this.committeeEventBusiness = committeeEventBusiness;
        this.validationUtils = validationUtils;
        this.committeeMapper = committeeMapper;
    }

    public Page<CommitteeDto> allCommittees(int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Committee> committeePage = committeeService.findAll(pageRequest);
            return committeeMapper.committeesToCommitteeDTOPage(committeePage);
        }, "Error retrieving committees");
    }

    public CommitteeDto committeeById(Long id) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Committee ID must not be null");
            Committee committee = committeeService.getById(id);
            validationUtils.validateExists(committee, "Committee not found");
            return committeeMapper.toDto(committee);
        }, "Error retrieving committee by ID");
    }

    public CommitteeDto addCommittee(CommitteeDto input) {
        return validationUtils.tryExecute(() -> {
            // Log de entrada para debugging
            System.out.println("🔍 [DEBUG] === CREANDO COMITÉ ===");
            System.out.println("🔍 [DEBUG] Input recibido:");
            System.out.println("  - coordinationId: " + input.getCoordinationId());
            System.out.println("  - isCurrent: " + input.isCurrent());
            System.out.println("  - isActive: " + input.isActive());
            System.out.println("  - eventIds ESPECÍFICOS: " + input.getEventIds());
            System.out.println("  - eventIds size: " + (input.getEventIds() != null ? input.getEventIds().size() : "NULL"));

            Committee entity = committeeMapper.toEntity(input);

            // Inicializar listas vacías si son null
            if (entity.getStudentsIds() == null) {
                entity.setStudentsIds(new ArrayList<>());
            }
            if (entity.getTeachersIds() == null) {
                entity.setTeachersIds(new ArrayList<>());
            }
            if (entity.getAdministrativesIds() == null) {
                entity.setAdministrativesIds(new ArrayList<>());
            }

            // Establecer valores por defecto para campos requeridos
            entity.setActive(true);  // Usar setActive para el campo isActive

            // Establecer isCurrent desde el input
            entity.setCurrent(input.isCurrent());  // Usar setCurrent para el campo isCurrent

            // Guardar el comité
            Committee saved = committeeService.save(entity);
            System.out.println("✅ [DEBUG] Comité guardado con ID: " + saved.getId());

            // 🎯 ASOCIACIÓN SELECTIVA: Solo eventos específicamente seleccionados
            if (input.getEventIds() != null && !input.getEventIds().isEmpty()) {
                System.out.println("🔄 [DEBUG] === ASOCIACIÓN SELECTIVA INICIADA ===");
                System.out.println("🔄 [DEBUG] Eventos ESPECÍFICOS a asociar: " + input.getEventIds());
                System.out.println("🔄 [DEBUG] Comité destino: " + saved.getId());

                try {
                    List<CommitteeEventDto> result = committeeEventBusiness.assignCommitteeToEvents(saved.getId(), input.getEventIds());

                    System.out.println("✅ [DEBUG] === ASOCIACIÓN SELECTIVA COMPLETADA ===");
                    System.out.println("✅ [DEBUG] Eventos asociados exitosamente: " + result.size());

                    if (result.size() > 0) {
                        System.out.println("📋 [DEBUG] Detalles de eventos asociados:");
                        for (CommitteeEventDto event : result) {
                            System.out.println("  ✅ ID: " + event.getId() +
                                             " | Fecha: " + event.getDate() +
                                             " | Hora: " + event.getHour() +
                                             " | Coordinación: " + event.getCoordinationName());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ [DEBUG] Error al asociar eventos específicos al comité: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("ℹ️ [DEBUG] === SIN ASOCIACIÓN ===");
                System.out.println("ℹ️ [DEBUG] No se proporcionaron eventIds específicos");
                System.out.println("ℹ️ [DEBUG] El comité se creó SIN eventos asociados");
                System.out.println("ℹ️ [DEBUG] Para asociar eventos, usa assignCommitteeToEvents por separado");
            }

            System.out.println("🔍 [DEBUG] === FIN CREACIÓN COMITÉ ===");
            return committeeMapper.toDto(saved);
        }, "Error adding committee");
    }

    public CommitteeDto updateCommittee(Long id, CommitteeDto input) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Committee ID must not be null");
            Committee existing = committeeService.getById(id);
            validationUtils.validateExists(existing, "Committee not found");
            committeeMapper.updateEntityFromDto(input, existing);
            Committee saved = committeeService.save(existing);
            return committeeMapper.toDto(saved);
        }, "Error updating committee");
    }

    public void deleteCommittee(Long id) {
        validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Committee ID must not be null");
            Committee existing = committeeService.getById(id);
            validationUtils.validateExists(existing, "Committee not found");
            existing.setActive(false); // Usar setActive para el campo isActive
            committeeService.save(existing);
            return null;
        }, "Error deleting committee");
    }

    public List<CommitteeDto> findCommitteesByStudentId(Long studentId) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(studentId, "Student ID must not be null");
            List<Committee> committees = committeeService.findCommitteesByStudentId(studentId);
            return committeeMapper.committeeToList(committees);
        }, "Error retrieving committees by student ID");
    }

    public List<CommitteeDto> findCommitteesByTeacherId(Long teacherId) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(teacherId, "Teacher ID must not be null");
            List<Committee> committees = committeeService.findCommitteesByTeacherId(teacherId);
            return committeeMapper.committeeToList(committees);
        }, "Error retrieving committees by teacher ID");
    }

    public List<CommitteeDto> findCommitteesByAdministrativeId(Long administrativeId) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(administrativeId, "Administrative ID must not be null");
            List<Committee> committees = committeeService.findCommitteesByAdministrativeId(administrativeId);
            return committeeMapper.committeeToList(committees);
        }, "Error retrieving committees by administrative ID");
    }

    public List<CommitteeDto> findActiveCommittees() {
        return validationUtils.tryExecute(() -> {
            List<Committee> committees = committeeService.findActiveCommittees();
            return committeeMapper.committeeToList(committees);
        }, "Error retrieving active committees");
    }

    public List<CommitteeDto> findCommitteesByCoordinationId(Long coordinationId) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(coordinationId, "Coordination ID must not be null");
            List<Committee> committees = committeeService.findCommitteesByCoordinationId(coordinationId);
            return committeeMapper.committeeToList(committees);
        }, "Error retrieving committees by coordination ID");
    }


    public CommitteeDto addPersonToCommittee(Long committeeId, Long personId, String role) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(committeeId, "Committee ID must not be null");
            validationUtils.validateNotNull(personId, "Person ID must not be null");
            validationUtils.validateNotNull(role, "Role must not be null");

            Committee committee = committeeService.getById(committeeId);
            validationUtils.validateExists(committee, "Committee not found");
            if (committee.getStudentsIds() == null) {
                committee.setStudentsIds(new ArrayList<>());
            }
            if (committee.getTeachersIds() == null) {
                committee.setTeachersIds(new ArrayList<>());
            }
            if (committee.getAdministrativesIds() == null) {
                committee.setAdministrativesIds(new ArrayList<>());
            }

            switch (role.toLowerCase()) {
                case "student":
                    if (!committee.getStudentsIds().contains(personId)) {
                        committee.getStudentsIds().add(personId);
                    }
                    break;
                case "teacher":
                    if (!committee.getTeachersIds().contains(personId)) {
                        committee.getTeachersIds().add(personId);
                    }
                    break;
                case "administrative":
                    if (!committee.getAdministrativesIds().contains(personId)) {
                        committee.getAdministrativesIds().add(personId);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Role must be student, teacher or administrative");
            }

            Committee saved = committeeService.save(committee);
            return committeeMapper.toDto(saved);
        }, "Error adding person to committee");
    }


    public CommitteeDto removePersonFromCommittee(Long committeeId, Long personId, String role) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(committeeId, "Committee ID must not be null");
            validationUtils.validateNotNull(personId, "Person ID must not be null");
            validationUtils.validateNotNull(role, "Role must not be null");

            Committee committee = committeeService.getById(committeeId);
            validationUtils.validateExists(committee, "Committee not found");

            switch (role.toLowerCase()) {
                case "student":
                    if (committee.getStudentsIds() != null) {
                        committee.getStudentsIds().remove(personId);
                    }
                    break;
                case "teacher":
                    if (committee.getTeachersIds() != null) {
                        committee.getTeachersIds().remove(personId);
                    }
                    break;
                case "administrative":
                    if (committee.getAdministrativesIds() != null) {
                        committee.getAdministrativesIds().remove(personId);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Role must be student, teacher or administrative");
            }

            Committee saved = committeeService.save(committee);
            return committeeMapper.toDto(saved);
        }, "Error removing person from committee");
    }
}