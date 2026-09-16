package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.NoveltyBusiness;
import co.sena.edu.themis.Business.NoveltyTypeBusiness;
import co.sena.edu.themis.Dto.NoveltyDto;
import co.sena.edu.themis.Dto.NoveltyTypeDto;
import co.sena.edu.themis.Dto.NoveltyTypeCountDto;
import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Service.NoveltyReminderScheduler;
import co.sena.edu.themis.Utils.DataConvert;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import java.util.*;
import java.text.Normalizer;

@DgsComponent
public class NoveltyResolver {
    private final NoveltyBusiness noveltyBusiness;
    private final NoveltyTypeBusiness noveltyTypeBusiness;
    private final DataConvert dataConvert;
    private final NoveltySubscription noveltySubscription;
    private final NoveltyReminderScheduler noveltyReminderScheduler;

    public NoveltyResolver(NoveltyBusiness noveltyBusiness,
            NoveltyTypeBusiness noveltyTypeBusiness,
            DataConvert dataConvert,
            NoveltySubscription noveltySubscription,
            NoveltyReminderScheduler noveltyReminderScheduler) {
        this.noveltyBusiness = noveltyBusiness;
        this.noveltyTypeBusiness = noveltyTypeBusiness;
        this.dataConvert = dataConvert;
        this.noveltySubscription = noveltySubscription;
        this.noveltyReminderScheduler = noveltyReminderScheduler;
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR','ADMINISTRADOR','ROLE_INSTRUCTOR','INSTRUCTOR','ROLE_APRENDIZ','APRENDIZ','ROLE_COORDINADOR','COORDINADOR')")
    @DgsQuery
    public Map<String, Object> allNovelties(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            // Extract user info
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            Long userId = Long.valueOf(auth.getName()); // Assuming name is userId as set in JWTFilter
            List<String> roles = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .collect(java.util.stream.Collectors.toList());

            Page<NoveltyDto> noveltyPage = noveltyBusiness.getNoveltiesByUser(userId, roles, page, size);

            return ResponseHttpApi.responseHttpFindAll(
                    noveltyPage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Novelties retrieved successfully",
                    noveltyPage.getTotalPages(),
                    page,
                    (int) noveltyPage.getTotalElements());
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DgsQuery
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR','ADMINISTRADOR','ROLE_COORDINADOR','COORDINADOR','ROLE_INSTRUCTOR','INSTRUCTOR','ROLE_APRENDIZ','APRENDIZ') or isAuthenticated()")
    public NoveltyDto noveltyById(@InputArgument("id") Long id) {
        return noveltyBusiness.noveltyById(id);
    }

    @DgsMutation
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR','ADMINISTRADOR','ROLE_COORDINADOR','COORDINADOR','ROLE_INSTRUCTOR','INSTRUCTOR','ROLE_APRENDIZ','APRENDIZ') or isAuthenticated()")
    public Map<String, Object> addNovelty(@InputArgument("input") @Valid NoveltyDto input) {
        try {
            // Authorization by novelty type: ADMINISTRADOR can add any; INSTRUCTOR only 'desercion'; APRENDIZ only 'retiro', 'aplazamiento', 'reintegro'
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            java.util.Set<String> authorities = new java.util.HashSet<>();
            if (auth != null) {
                for (org.springframework.security.core.GrantedAuthority g : auth.getAuthorities()) {
                    authorities.add(g.getAuthority().toUpperCase());
                }
            }

            boolean isAdmin = authorities.stream().anyMatch(a -> a.contains("ADMINISTRADOR") || a.contains("SUPERADMIN"));
            boolean isInstructor = authorities.stream().anyMatch(a -> a.contains("INSTRUCTOR") || a.contains("DOCENTE") || a.contains("TEACHER"));
            boolean isApprentice = authorities.stream().anyMatch(a -> a.contains("APRENDIZ") || a.contains("APPRENTICE") || a.contains("STUDENT"));

            String typeName = null;
            if (input != null && input.getNoveltyType() != null) {
                if (input.getNoveltyType().getNameNovelty() != null) {
                    typeName = normalize(input.getNoveltyType().getNameNovelty());
                } else if (input.getNoveltyType().getId() != null) {
                    try {
                        // obtener el nombre del tipo desde la base si el cliente sólo envió id
                        NoveltyTypeDto typeDto = noveltyTypeBusiness.findById(input.getNoveltyType().getId());
                        if (typeDto != null && typeDto.getNameNovelty() != null) typeName = normalize(typeDto.getNameNovelty());
                    } catch (Exception e) {
                        // no block: si no encontramos el tipo, typeName queda null y la validación fallará como antes
                    }
                }
            }

            if (!isAdmin) {
                if (isInstructor) {
                    if (typeName == null || !typeName.contains("deserc")) {
                        return ResponseHttpApi.responseHttpError("Acceso denegado: el rol INSTRUCTOR sólo puede registrar novedades de deserción", org.springframework.http.HttpStatus.FORBIDDEN);
                    }
                } else if (isApprentice) {
                    if (typeName == null || !(typeName.contains("retiro") || typeName.contains("aplaz") || typeName.contains("reinteg"))) {
                        return ResponseHttpApi.responseHttpError("Acceso denegado: el rol APRENDIZ sólo puede registrar retiro voluntario, aplazamiento o reintegro", org.springframework.http.HttpStatus.FORBIDDEN);
                    }
                    // Si es aprendiz y no envió studentId en el input, asignarlo desde el token (auth.getName())
                    try {
                        if (input.getStudentId() == null && auth != null && auth.getName() != null) {
                            Long userId = Long.valueOf(auth.getName());
                            input.setStudentId(userId);
                        }
                    } catch (Exception e) {
                        // no bloquear: si no se puede parsear el userId dejaremos que falle más adelante con mensaje claro
                    }
                    // Si después de intentar inferir studentId sigue siendo null, devolver error claro
                    if (input.getStudentId() == null) {
                        try { System.out.println("addNovelty: cannot infer studentId from token; auth.getName()=" + (auth != null ? auth.getName() : "null")); } catch (Exception ignored) {}
                        return ResponseHttpApi.responseHttpError("Falta studentId en el input y no se pudo inferir desde el token", org.springframework.http.HttpStatus.BAD_REQUEST);
                    }
                } else {
                    return ResponseHttpApi.responseHttpError("Acceso denegado: rol no autorizado para registrar novedades", org.springframework.http.HttpStatus.FORBIDDEN);
                }
            }

            System.out.println("Adding novelty: " + input);
            NoveltyDto novelty = noveltyBusiness.addNovelty(input);
            return ResponseHttpApi.responseHttpAction(
                    novelty.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Novelty added successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DgsMutation
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    public Map<String, Object> updateNovelty(@InputArgument("id") Long id,
            @InputArgument("input") @Valid NoveltyDto input) {
        try {
            NoveltyDto novelty = noveltyBusiness.updateNovelty(id, input);
            return ResponseHttpApi.responseHttpAction(
                    novelty.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Novelty updated successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DgsMutation
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    public Map<String, Object> deleteNovelty(@InputArgument("id") Long id) {
        try {
            noveltyBusiness.deleteNovelty(id);
            return ResponseHttpApi.responseHttpAction(
                    null,
                    ResponseHttpApi.CODE_OK,
                    "Novelty deleted successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DgsMutation
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    public Map<String, Object> sendPendingNoveltyReminders() {
        try {
            noveltyReminderScheduler.sendPendingNoveltyReminders();
            return ResponseHttpApi.responseHttpAction(
                    null,
                    ResponseHttpApi.CODE_OK,
                    "Pending novelty reminders triggered");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DgsMutation
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    public Map<String, Object> returnNovelty(@InputArgument("id") Long id,
            @InputArgument("observation") String observation) {
        try {
            NoveltyDto result = noveltyBusiness.returnNovelty(id, observation);
            return ResponseHttpApi.responseHttpAction(
                    result != null ? result.getId() : null,
                    ResponseHttpApi.CODE_OK,
                    "Novelty returned successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Normaliza nombre: quita acentos, pasa a minúsculas
    private String normalize(String s) {
        if (s == null) return null;
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase().trim();
    }

}
