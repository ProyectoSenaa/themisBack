package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.NoveltyDto;
import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Entity.NoveltyStatus;
import co.sena.edu.themis.Repository.NoveltyStatusRepository;
import co.sena.edu.themis.Repository.NoveltyTypeRepository;
import co.sena.edu.themis.Service.EmailService;
import co.sena.edu.themis.Service.NoveltyService;
import co.sena.edu.themis.Service.StudySheetGrpcService;
import co.sena.edu.themis.Strategy.NoveltyValidationContext;
import co.sena.edu.themis.Utils.mapper.NoveltyMapper;
import co.sena.edu.themis.Utils.validation.ValidationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.sena.edu.olympo_back.proto.PersonResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import co.sena.edu.themis.Entity.Notification;
import co.sena.edu.themis.Service.NotificationService;

@Component
public class NoveltyBusiness {
    private final NoveltyService noveltyService;
    private final EmailService emailService;
    private final ValidationUtils validationUtils;
    private final NoveltyValidationContext validationContext;
    private final NoveltyStatusRepository noveltyStatusRepository;
    private final NoveltyTypeRepository noveltyTypeRepository;
    private final StudySheetGrpcService studySheetGrpcService;
    private final NotificationService notificationService;

    private static final Logger logger = LoggerFactory.getLogger(NoveltyBusiness.class);

    public NoveltyBusiness(
            NoveltyService noveltyService,
            EmailService emailService,
            ValidationUtils validationUtils,
            NoveltyValidationContext validationContext,
            NoveltyStatusRepository noveltyStatusRepository,
            NoveltyTypeRepository noveltyTypeRepository,
            StudySheetGrpcService studySheetGrpcService,
            NotificationService notificationService
            ) {
        this.noveltyService = noveltyService;
        this.emailService = emailService;
        this.validationUtils = validationUtils;
        this.validationContext = validationContext;
        this.noveltyStatusRepository = noveltyStatusRepository;
        this.noveltyTypeRepository = noveltyTypeRepository;
        this.studySheetGrpcService = studySheetGrpcService;
        this.notificationService = notificationService;
    }

    public Page<NoveltyDto> allNovelties(int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Novelty> noveltyPage = noveltyService.findAll(pageRequest);
            return NoveltyMapper.INSTANCE.macroRegionsToMacroRegionDTOPage(noveltyPage);
        }, "Error retrieving novelties");
    }

    public Page<NoveltyDto> getNoveltiesByUser(Long userId, List<String> roles, int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Novelty> noveltyPage;

            if (roles.contains("ADMINISTRADOR")) {
                noveltyPage = noveltyService.findAll(pageRequest);
            } else {
                List<Novelty> novelties = new ArrayList<>();
                if (roles.contains("APRENDIZ")) {
                    novelties = noveltyService.findAllByIdStudent(userId);
                } else if (roles.contains("INSTRUCTOR")) {
                    novelties = noveltyService.findAllByIdTeacher(userId);
                }

                // Manual pagination for List
                int start = (int) pageRequest.getOffset();
                int end = Math.min((start + pageRequest.getPageSize()), novelties.size());
                List<Novelty> pageContent = (start > novelties.size()) ? List.of() : novelties.subList(start, end);

                noveltyPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageRequest,
                        novelties.size());
            }

            return NoveltyMapper.INSTANCE.macroRegionsToMacroRegionDTOPage(noveltyPage);
        }, "Error retrieving novelties for user");
    }

    public NoveltyDto noveltyById(Long id) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Novelty ID must not be null");
            Novelty novelty = noveltyService.getById(id);
            validationUtils.validateExists(novelty, "Novelty not found");
            return NoveltyMapper.INSTANCE.toDto(novelty);
        }, "Error retrieving novelty by ID");
    }

    public NoveltyDto addNovelty(NoveltyDto input) {
        System.out.println("Adding novelty bussssssssssssssssssssss: " + input);
        Novelty entity = NoveltyMapper.INSTANCE.toEntity(input);
        System.out.println("Novelty entity: " + entity);
        entity.setIsActive(true);
        entity.setDate(LocalDate.now());

        Novelty processedEntity = validationContext.validateAndProcess(entity);
        Novelty saved = noveltyService.save(processedEntity);

        if (input.getNoveltyType() != null) {
            emailService.sendNoveltyCreationEmail(saved, input.getNoveltyType());
        }

        // Crear una notificación vinculada a la novedad para notificar a coordinadores
        try {
            Notification notification = new Notification();
            String msg = "Nueva novedad creada";
            if (input.getNoveltyType() != null && input.getNoveltyType().getNameNovelty() != null) {
                msg += ": " + input.getNoveltyType().getNameNovelty();
            }
            notification.setNotiMessage(msg);
            notification.setNotiStatus("NEW");
            notification.setRegistrationDate(new Date());
            // asociar la entidad novelty para que el listener pueda deducir studentId
            notification.setNovelty(saved);
            // Guardar usando NotificationService (publica el evento y activa listeners)
            notificationService.save(notification);
        } catch (Exception e) {
            logger.warn("Error creando notificación automática para la novedad id={}: {}", saved.getId(), e.getMessage());
        }

        return NoveltyMapper.INSTANCE.toDto(saved);
    }

    public NoveltyDto updateNovelty(Long id, NoveltyDto input) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Novelty ID must not be null");
            Novelty existing = noveltyService.getById(id);
            validationUtils.validateExists(existing, "Novelty not found");

            if (input.getNoveltyStatus() != null && input.getNoveltyStatus().getId() != null) {
                Long statusId = input.getNoveltyStatus().getId();
                NoveltyStatus status = noveltyStatusRepository.findById(statusId)
                        .orElseThrow(() -> new RuntimeException("NoveltyStatus not found: " + statusId));

                existing.setNoveltyStatus(status);
                input.setNoveltyStatus(null);
            }

            NoveltyMapper.INSTANCE.updateEntityFromDto(input, existing);
            Novelty saved = noveltyService.save(existing);

            // Llamar al EmailService siempre para notificar actualizaciones; el servicio
            // resolverá el tipo si es necesario
            try {
                emailService.sendNoveltyUpdateEmail(saved, input.getNoveltyType());
            } catch (Exception e) {
                // Registrar el error, pero no interrumpir el flujo normal
                logger.error("Error enviando correo de actualización de novedad (id={}): {}", saved.getId(),
                        e.getMessage(), e);
            }

            return NoveltyMapper.INSTANCE.toDto(saved);
        }, "Error updating novelty");
    }

    public void deleteNovelty(Long id) {
        validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Novelty ID must not be null");
            Novelty existing = noveltyService.getById(id);
            validationUtils.validateExists(existing, "Novelty not found");
            noveltyService.deleteById(id);
        }, "Error deleting novelty");
    }

    public List<Novelty> findAllByIdStudent(Long studentId) {
        System.out.println(noveltyService.findAllByIdStudent(studentId));
        return noveltyService.findAllByIdStudent(studentId);
    }

    public List<Novelty> findAllByIdTeacher(Long teacherId) {
        return noveltyService.findAllByIdTeacher(teacherId);
    }

    public List<Novelty> findAllByIdAdministrative(Long administrativeId) {
        return noveltyService.findAllByIdAdministrative(administrativeId);
    }

    public NoveltyDto processNoveltyValidation(Long noveltyId) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(noveltyId, "Novelty ID must not be null");
            Novelty novelty = noveltyService.getById(noveltyId);
            validationUtils.validateExists(novelty, "Novelty not found");

            Novelty processedNovelty = validationContext.validateAndProcess(novelty);
            Novelty saved = noveltyService.save(processedNovelty);

            return NoveltyMapper.INSTANCE.toDto(saved);
        }, "Error processing novelty validation");
    }

    public List<Novelty> findAllByStudentIds(List<Long> studentIds) {
        return noveltyService.findAllByStudentIds(studentIds);
    }

    public NoveltyDto returnNovelty(Long id, String observation) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Novelty ID must not be null");
            validationUtils.validateNotNull(observation, "Observation is required to return the novelty");

            Novelty existing = noveltyService.getById(id);
            validationUtils.validateExists(existing, "Novelty not found");

            // Buscar estado DENEGADO ("denegado") sin usar enum
            String deniedName = "denegado";
            NoveltyStatus denied = noveltyStatusRepository.findByName(deniedName)
                    .orElseThrow(() -> new IllegalArgumentException("NoveltyStatus not found: " + deniedName));

            existing.setNoveltyStatus(denied);
            existing.setObservation(observation);

            Novelty saved = noveltyService.save(existing);
            return NoveltyMapper.INSTANCE.toDto(saved);
        }, "Error returning novelty");
    }
}
