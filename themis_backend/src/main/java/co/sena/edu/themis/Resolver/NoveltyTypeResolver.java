package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.NoveltyTypeBusiness;
import co.sena.edu.themis.Dto.NoveltyTypeDto;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@DgsComponent
public class NoveltyTypeResolver {

    private final NoveltyTypeBusiness noveltyTypeBusiness;

    public NoveltyTypeResolver(NoveltyTypeBusiness noveltyTypeBusiness) {
        this.noveltyTypeBusiness = noveltyTypeBusiness;
    }

//    @DgsData(parentType = "NoveltyType")
//    public List<Map<String, Object>> role(DgsDataFetchingEnvironment env) {
//        NoveltyTypeDto noveltyTypeDto = env.getSource();
//        assert noveltyTypeDto != null;
//
//        return noveltyTypeDto.getExternalRoleIds().stream()
//                .map(roleId -> {
//                    Map<String, Object> reference = new HashMap<>();
//                    reference.put("__typename", "User");
//                    reference.put("id", roleId);
//                    return reference;
//                })
//                .collect(Collectors.toList());
//    }


    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR','ADMINISTRADOR','ROLE_COORDINADOR','COORDINADOR','ROLE_APRENDIZ','APRENDIZ','ROLE_INSTRUCTOR','INSTRUCTOR')")
    @DgsQuery
    public Map<String, Object> allNoveltyTypes(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            Page<NoveltyTypeDto> noveltyTypesPage = noveltyTypeBusiness.findAll(page, size);
            return ResponseHttpApi.responseHttpFindAll(
                    noveltyTypesPage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Novelty Types retrieved successfully",
                    noveltyTypesPage.getTotalPages(),
                    page,
                    (int) noveltyTypesPage.getTotalElements()
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
    public Map<String, Object> addNoveltyType(@InputArgument(name = "input") NoveltyTypeDto noveltyTypeDto) {
        try {
            NoveltyTypeDto noveltyTypeCreated = noveltyTypeBusiness.createNoveltyType(noveltyTypeDto);
            return ResponseHttpApi.responseHttpAction(
                    noveltyTypeCreated.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Novelty Type created successfully"
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
    public Map<String, Object> updateNoveltyType(@InputArgument Long id, @InputArgument(name = "input") NoveltyTypeDto noveltyTypeDto) {
        try {
            noveltyTypeDto.setId(id);
            noveltyTypeBusiness.updateNoveltyType(noveltyTypeDto);
            return ResponseHttpApi.responseHttpAction(
                    id,
                    ResponseHttpApi.CODE_OK,
                    "Novelty Type updated successfully"
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
    public Map<String, Object> deleteNoveltyType(@InputArgument Long id) {
        try {
            noveltyTypeBusiness.deleteNoveltyTypeById(id);
            return ResponseHttpApi.responseHttpAction(
                    id,
                    ResponseHttpApi.CODE_OK,
                    "Novelty Type deleted successfully"
            );
        } catch (Exception e ) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


}
