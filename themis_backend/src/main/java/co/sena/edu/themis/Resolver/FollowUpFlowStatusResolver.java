package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.FollowUpFlowStatusBusiness;
import co.sena.edu.themis.Dto.FollowUpFlowStatusDto;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@DgsComponent
public class FollowUpFlowStatusResolver {
    private final FollowUpFlowStatusBusiness followUpFlowStatusBusiness;

    public FollowUpFlowStatusResolver(FollowUpFlowStatusBusiness followUpFlowStatusBusiness) {
        this.followUpFlowStatusBusiness = followUpFlowStatusBusiness;
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> allFollowUpFlowStatuses(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            Page<FollowUpFlowStatusDto> followUpFlowStatusPage = followUpFlowStatusBusiness.findAll(page, size);
            return ResponseHttpApi.responseHttpFindAll(
                    followUpFlowStatusPage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up flow statuses retrieved successfully",
                    followUpFlowStatusPage.getTotalPages(),
                    page,
                    (int) followUpFlowStatusPage.getTotalElements()
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public FollowUpFlowStatusDto followUpFlowStatusById(@InputArgument("id") Long id) {
        return followUpFlowStatusBusiness.followUpFlowStatusById(id);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> addFollowUpFlowStatus(@InputArgument("input") FollowUpFlowStatusDto input) {
        try {
            FollowUpFlowStatusDto followUpFlowStatus = followUpFlowStatusBusiness.addFollowUpFlowStatus(input);
            return ResponseHttpApi.responseHttpAction(
                    followUpFlowStatus.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up flow status created successfully"
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
