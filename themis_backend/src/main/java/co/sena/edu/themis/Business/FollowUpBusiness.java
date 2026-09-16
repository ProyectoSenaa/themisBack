package co.sena.edu.themis.Business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import co.sena.edu.themis.Dto.FollowUpDto;
import co.sena.edu.themis.Dto.FollowUpTypeDto;
import co.sena.edu.themis.Entity.FollowUp;
import co.sena.edu.themis.Entity.FollowUpFlowStatus;
import co.sena.edu.themis.Entity.FollowUpStatus;
import co.sena.edu.themis.Entity.FollowUpType;
import co.sena.edu.themis.Repository.FollowUpStatusRepository;
import co.sena.edu.themis.Service.FollowUpFlowStatusService;
import co.sena.edu.themis.Service.FollowUpService;
import co.sena.edu.themis.Service.FollowUpStatusService;
import co.sena.edu.themis.Service.FollowUpTypeService;
import co.sena.edu.themis.Utils.mapper.FollowUpMapper;
import co.sena.edu.themis.Utils.validation.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Component
public class FollowUpBusiness {
    private static final Logger logger = LoggerFactory.getLogger(FollowUpBusiness.class);

    private final FollowUpService followUpService;
    private final ValidationUtils validationUtils;
    private final FollowUpTypeService followUpTypeService;
    private final FollowUpStatusService followUpStatusService;
    private final FollowUpFlowStatusService followUpFlowStatusService;
    private final FollowUpStatusRepository followUpStatusRepository;

    public FollowUpBusiness(FollowUpService followUpService,
                            ValidationUtils validationUtils,
                            FollowUpTypeService followUpTypeService,
                            FollowUpStatusService followUpStatusService,
                            FollowUpFlowStatusService followUpFlowStatusService,
                            FollowUpStatusRepository followUpStatusRepository) {
        this.followUpService = followUpService;
        this.validationUtils = validationUtils;
        this.followUpTypeService = followUpTypeService;
        this.followUpStatusService = followUpStatusService;
        this.followUpFlowStatusService = followUpFlowStatusService;
        this.followUpStatusRepository = followUpStatusRepository;
    }

    public Page<FollowUpDto> allFollowUps(int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<FollowUp> followUpPage = followUpService.findAll(pageRequest);

            // Manual mapping with fallback: map each entity and ensure studentId/teacherId are set
            List<FollowUpDto> dtoList = new ArrayList<>();
            for (FollowUp entity : followUpPage.getContent()) {
                FollowUpDto dto = FollowUpMapper.INSTANCE.toDto(entity);
                // fallback: if mapper didn't set the ids, copy from entity
                try {
                    if (dto.getStudentId() == null && entity.getStudentId() != null) {
                        dto.setStudentId(entity.getStudentId());
                    }
                    if (dto.getTeacherId() == null && entity.getTeacherId() != null) {
                        dto.setTeacherId(entity.getTeacherId());
                    }
                } catch (Exception e) {
                    logger.warn("Error applying fallback id mapping for followUp id={}", entity.getId(), e);
                }
                dtoList.add(dto);

                // Debug: log key ids for each DTO to diagnose missing teacher assignment
                try {
                    logger.info("FollowUpDto debug id={} studentId={} teacherId={} followUpTypeId= {}",
                            dto.getId(), dto.getStudentId(), dto.getTeacherId(), dto.getFollowUpTypeId());
                } catch (Exception e) {
                    logger.warn("Error logging FollowUpDto debug info", e);
                }
            }

            return new PageImpl<>(dtoList, followUpPage.getPageable(), followUpPage.getTotalElements());
        }, "Error retrieving follow-ups");
    }

    public FollowUpDto followUpById(Long id) {
        return validationUtils.tryExecute(() -> {
            FollowUp followUp = followUpService.getById(id);
            FollowUpDto dto = FollowUpMapper.INSTANCE.toDto(followUp);
            // Ensure followUpType object present for view
            if (dto.getFollowUpType() == null && followUp.getFollowUpType() != null) {
                dto.setFollowUpType(new FollowUpTypeDto(
                        followUp.getFollowUpType().getId(),
                        followUp.getFollowUpType().getName(),
                        followUp.getFollowUpType().isActive(),
                        followUp.getFollowUpType().getDescription()
                ));
            }
            return dto;
        }, "Error retrieving follow-up by ID");
    }

    public FollowUpDto addFollowUp(FollowUpDto followUpDto) {
        return validationUtils.tryExecute(() -> {
            // Log del input recibido
            logger.info("addFollowUp - Input received: followUpTypeId={}, followUpType={}, followUpStatusId={}, followUpStatus={}, followUpFlowStatusId={}, followUpFlowStatus={}",
                    followUpDto.getFollowUpTypeId(),
                    followUpDto.getFollowUpType() != null ? "NOT NULL (id=" + followUpDto.getFollowUpType().getId() + ")" : "NULL",
                    followUpDto.getFollowUpStatusId(),
                    followUpDto.getFollowUpStatus() != null ? "NOT NULL (id=" + followUpDto.getFollowUpStatus().getId() + ")" : "NULL",
                    followUpDto.getFollowUpFlowStatusId(),
                    followUpDto.getFollowUpFlowStatus() != null ? "NOT NULL (id=" + followUpDto.getFollowUpFlowStatus().getId() + ")" : "NULL");

            FollowUp followUp = FollowUpMapper.INSTANCE.toEntity(followUpDto);

            // Evitar que JPA intente persistir relaciones incompletas
            followUp.setFollowUpType(null);
            followUp.setFollowUpStatus(null);
            followUp.setFollowUpFlowStatus(null);

            // Establecer automáticamente la fecha de creación si no se proporciona
            if (followUp.getCreationDate() == null) {
                followUp.setCreationDate(java.time.LocalDate.now());
            }
            if (followUp.getIsActive() == null) {
                followUp.setIsActive(true);
            }

            // Asignar entidades persistentes por ID
            // Extraer ID del objeto anidado si existe
            Long followUpTypeId = followUpDto.getFollowUpTypeId();
            if (followUpTypeId == null && followUpDto.getFollowUpType() != null) {
                followUpTypeId = followUpDto.getFollowUpType().getId();
            }

            if (followUpTypeId != null) {
                logger.info("Setting followUpType with ID: {}", followUpTypeId);
                FollowUpType followUpType = followUpTypeService.getById(followUpTypeId);
                followUp.setFollowUpType(followUpType);
            }

            // Extraer ID del objeto anidado si existe
            Long followUpStatusId = followUpDto.getFollowUpStatusId();
            if (followUpStatusId == null && followUpDto.getFollowUpStatus() != null) {
                followUpStatusId = followUpDto.getFollowUpStatus().getId();
            }

            if (followUpStatusId != null) {
                logger.info("Setting followUpStatus with ID: {}", followUpStatusId);
                FollowUpStatus followUpStatus = followUpStatusService.getById(followUpStatusId);
                followUp.setFollowUpStatus(followUpStatus);
            } else {
                // Si no se envía el ID, asignar el estado por defecto (ejemplo: "en revision")
                logger.info("No followUpStatus provided, setting default: en revision");
                FollowUpStatus defaultStatus = followUpStatusService.findByName("en revision")
                        .orElseThrow(() -> new RuntimeException("Estado 'en revision' no encontrado"));
                followUp.setFollowUpStatus(defaultStatus);
            }

            // Extraer ID del objeto anidado si existe
            Long followUpFlowStatusId = followUpDto.getFollowUpFlowStatusId();
            if (followUpFlowStatusId == null && followUpDto.getFollowUpFlowStatus() != null) {
                followUpFlowStatusId = followUpDto.getFollowUpFlowStatus().getId();
            }

            if (followUpFlowStatusId != null) {
                logger.info("Setting followUpFlowStatus with ID: {}", followUpFlowStatusId);
                FollowUpFlowStatus followUpFlowStatus = followUpFlowStatusService.getById(followUpFlowStatusId);
                followUp.setFollowUpFlowStatus(followUpFlowStatus);
            } else {
                logger.info("No followUpFlowStatus provided, setting default: coordinacion");
                FollowUpFlowStatus defaultFlowStatus = followUpFlowStatusService.findByName("coordinacion");
                if (defaultFlowStatus != null) {
                    followUp.setFollowUpFlowStatus(defaultFlowStatus);
                } else {
                    throw new RuntimeException("Estado de flujo 'coordinacion' no encontrado");
                }
            }

            FollowUp savedFollowUp = followUpService.save(followUp);
            logger.info("FollowUp saved successfully with ID: {} and followUpType: {}",
                    savedFollowUp.getId(),
                    savedFollowUp.getFollowUpType() != null ? savedFollowUp.getFollowUpType().getId() : "NULL");

            FollowUpDto resultDto = FollowUpMapper.INSTANCE.toDto(savedFollowUp);
            // Ensure followUpType object present for view
            if (resultDto.getFollowUpType() == null && savedFollowUp.getFollowUpType() != null) {
                resultDto.setFollowUpType(new FollowUpTypeDto(
                        savedFollowUp.getFollowUpType().getId(),
                        savedFollowUp.getFollowUpType().getName(),
                        savedFollowUp.getFollowUpType().isActive(),
                        savedFollowUp.getFollowUpType().getDescription()
                ));
            }

            return resultDto;
        }, "Error adding follow-up");
    }

    public FollowUpDto updateFollowUp(Long id, FollowUpDto input) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Follow-up ID must not be null");
            FollowUp existing = followUpService.getById(id);
            validationUtils.validateExists(existing, "Follow-up not found");

            if (input.getFollowUpStatus() != null && input.getFollowUpStatus().getId() != null) {
                Long statusId = input.getFollowUpStatus().getId();
                FollowUpStatus status = followUpStatusRepository.findById(statusId)
                        .orElseThrow(() -> new RuntimeException("FollowUpStatus not found: " + statusId));

                existing.setFollowUpStatus(status);
                input.setFollowUpStatus(null);
            }

            FollowUpMapper.INSTANCE.updateEntityFromDto(input, existing);
            FollowUp saved = followUpService.save(existing);

            // Reload from DB to ensure we return the persisted state
            FollowUp persisted = followUpService.getById(saved.getId());

            logger.debug("Reloaded FollowUp id={} persisted caseDescription length={} statusId={} flowId={} typeId={}",
                    persisted.getId(),
                    persisted.getCaseDescription() != null ? persisted.getCaseDescription().length() : 0,
                    persisted.getFollowUpStatus() != null ? persisted.getFollowUpStatus().getId() : null,
                    persisted.getFollowUpFlowStatus() != null ? persisted.getFollowUpFlowStatus().getId() : null,
                    persisted.getFollowUpType() != null ? persisted.getFollowUpType().getId() : null);

            FollowUpDto dto = FollowUpMapper.INSTANCE.toDto(persisted);
            // Ensure followUpType object present for view
            if (dto.getFollowUpType() == null && persisted.getFollowUpType() != null) {
                dto.setFollowUpType(new FollowUpTypeDto(
                        persisted.getFollowUpType().getId(),
                        persisted.getFollowUpType().getName(),
                        persisted.getFollowUpType().isActive(),
                        persisted.getFollowUpType().getDescription()
                ));
            }

            return dto;
        }, "Error updating follow-up");
    }

    public void deleteFollowUp(Long id) {
        validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Follow-up ID cannot be null");
            followUpService.deleteById(id);
            return null;
        }, "Error deleting follow-up");
    }

    // Delegation methods used by DGS field resolvers (FollowUpData) — mirror
    // NoveltyBusiness
    public List<FollowUp> findAllByIdStudent(Long studentId) {
        return followUpService.findCasesByStudentId(studentId);
    }

    public Page<FollowUpDto> findAllByIdStudentPaginated(Long studentId, int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            List<FollowUp> allFollowUps = followUpService.findCasesByStudentId(studentId);

            // Manual pagination
            int start = (int) pageRequest.getOffset();
            int end = Math.min((start + pageRequest.getPageSize()), allFollowUps.size());
            List<FollowUp> pageContent = allFollowUps.subList(start, end);

            // Map to DTOs
            List<FollowUpDto> dtoList = new ArrayList<>();
            for (FollowUp entity : pageContent) {
                FollowUpDto dto = FollowUpMapper.INSTANCE.toDto(entity);
                // Ensure IDs are set
                if (dto.getStudentId() == null && entity.getStudentId() != null) {
                    dto.setStudentId(entity.getStudentId());
                }
                if (dto.getTeacherId() == null && entity.getTeacherId() != null) {
                    dto.setTeacherId(entity.getTeacherId());
                }
                // Ensure followUpType object present for view
                if (dto.getFollowUpType() == null && entity.getFollowUpType() != null) {
                    dto.setFollowUpType(new FollowUpTypeDto(
                            entity.getFollowUpType().getId(),
                            entity.getFollowUpType().getName(),
                            entity.getFollowUpType().isActive(),
                            entity.getFollowUpType().getDescription()
                    ));
                    if (dto.getFollowUpTypeId() == null) dto.setFollowUpTypeId(entity.getFollowUpType().getId());
                }
                dtoList.add(dto);
            }

            return new PageImpl<>(dtoList, pageRequest, allFollowUps.size());
        }, "Error retrieving follow-ups by student ID");
    }

    public List<FollowUp> findAllByIdTeacher(Long teacherId) {
        return followUpService.findCasesByTeacherId(teacherId);
    }

    public List<FollowUp> findAllByIdCoordinator(Long coordinatorId) {
        return followUpService.findByCoordinatorId(coordinatorId);
    }
}
