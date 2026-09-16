package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.FollowUpTypeDto;
import co.sena.edu.themis.Entity.FollowUpType;
import co.sena.edu.themis.Service.FollowUpTypeService;
import co.sena.edu.themis.Utils.mapper.FollowUpTypeMapper;
import co.sena.edu.themis.Utils.validation.ValidationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;


@Component
public class FollowUpTypeBusiness {
    private final FollowUpTypeService followUpTypeService;
    private final ValidationUtils validationUtils;
    private final FollowUpTypeMapper followUpTypeMapper = FollowUpTypeMapper.INSTANCE;

    public FollowUpTypeBusiness(FollowUpTypeService followUpTypeService, ValidationUtils validationUtils) {
        this.followUpTypeService = followUpTypeService;
        this.validationUtils = validationUtils;
    }

    public Page<FollowUpTypeDto> findAll(int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<FollowUpType> followUpTypePage = followUpTypeService.findAll(pageRequest);
            return FollowUpTypeMapper.INSTANCE.followUpTypesToFollowUpTypeDTOPage(followUpTypePage);
        }, "Error retrieving follow-up types");
    }

    public FollowUpTypeDto followUpTypeById(Long id) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Follow-up type ID cannot be null");
            FollowUpType followUpType = followUpTypeService.getById(id);
            return FollowUpTypeMapper.INSTANCE.followUpTypeToFollowUpTypeDTO(followUpType);
        }, "Error retrieving follow-up type by ID");
    }

    public FollowUpTypeDto addFollowUpType(FollowUpTypeDto followUpTypeDto) {
        return validationUtils.tryExecute(() -> {
            FollowUpType followUpType = FollowUpTypeMapper.INSTANCE.followUpTypeDTOToFollowUpType(followUpTypeDto);
            FollowUpType savedFollowUpType = followUpTypeService.save(followUpType);
            return FollowUpTypeMapper.INSTANCE.followUpTypeToFollowUpTypeDTO(savedFollowUpType);
        }, "Error adding follow-up type");
    }

    public FollowUpTypeDto updateFollowUpType(Long id, FollowUpTypeDto followUpTypeDto) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Follow-up type ID cannot be null");
            FollowUpType existingFollowUpType = followUpTypeService.getById(id);
            followUpTypeDto.setId(id);
            FollowUpTypeMapper.INSTANCE.updateEntityFromDto(followUpTypeDto, existingFollowUpType);
            FollowUpType updatedFollowUpType = followUpTypeService.save(existingFollowUpType);
            return FollowUpTypeMapper.INSTANCE.followUpTypeToFollowUpTypeDTO(updatedFollowUpType);
        }, "Error updating follow-up type");
    }

    public void deleteFollowUpType(Long id) {
        validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Follow-up type ID cannot be null");
            followUpTypeService.deleteById(id);
            return null;
        }, "Error deleting follow-up type");
    }
}
