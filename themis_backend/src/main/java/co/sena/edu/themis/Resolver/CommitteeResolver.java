package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.CommitteeBusiness;
import co.sena.edu.themis.Dto.CommitteeDto;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Map;

@DgsComponent
public class CommitteeResolver {
    private final CommitteeBusiness committeeBusiness;

    public CommitteeResolver(CommitteeBusiness committeeBusiness) {
        this.committeeBusiness = committeeBusiness;
    }

    // Query methods
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsQuery
    public Map<String, Object> allCommittees(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            Page<CommitteeDto> committeePage = committeeBusiness.allCommittees(page, size);
            return ResponseHttpApi.responseHttpFindAll(
                    committeePage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Committees retrieved successfully",
                    committeePage.getTotalPages(),
                    page,
                    (int) committeePage.getTotalElements());
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> committeeById(@InputArgument Long id) {
        try {
            CommitteeDto committee = committeeBusiness.committeeById(id);
            return ResponseHttpApi.responseHttpFindId(
                    committee,
                    ResponseHttpApi.CODE_OK,
                    "Committee retrieved successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> committeesByStudentId(@InputArgument Long studentId) {
        try {
            List<CommitteeDto> committees = committeeBusiness.findCommitteesByStudentId(studentId);
            return ResponseHttpApi.responseHttpFindAllList(
                    committees,
                    ResponseHttpApi.CODE_OK,
                    "Committees by student retrieved successfully",
                    committees.size());
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> activeCommittees() {
        try {
            List<CommitteeDto> committees = committeeBusiness.findActiveCommittees();
            return ResponseHttpApi.responseHttpFindAllList(
                    committees,
                    ResponseHttpApi.CODE_OK,
                    "Active committees retrieved successfully",
                    committees.size());
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Mutation methods
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> addCommittee(@InputArgument CommitteeDto input) {
        try {
            CommitteeDto savedCommittee = committeeBusiness.addCommittee(input);
            return ResponseHttpApi.responseHttpAction(
                    savedCommittee.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Committee created successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> updateCommittee(@InputArgument Long id, @InputArgument CommitteeDto input) {
        try {
            CommitteeDto updatedCommittee = committeeBusiness.updateCommittee(id, input);
            return ResponseHttpApi.responseHttpAction(
                    updatedCommittee.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Committee updated successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> deleteCommittee(@InputArgument Long id) {
        try {
            committeeBusiness.deleteCommittee(id);
            return ResponseHttpApi.responseHttpAction(
                    id,
                    ResponseHttpApi.CODE_OK,
                    "Committee deleted successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> addPersonToCommittee(@InputArgument Long committeeId, @InputArgument Long personId,
            @InputArgument String role) {
        try {
            CommitteeDto updatedCommittee = committeeBusiness.addPersonToCommittee(committeeId, personId, role);
            return ResponseHttpApi.responseHttpAction(
                    updatedCommittee.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Person added to committee successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> removePersonFromCommittee(@InputArgument Long committeeId, @InputArgument Long personId,
            @InputArgument String role) {
        try {
            CommitteeDto updatedCommittee = committeeBusiness.removePersonFromCommittee(committeeId, personId, role);
            return ResponseHttpApi.responseHttpAction(
                    updatedCommittee.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Person removed from committee successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
