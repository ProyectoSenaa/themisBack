package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.FollowUpFlowStatusDto;
import co.sena.edu.themis.Entity.FollowUpFlowStatus;
import co.sena.edu.themis.Service.FollowUpFlowStatusService;
import co.sena.edu.themis.Utils.mapper.FollowUpFlowStatusMapper;
import co.sena.edu.themis.Utils.validation.ValidationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class FollowUpFlowStatusBusiness {
    private final FollowUpFlowStatusService followUpFlowStatusService;
    private final ValidationUtils validationUtils;
    private final FollowUpFlowStatusMapper followUpFlowStatusMapper;

    public FollowUpFlowStatusBusiness(
            FollowUpFlowStatusService followUpFlowStatusService,
            ValidationUtils validationUtils,
            FollowUpFlowStatusMapper followUpFlowStatusMapper) {
        this.followUpFlowStatusService = followUpFlowStatusService;
        this.validationUtils = validationUtils;
        this.followUpFlowStatusMapper = followUpFlowStatusMapper;
    }

    public Page<FollowUpFlowStatusDto> findAll(int page, int size) {
        return validationUtils.tryExecute(() -> {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<FollowUpFlowStatus> followUpFlowStatusPage = followUpFlowStatusService.findAll(pageRequest);
            return followUpFlowStatusPage.map(followUpFlowStatusMapper::toDto);
        }, "Error retrieving follow-up flow statuses");
    }

    public FollowUpFlowStatusDto followUpFlowStatusById(Long id) {
        return validationUtils.tryExecute(() -> {
            validationUtils.validateNotNull(id, "Follow-up flow status ID cannot be null");
            FollowUpFlowStatus followUpFlowStatus = followUpFlowStatusService.getById(id);
            return followUpFlowStatusMapper.toDto(followUpFlowStatus);
        }, "Error retrieving follow-up flow status by ID");
    }

    public FollowUpFlowStatusDto addFollowUpFlowStatus(FollowUpFlowStatusDto followUpFlowStatusDto) {
        return validationUtils.tryExecute(() -> {
            FollowUpFlowStatus followUpFlowStatus = followUpFlowStatusMapper.toEntity(followUpFlowStatusDto);
            FollowUpFlowStatus savedFollowUpFlowStatus = followUpFlowStatusService.save(followUpFlowStatus);
            return followUpFlowStatusMapper.toDto(savedFollowUpFlowStatus);
        }, "Error adding follow-up flow status");
    }
}
