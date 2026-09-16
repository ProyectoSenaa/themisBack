package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.FollowUpStatusDto;
import co.sena.edu.themis.Entity.FollowUpStatus;
import co.sena.edu.themis.Service.FollowUpStatusService;
import co.sena.edu.themis.Utils.mapper.FollowUpStatusMapper;
import co.sena.edu.themis.Utils.validation.ValidationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class FollowUpStatusBusiness {
    private final FollowUpStatusService followUpStatusService;
    private final ValidationUtils validationUtils;
    private final FollowUpStatusMapper followUpStatusMapper;

    public FollowUpStatusBusiness(
            FollowUpStatusService followUpStatusService,
            ValidationUtils validationUtils,
            FollowUpStatusMapper followUpStatusMapper) {
        this.followUpStatusService = followUpStatusService;
        this.validationUtils = validationUtils;
        this.followUpStatusMapper = followUpStatusMapper;
    }

    public Page<FollowUpStatusDto> findAll(int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<FollowUpStatus> followUpStatusPage = followUpStatusService.findAll(pageRequest);
            return followUpStatusPage.map(followUpStatusMapper::toDto);
        }, "Error retrieving follow-up statuses");
    }

    public FollowUpStatusDto followUpStatusById(Long id) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Follow-up status ID cannot be null");
            FollowUpStatus followUpStatus = followUpStatusService.getById(id);
            return followUpStatusMapper.toDto(followUpStatus);
        }, "Error retrieving follow-up status by ID");
    }

    public FollowUpStatusDto addFollowUpStatus(FollowUpStatusDto followUpStatusDto) {
        return validationUtils.tryExecute(() -> {
            FollowUpStatus followUpStatus = followUpStatusMapper.toEntity(followUpStatusDto);
            FollowUpStatus savedFollowUpStatus = followUpStatusService.save(followUpStatus);
            return followUpStatusMapper.toDto(savedFollowUpStatus);
        }, "Error adding follow-up status");
    }
}
