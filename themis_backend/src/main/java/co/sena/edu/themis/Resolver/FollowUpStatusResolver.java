package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.FollowUpStatusBusiness;
import co.sena.edu.themis.Dto.FollowUpStatusDto;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@DgsComponent
public class FollowUpStatusResolver {
    private final FollowUpStatusBusiness followUpStatusBusiness;

    public FollowUpStatusResolver(FollowUpStatusBusiness followUpStatusBusiness) {
        this.followUpStatusBusiness = followUpStatusBusiness;
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> allFollowUpStatuses(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            Page<FollowUpStatusDto> followUpStatusPage = followUpStatusBusiness.findAll(page, size);
            return ResponseHttpApi.responseHttpFindAll(
                    followUpStatusPage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up statuses retrieved successfully",
                    followUpStatusPage.getTotalPages(),
                    page,
                    (int) followUpStatusPage.getTotalElements()
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
    public FollowUpStatusDto followUpStatusById(@InputArgument("id") Long id) {
        return followUpStatusBusiness.followUpStatusById(id);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> addFollowUpStatus(@InputArgument("input") FollowUpStatusDto input) {
        try {
            FollowUpStatusDto followUpStatus = followUpStatusBusiness.addFollowUpStatus(input);
            return ResponseHttpApi.responseHttpAction(
                    followUpStatus.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up status created successfully"
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
