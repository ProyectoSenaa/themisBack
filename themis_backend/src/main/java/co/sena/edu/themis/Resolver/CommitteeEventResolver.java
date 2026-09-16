package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.CommitteeEventBusiness;
import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@DgsComponent
public class CommitteeEventResolver {

    private final CommitteeEventBusiness committeeEventBusiness;

    public CommitteeEventResolver(CommitteeEventBusiness committeeEventBusiness) {
        this.committeeEventBusiness = committeeEventBusiness;
    }

    // Queries
    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> allCommitteeEvents(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            int p = page == null ? 0 : page;
            int s = size == null ? 10 : size;
            PageRequest pageRequest = PageRequest.of(p, s);
            Page<CommitteeEventDto> events = committeeEventBusiness.allEvents(pageRequest.getPageNumber(), pageRequest.getPageSize());

            return ResponseHttpApi.responseHttpFindAll(
                    events.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Committee events retrieved successfully",
                    events.getTotalPages(),
                    p,
                    (int) events.getTotalElements()
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
    public Map<String, Object> committeeEventById(@InputArgument Long id) {
        try {
            CommitteeEventDto event = committeeEventBusiness.eventById(id);
            return ResponseHttpApi.responseHttpFindId(
                    event,
                    ResponseHttpApi.CODE_OK,
                    "Committee event retrieved successfully"
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Mutations
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> addCommitteeEvent(@InputArgument("input") CommitteeEventDto input) {
        try {
            CommitteeEventDto saved = committeeEventBusiness.addEvent(input);
            return ResponseHttpApi.responseHttpAction(
                    saved.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Committee event added successfully"
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
    public Map<String, Object> addCommitteeEventsBulk(@InputArgument("input") List<CommitteeEventDto> input) {
        try {
            List<CommitteeEventDto> createdEvents = committeeEventBusiness.addEventsBulkWithReturn(input);
            return ResponseHttpApi.responseHttpFindAll(
                    createdEvents,
                    ResponseHttpApi.CODE_OK,
                    "Committee events bulk added successfully",
                    1, // totalPages
                    0, // currentPage
                    createdEvents.size() // totalItems
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
    public Map<String, Object> updateCommitteeEvent(@InputArgument("id") Long id,
                                                    @InputArgument("input") CommitteeEventDto input) {
        try {
            CommitteeEventDto updated = committeeEventBusiness.updateEvent(id, input);
            return ResponseHttpApi.responseHttpAction(
                    updated.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Committee event updated successfully"
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
    public Map<String, Object> deleteCommitteeEvent(@InputArgument("id") Long id) {
        try {
            committeeEventBusiness.deleteEvent(id);
            return ResponseHttpApi.responseHttpAction(
                    id,
                    ResponseHttpApi.CODE_OK,
                    "Committee event deleted successfully"
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
    public Map<String, Object> finalizeCommitteeEvent(@InputArgument("eventId") Long eventId,
                                                      @InputArgument("responses") List<Map<String, Object>> responses) {
        try {
            int updated = committeeEventBusiness.finalizeCommitteeEvent(eventId, responses);
            return ResponseHttpApi.responseHttpAction(
                    (long) updated,
                    ResponseHttpApi.CODE_OK,
                    "Comité finalizado. Observaciones aplicadas: " + updated
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
    public Map<String, Object> assignCommitteeToEvents(@InputArgument("committeeId") Long committeeId,
                                                       @InputArgument("eventIds") List<Long> eventIds) {
        try {
            List<CommitteeEventDto> updatedEvents = committeeEventBusiness.assignCommitteeToEvents(committeeId, eventIds);

            return ResponseHttpApi.responseHttpFindAll(
                    updatedEvents,
                    ResponseHttpApi.CODE_OK,
                    "Comité asignado a " + updatedEvents.size() + " eventos exitosamente",
                    1, // totalPages
                    0, // currentPage
                    updatedEvents.size() // totalItems
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
    public Map<String, Object> finalizeCommitteeEventOnly(@InputArgument("eventId") Long eventId) {
        try {
            committeeEventBusiness.finalizeCommitteeEventOnly(eventId);
            return ResponseHttpApi.responseHttpAction(
                    eventId,
                    ResponseHttpApi.CODE_OK,
                    "Evento de comité finalizado exitosamente"
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
    public Map<String, Object> respondNoveltiesFromCommittee(@InputArgument("committeeId") Long committeeId,
                                                             @InputArgument("responses") List<Map<String, Object>> responses) {
        try {
            int updated = committeeEventBusiness.respondNoveltiesFromCommittee(committeeId, responses);
            return ResponseHttpApi.responseHttpAction(
                    (long) updated,
                    ResponseHttpApi.CODE_OK,
                    "Respuestas de novedades procesadas. Novedades actualizadas: " + updated
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}