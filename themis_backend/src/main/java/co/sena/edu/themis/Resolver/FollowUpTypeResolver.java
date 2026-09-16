package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.FollowUpTypeBusiness;
import co.sena.edu.themis.Dto.FollowUpTypeDto;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@DgsComponent
public class FollowUpTypeResolver {
    private final FollowUpTypeBusiness followUpTypeBusiness;

    public FollowUpTypeResolver(FollowUpTypeBusiness followUpTypeBusiness) {
        this.followUpTypeBusiness = followUpTypeBusiness;
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> allFollowUpTypes(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            Page<FollowUpTypeDto> followUpTypePage = followUpTypeBusiness.findAll(page, size);
            return ResponseHttpApi.responseHttpFindAll(
                    followUpTypePage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up types retrieved successfully",
                    followUpTypePage.getTotalPages(),
                    page,
                    (int) followUpTypePage.getTotalElements()
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
    public FollowUpTypeDto followUpTypeById(@InputArgument("id") Long id) {
        return followUpTypeBusiness.followUpTypeById(id);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> addFollowUpType(@InputArgument("input") FollowUpTypeDto input) {
        try {
            FollowUpTypeDto followUpType = followUpTypeBusiness.addFollowUpType(input);
            return ResponseHttpApi.responseHttpAction(
                    followUpType.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up type created successfully"
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> updateFollowUpType(@InputArgument("input") FollowUpTypeDto input) {
        try {
            FollowUpTypeDto followUpType = followUpTypeBusiness.updateFollowUpType(input.getId(), input);
            return ResponseHttpApi.responseHttpAction(
                    followUpType.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up type updated successfully"
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> deleteFollowUpType(@InputArgument("id") Long id) {
        try {
            followUpTypeBusiness.deleteFollowUpType(id);
            return ResponseHttpApi.responseHttpAction(
                    id,
                    ResponseHttpApi.CODE_OK,
                    "Follow-up type deleted successfully"
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
