package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.FollowUpBusiness;
import co.sena.edu.themis.Business.FollowUpTypeBusiness;
import co.sena.edu.themis.Dto.FollowUpDto;
import co.sena.edu.themis.Dto.FollowUpTypeDto;
import co.sena.edu.themis.Service.PersonGrpcService;
import co.sena.edu.themis.Service.StudentGrpcService;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * FollowUpResolver reorganizado con la misma estructura que NoveltyResolver
 * - Mantiene TODA la lógica original (prefetch, caches, mapeo, followUpType resolution, gRPC helpers)
 * - Reagrupado: campos/constructor -> Queries -> Mutations -> Helpers (caches & gRPC)
 */
@DgsComponent
public class FollowUpResolver {

    private final FollowUpBusiness followUpBusiness;
    private final FollowUpTypeBusiness followUpTypeBusiness;
    private final StudentGrpcService studentGrpcService;
    private final PersonGrpcService personGrpcService;
    private static final Logger logger = LoggerFactory.getLogger(FollowUpResolver.class);

    // CACHES y control de concurrencia para gRPC
    private static final java.util.concurrent.ConcurrentHashMap<Long, StudentCacheEntry> STUDENT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Long, PersonCacheEntry> PERSON_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.Semaphore GRPC_SEMAPHORE = new java.util.concurrent.Semaphore(50);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final long NEGATIVE_TTL_MS = 60 * 1000L;   // 1 minute

    private static class StudentCacheEntry {
        public co.sena.edu.olympo_back.Student.StudentResponse resp;
        public long ts;
        public boolean found;
    }

    private static class PersonCacheEntry {
        public co.sena.edu.olympo_back.proto.PersonResponse resp;
        public long ts;
        public boolean found;
    }

    public FollowUpResolver(FollowUpBusiness followUpBusiness,
                            FollowUpTypeBusiness followUpTypeBusiness,
                            StudentGrpcService studentGrpcService,
                            PersonGrpcService personGrpcService) {
        this.followUpBusiness = followUpBusiness;
        this.followUpTypeBusiness = followUpTypeBusiness;
        this.studentGrpcService = studentGrpcService;
        this.personGrpcService = personGrpcService;
    }

    // ============================
    // QUERIES
    // ============================

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR','ADMINISTRADOR','ROLE_COORDINADOR','COORDINADOR','ROLE_INSTRUCTOR','INSTRUCTOR','ROLE_APRENDIZ','APRENDIZ')")
    @DgsQuery
    public Map<String, Object> allFollowUps(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            Page<FollowUpDto> followUpPage = followUpBusiness.allFollowUps(page, size);
            return ResponseHttpApi.responseHttpFindAll(
                    followUpPage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-ups retrieved successfully",
                    followUpPage.getTotalPages(),
                    page,
                    (int) followUpPage.getTotalElements());
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR','ADMINISTRADOR','ROLE_COORDINADOR','COORDINADOR','ROLE_INSTRUCTOR','INSTRUCTOR','ROLE_APRENDIZ','APRENDIZ') or isAuthenticated()")
    @DgsQuery
    public FollowUpDto followUpById(@InputArgument("id") Long id) {
        return followUpBusiness.followUpById(id);
    }

    /**
     * followUpsByStudent - query paginada que devuelve una página con maps (cada map es un FollowUp 'serializado')
     * Mantiene la lógica original: prefetch gRPC para students/persons, mapeo de followUpType con fallback, etc.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_INSTRUCTOR') or hasAuthority('INSTRUCTOR') or hasAuthority('ROLE_APRENDIZ') or hasAuthority('APRENDIZ') or (#studentId != null and authentication.name == #studentId.toString()) or isAuthenticated()")
    @DgsQuery
    public Map<String, Object> followUpsByStudent(
            @InputArgument("studentId") Long studentId,
            @InputArgument Integer page,
            @InputArgument Integer size) {
        try {
            int pageVal = (page == null) ? 0 : page;
            int sizeVal = (size == null) ? 10 : size;

            Page<Map<String, Object>> followUpPage;
            List<Map<String, Object>> mapList = new ArrayList<>();

            java.util.Map<Long, Object> studentCache = new java.util.HashMap<>();
            java.util.Map<Long, Object> personCache = new java.util.HashMap<>();

            Consumer<FollowUpDto> dtoToMap = (dto) -> {
                Map<String, Object> elem = new java.util.HashMap<>();
                elem.put("__typename", "FollowUp");
                // Alineamos con el tipo GraphQL ID -> enviar como String
                elem.put("id", dto.getId() != null ? String.valueOf(dto.getId()) : null);
                elem.put("creationDate", dto.getCreationDate());
                elem.put("caseDescription", dto.getCaseDescription());
                elem.put("evidenceFiles", dto.getEvidenceFiles());
                elem.put("isActive", dto.getIsActive());

                elem.put("studentId", dto.getStudentId());
                elem.put("teacherId", dto.getTeacherId());
                elem.put("coordinatorId", dto.getCoordinatorId());
                elem.put("studySheetId", dto.getStudySheetId());

                if (dto.getStudentId() != null) {
                    Map<String, Object> s = new java.util.HashMap<>();
                    s.put("__typename", "Student");
                    s.put("id", String.valueOf(dto.getStudentId()));
                    // Añadir campos fallback para evitar 'undefined' en UI si la federación falla
                    String fallback = "No especificado";
                    // Si el prefetch trajo información del estudiante, usarla
                    Object pref = studentCache.get(dto.getStudentId());
                    if (pref instanceof co.sena.edu.olympo_back.Student.StudentResponse) {
                        co.sena.edu.olympo_back.Student.StudentResponse sr = (co.sena.edu.olympo_back.Student.StudentResponse) pref;
                        if (sr != null && sr.getFound() && sr.getPerson() != null && sr.getPerson().getFound()) {
                            var p = sr.getPerson();
                            String name = p.getName() != null && !p.getName().isBlank() ? p.getName() : fallback;
                            String lastname = p.getLastname() != null && !p.getLastname().isBlank() ? p.getLastname() : fallback;
                            s.put("name", name + " " + lastname);
                            s.put("fullName", name + " " + lastname);
                            s.put("firstname", name);
                            s.put("lastname", lastname);
                            Map<String, Object> personMap = new java.util.HashMap<>();
                            personMap.put("__typename", "Person");
                            personMap.put("id", String.valueOf(p.getPersonId()));
                            personMap.put("document", p.getDocument());
                            personMap.put("name", name);
                            personMap.put("lastname", lastname);
                            personMap.put("phone", p.getPhone() != null ? p.getPhone() : fallback);
                            personMap.put("email", p.getEmail() != null ? p.getEmail() : fallback);
                            personMap.put("address", p.getAddress() != null ? p.getAddress() : fallback);
                            s.put("person", personMap);
                            // También rellenar el campo top-level studentName para la UI
                            elem.put("studentName", name + " " + lastname);
                        } else {
                            // fallback
                            s.put("name", fallback);
                            s.put("fullName", fallback);
                            s.put("firstname", fallback);
                            s.put("lastname", fallback);
                            Map<String, Object> personMap = new java.util.HashMap<>();
                            personMap.put("__typename", "Person");
                            personMap.put("id", null);
                            personMap.put("document", fallback);
                            personMap.put("name", fallback);
                            personMap.put("lastname", fallback);
                            personMap.put("phone", fallback);
                            personMap.put("email", fallback);
                            personMap.put("address", fallback);
                            s.put("person", personMap);
                            elem.put("studentName", fallback);
                        }
                    } else {
                        // No prefetched info: fallback
                        s.put("name", fallback);
                        s.put("fullName", fallback);
                        s.put("firstname", fallback);
                        s.put("lastname", fallback);
                        Map<String, Object> personMap = new java.util.HashMap<>();
                        personMap.put("__typename", "Person");
                        personMap.put("id", null);
                        personMap.put("document", fallback);
                        personMap.put("name", fallback);
                        personMap.put("lastname", fallback);
                        personMap.put("phone", fallback);
                        personMap.put("email", fallback);
                        personMap.put("address", fallback);
                        s.put("person", personMap);
                        elem.put("studentName", fallback);
                    }
                    elem.put("student", s);
                } else {
                    // No studentId available — ensure studentName fallback and provide a student object with fallback fields
                    String fallback = "No especificado";
                    elem.put("studentName", fallback);
                    Map<String, Object> s = new java.util.HashMap<>();
                    s.put("__typename", "Student");
                    s.put("id", null);
                    s.put("name", fallback);
                    s.put("fullName", fallback);
                    s.put("firstname", fallback);
                    s.put("lastname", fallback);
                    // also include a person fallback to match allStudents shape
                    Map<String, Object> personMap = new java.util.HashMap<>();
                    personMap.put("__typename", "Person");
                    personMap.put("id", null);
                    personMap.put("document", fallback);
                    personMap.put("name", fallback);
                    personMap.put("lastname", fallback);
                    personMap.put("phone", fallback);
                    personMap.put("email", fallback);
                    personMap.put("address", fallback);
                    s.put("person", personMap);
                    elem.put("student", s);
                }

                if (dto.getTeacherId() != null) {
                    // Añadir mapeo con campos fallback para evitar 'undefined' si la federación falla
                    Map<String, Object> t = new java.util.HashMap<>();
                    t.put("__typename", "Teacher");
                    t.put("id", String.valueOf(dto.getTeacherId()));
                    String fallbackT = "No especificado";
                    // usar personCache si está prefetched
                    Object pfx = personCache.get(dto.getTeacherId());
                    if (pfx instanceof co.sena.edu.olympo_back.proto.PersonResponse) {
                        co.sena.edu.olympo_back.proto.PersonResponse pr = (co.sena.edu.olympo_back.proto.PersonResponse) pfx;
                        if (pr != null && pr.getFound()) {
                            String name = pr.getName() != null && !pr.getName().isBlank() ? pr.getName() : fallbackT;
                            String lastname = pr.getLastname() != null && !pr.getLastname().isBlank() ? pr.getLastname() : fallbackT;
                            t.put("name", name + " " + lastname);
                            java.util.Map<String,Object> person = new java.util.HashMap<>();
                            person.put("__typename","Person");
                            person.put("id", String.valueOf(pr.getPersonId()));
                            person.put("document", pr.getDocument());
                            person.put("name", name);
                            person.put("lastname", lastname);
                            person.put("phone", pr.getPhone() != null ? pr.getPhone() : fallbackT);
                            person.put("email", pr.getEmail() != null ? pr.getEmail() : fallbackT);
                            person.put("address", pr.getAddress() != null ? pr.getAddress() : fallbackT);
                            t.put("person", person);
                            // Rellenar campo top-level teacherName
                            elem.put("teacherName", name + " " + lastname);
                        } else {
                            t.put("name", fallbackT);
                            java.util.Map<String,Object> person = new java.util.HashMap<>();
                            person.put("__typename","Person");
                            person.put("id", null);
                            person.put("document", fallbackT);
                            person.put("name", fallbackT);
                            person.put("lastname", fallbackT);
                            person.put("phone", fallbackT);
                            person.put("email", fallbackT);
                            person.put("address", fallbackT);
                            t.put("person", person);
                            elem.put("teacherName", fallbackT);
                        }
                    } else {
                        t.put("name", fallbackT);
                        java.util.Map<String,Object> person = new java.util.HashMap<>();
                        person.put("__typename","Person");
                        person.put("id", null);
                        person.put("document", fallbackT);
                        person.put("name", fallbackT);
                        person.put("lastname", fallbackT);
                        person.put("phone", fallbackT);
                        person.put("email", fallbackT);
                        person.put("address", fallbackT);
                        t.put("person", person);
                        elem.put("teacherName", fallbackT);
                    }
                    elem.put("teacher", t);
                }

                if (dto.getFollowUpType() != null) {
                    Map<String, Object> typeMap = new java.util.HashMap<>();
                    typeMap.put("__typename", "FollowUpType");
                    typeMap.put("id", dto.getFollowUpType().getId());
                    String typeName = dto.getFollowUpType().getName();
                    Long lookupId = dto.getFollowUpTypeId() != null ? dto.getFollowUpTypeId() : dto.getFollowUpType().getId();
                    if ((typeName == null || typeName.trim().isEmpty()) && lookupId != null) {
                        try {
                            FollowUpTypeDto typeDto = followUpTypeBusiness.followUpTypeById(lookupId);
                            if (typeDto != null && typeDto.getName() != null) typeName = typeDto.getName();
                        } catch (Exception e) {
                            logger.debug("Could not fetch FollowUpType name for id={} from existing object: {}", lookupId, e.getMessage());
                        }
                    }
                    String displayName = (typeName != null && !typeName.trim().isEmpty()) ? typeName : "No especificado";
                    typeMap.put("name", displayName);
                    logger.debug("followUpsByStudent - resolved followUpType for dto.id={} typeId={} name={}", dto.getId(), dto.getFollowUpTypeId(), displayName);
                    elem.put("followUpType", typeMap);
                } else if (dto.getFollowUpTypeId() != null) {
                    Map<String, Object> typeMap = new java.util.HashMap<>();
                    typeMap.put("__typename", "FollowUpType");
                    typeMap.put("id", String.valueOf(dto.getFollowUpTypeId()));
                    try {
                        FollowUpTypeDto typeDto = followUpTypeBusiness.followUpTypeById(dto.getFollowUpTypeId());
                        if (typeDto != null && typeDto.getName() != null) {
                            typeMap.put("name", typeDto.getName());
                        }
                        String fetchedName = typeMap.get("name") != null ? (String) typeMap.get("name") : "No especificado";
                        logger.debug("followUpsByStudent - fetched followUpTypeById for dto.id={} typeId={} name={}", dto.getId(), dto.getFollowUpTypeId(), fetchedName);
                    } catch (Exception e) {
                        logger.debug("Could not fetch FollowUpType name for id={}: {}", dto.getFollowUpTypeId(), e.getMessage());
                    }
                    if (!typeMap.containsKey("name")) typeMap.put("name", "No especificado");
                    elem.put("followUpType", typeMap);
                }

                elem.put("followUpStatus", dto.getFollowUpStatus());
                elem.put("followUpFlowStatus", dto.getFollowUpFlowStatus());
                elem.put("minute", dto.getMinute());
                elem.put("improvementPlanFiles", dto.getImprovementPlanFiles());

                mapList.add(elem);
            };

            if (studentId == null) {
                Page<FollowUpDto> pageResult = followUpBusiness.allFollowUps(pageVal, sizeVal);

                // Prefetch unique student and teacher IDs to reduce repeated/serial gRPC calls
                try {
                    Set<Long> sIds = new HashSet<>();
                    Set<Long> tIds = new HashSet<>();
                    for (FollowUpDto dto : pageResult.getContent()) {
                        if (dto.getStudentId() != null) sIds.add(dto.getStudentId());
                        if (dto.getTeacherId() != null) tIds.add(dto.getTeacherId());
                    }
                    java.util.concurrent.ExecutorService prefetchExec = java.util.concurrent.Executors.newFixedThreadPool(Math.min(10, Math.max(1, sIds.size() + tIds.size())));
                    try {
                        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
                        for (Long id : sIds) {
                            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try {
                                    co.sena.edu.olympo_back.Student.StudentResponse r = getStudentResponse(id);
                                    studentCache.put(id, r);
                                } catch (Exception ignored) {
                                }
                            }, prefetchExec));
                        }
                        for (Long id : tIds) {
                            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try {
                                    co.sena.edu.olympo_back.proto.PersonResponse p = getPersonResponse(id);
                                    personCache.put(id, p);
                                } catch (Exception ignored) {
                                }
                            }, prefetchExec));
                        }
                        if (!futures.isEmpty()) {
                            try {
                                java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).get(5, TimeUnit.SECONDS);
                            } catch (Exception ex) {
                                logger.debug("Prefetching student/teacher ids timed out or failed: {}", ex.getMessage());
                            }
                        }
                    } finally {
                        prefetchExec.shutdownNow();
                    }
                } catch (Exception ex) {
                    logger.debug("Error during prefetch setup: {}", ex.getMessage());
                }

                for (FollowUpDto dto : pageResult.getContent()) dtoToMap.accept(dto);
                int total = (int) Math.min(pageResult.getTotalElements(), Integer.MAX_VALUE);
                followUpPage = new PageImpl<>(mapList, PageRequest.of(pageVal, sizeVal), total);
            } else {
                Page<FollowUpDto> pageResult = followUpBusiness.findAllByIdStudentPaginated(studentId, pageVal, sizeVal);

                // Prefetch unique student and teacher IDs for this page
                try {
                    Set<Long> sIds = new HashSet<>();
                    Set<Long> tIds = new HashSet<>();
                    for (FollowUpDto dto : pageResult.getContent()) {
                        if (dto.getStudentId() != null) sIds.add(dto.getStudentId());
                        if (dto.getTeacherId() != null) tIds.add(dto.getTeacherId());
                    }
                    java.util.concurrent.ExecutorService prefetchExec = java.util.concurrent.Executors.newFixedThreadPool(Math.min(10, Math.max(1, sIds.size() + tIds.size())));
                    try {
                        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
                        for (Long id : sIds) {
                            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try {
                                    studentCache.put(id, getStudentResponse(id));
                                } catch (Exception ignored) {
                                }
                            }, prefetchExec));
                        }
                        for (Long id : tIds) {
                            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                                try {
                                    personCache.put(id, getPersonResponse(id));
                                } catch (Exception ignored) {
                                }
                            }, prefetchExec));
                        }
                        if (!futures.isEmpty()) {
                            try {
                                java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).get(5, TimeUnit.SECONDS);
                            } catch (Exception ex) {
                                logger.debug("Prefetching student/teacher ids timed out or failed: {}", ex.getMessage());
                            }
                        }
                    } finally {
                        prefetchExec.shutdownNow();
                    }
                } catch (Exception ex) {
                    logger.debug("Error during prefetch setup: {}", ex.getMessage());
                }

                for (FollowUpDto dto : pageResult.getContent()) dtoToMap.accept(dto);
                int total = (int) Math.min(pageResult.getTotalElements(), Integer.MAX_VALUE);
                followUpPage = new PageImpl<>(mapList, PageRequest.of(pageVal, sizeVal), total);
            }

            // Log content for diagnosis
            try {
                for (Object item : followUpPage.getContent()) {
                    logger.info("followUpsByStudent - item class={} content={}", item != null ? item.getClass().getName() : null, item);
                }
            } catch (Exception e) {
                logger.warn("followUpsByStudent - error logging content", e);
            }

            return ResponseHttpApi.responseHttpFindAll(
                    followUpPage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-ups retrieved successfully",
                    followUpPage.getTotalPages(),
                    pageVal,
                    (int) followUpPage.getTotalElements()
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // ============================
    // MUTATIONS
    // ============================

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_INSTRUCTOR') or hasAuthority('INSTRUCTOR')")
    @DgsMutation
    public Map<String, Object> addFollowUp(@InputArgument("input") FollowUpDto input) {
        try {
            logger.info("addFollowUp - incoming DTO followUpTypeId={} followUpType={} followUpStatusId={} followUpFlowStatusId={}",
                    input != null ? input.getFollowUpTypeId() : null,
                    input != null && input.getFollowUpType() != null ? "NOT NULL (id=" + input.getFollowUpType().getId() + ")" : "NULL",
                    input != null ? input.getFollowUpStatusId() : null,
                    input != null ? input.getFollowUpFlowStatusId() : null);

            FollowUpDto followUp = followUpBusiness.addFollowUp(input);
            return ResponseHttpApi.responseHttpAction(
                    followUp.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up added successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_INSTRUCTOR') or hasAuthority('INSTRUCTOR')")
    @DgsMutation
    public Map<String, Object> updateFollowUp(@InputArgument("id") Long id, @InputArgument("input") FollowUpDto input) {
        try {
            FollowUpDto followUp = followUpBusiness.updateFollowUp(id, input);
            return ResponseHttpApi.responseHttpAction(
                    followUp.getId(),
                    ResponseHttpApi.CODE_OK,
                    "Follow-up updated successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR') or hasAuthority('ROLE_INSTRUCTOR') or hasAuthority('INSTRUCTOR')")
    @DgsMutation
    public Map<String, Object> deleteFollowUp(@InputArgument("id") Long id) {
        try {
            followUpBusiness.deleteFollowUp(id);
            return ResponseHttpApi.responseHttpAction(
                    id,
                    ResponseHttpApi.CODE_OK,
                    "Follow-up deleted successfully");
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============================
    // HELPERS (gRPC fetch + cache)
    // ============================

    private co.sena.edu.olympo_back.Student.StudentResponse getStudentResponse(Long id) {
        if (id == null) return null;
        try {
            StudentCacheEntry entry = STUDENT_CACHE.get(id);
            long now = System.currentTimeMillis();
            if (entry != null && (now - entry.ts) < (entry.found ? CACHE_TTL_MS : NEGATIVE_TTL_MS)) {
                logger.debug("getStudentResponse - cache hit id={} found={}", id, entry.found);
                return entry.resp;
            }
            logger.debug("getStudentResponse - cache miss or expired for id={}", id);

            boolean permit = GRPC_SEMAPHORE.tryAcquire(1000, TimeUnit.MILLISECONDS);
            if (!permit) {
                logger.debug("gRPC semaphore busy for student id={}", id);
                return entry != null ? entry.resp : null;
            }
            logger.debug("getStudentResponse - acquired semaphore, fetching id={}", id);
            try {
                var resp = studentGrpcService.getStudentById(id);
                StudentCacheEntry newEntry = new StudentCacheEntry();
                newEntry.resp = resp;
                newEntry.ts = System.currentTimeMillis();
                newEntry.found = resp != null && resp.getFound();
                STUDENT_CACHE.put(id, newEntry);
                logger.debug("getStudentResponse - fetched id={} found={}", id, newEntry.found);
                return resp;
            } catch (Exception e) {
                logger.debug("Error fetching student id {}: {}", id, e.getMessage());
                StudentCacheEntry newEntry = new StudentCacheEntry();
                newEntry.resp = co.sena.edu.olympo_back.Student.StudentResponse.newBuilder().setFound(false).build();
                newEntry.ts = System.currentTimeMillis();
                newEntry.found = false;
                STUDENT_CACHE.put(id, newEntry);
                logger.debug("getStudentResponse - negative cached id={}", id);
                return newEntry.resp;
            } finally {
                GRPC_SEMAPHORE.release();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private co.sena.edu.olympo_back.proto.PersonResponse getPersonResponse(Long id) {
        if (id == null) return null;
        try {
            PersonCacheEntry entry = PERSON_CACHE.get(id);
            long now = System.currentTimeMillis();
            if (entry != null && (now - entry.ts) < (entry.found ? CACHE_TTL_MS : NEGATIVE_TTL_MS)) {
                logger.debug("getPersonResponse - cache hit id={} found={}", id, entry.found);
                return entry.resp;
            }
            logger.debug("getPersonResponse - cache miss or expired for id={}", id);

            boolean permit = GRPC_SEMAPHORE.tryAcquire(1000, TimeUnit.MILLISECONDS);
            if (!permit) {
                logger.debug("gRPC semaphore busy for person id={}", id);
                return entry != null ? entry.resp : null;
            }
            logger.debug("getPersonResponse - acquired semaphore, fetching id={}", id);
            try {
                var resp = personGrpcService.getPersonById(id);
                PersonCacheEntry newEntry = new PersonCacheEntry();
                newEntry.resp = resp;
                newEntry.ts = System.currentTimeMillis();
                newEntry.found = resp != null && resp.getFound();
                PERSON_CACHE.put(id, newEntry);
                logger.debug("getPersonResponse - fetched id={} found={}", id, newEntry.found);
                return resp;
            } catch (Exception e) {
                logger.debug("Error fetching person id {}: {}", id, e.getMessage());
                PersonCacheEntry newEntry = new PersonCacheEntry();
                newEntry.resp = co.sena.edu.olympo_back.proto.PersonResponse.newBuilder().setFound(false).build();
                newEntry.ts = System.currentTimeMillis();
                newEntry.found = false;
                PERSON_CACHE.put(id, newEntry);
                logger.debug("getPersonResponse - negative cached id={}", id);
                return newEntry.resp;
            } finally {
                GRPC_SEMAPHORE.release();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
