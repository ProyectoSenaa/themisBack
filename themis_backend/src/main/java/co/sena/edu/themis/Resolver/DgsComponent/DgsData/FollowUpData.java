package co.sena.edu.themis.Resolver.DgsComponent.DgsData;

import co.sena.edu.themis.Business.FollowUpBusiness;
import co.sena.edu.themis.Dto.FollowUpDto;
import co.sena.edu.themis.Dto.OlympoFederated.Administrative;
import co.sena.edu.themis.Dto.OlympoFederated.Student;
import co.sena.edu.themis.Dto.OlympoFederated.Teacher;
import co.sena.edu.themis.Entity.FollowUp;
import co.sena.edu.themis.Utils.DataConvert;
import com.netflix.graphql.dgs.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@DgsComponent
public class FollowUpData {

    private final FollowUpBusiness followUpBusiness;
    private final DataConvert dataConvert;

    @Autowired
    public FollowUpData(FollowUpBusiness followUpBusiness, DataConvert dataConvert) {
        this.followUpBusiness = followUpBusiness;
        this.dataConvert = dataConvert;
    }

    // ============================================================
    //              FEDERATED FIELDS (MISMA ESTRUCTURA QUE Novelty)
    // ============================================================

    @DgsData(parentType = "FollowUp", field = "student")
    public Map<String, Object> student(DgsDataFetchingEnvironment env) {
        Object source = env.getSource();

        // Si la fuente es un Map (por ejemplo cuando FollowUpResolver construye mapas), preferir la info en el mapa
        if (source instanceof Map) {
            Object sObj = ((Map<?, ?>) source).get("student");
            if (sObj instanceof Map) {
                //noinspection unchecked
                Map<String, Object> sMap = (Map<String, Object>) sObj;
                Object personObj = sMap.get("person");
                boolean hasPerson = personObj instanceof Map && ((Map<?, ?>) personObj).get("name") != null;
                boolean hasName = sMap.get("name") != null && !String.valueOf(sMap.get("name")).isBlank();
                if (hasPerson && hasName) return sMap;

                // Si el mapa ya contiene id, devolver referencia mínima
                Object idObj = ((Map<?, ?>) source).get("studentId");
                if (idObj == null) idObj = ((Map<?, ?>) source).get("student_id");
                if (idObj == null) {
                    Object studentInner = ((Map<?, ?>) source).get("student");
                    if (studentInner instanceof Map) idObj = ((Map<?, ?>) studentInner).get("id");
                }
                Long existingId = null;
                if (idObj instanceof Number) existingId = ((Number) idObj).longValue();
                else if (idObj != null) {
                    existingId = dataConvert.parseLongOrNull(String.valueOf(idObj));
                }
                if (existingId != null) {
                    return Map.of(
                            "__typename", "Student",
                            "id", String.valueOf(existingId)
                    );
                }

                // No hay id ni datos: devolver el mapa rellenado con fallbacks para evitar 'undefined'
                String fallbackVal = "No especificado";
                if (!sMap.containsKey("name")) sMap.put("name", fallbackVal);
                if (!sMap.containsKey("firstname")) sMap.put("firstname", fallbackVal);
                if (!sMap.containsKey("lastname")) sMap.put("lastname", fallbackVal);
                if (!sMap.containsKey("person")) {
                    Map<String, Object> pf = new java.util.HashMap<>();
                    pf.put("__typename", "Person");
                    pf.put("id", null);
                    pf.put("document", fallbackVal);
                    pf.put("name", fallbackVal);
                    pf.put("lastname", fallbackVal);
                    pf.put("phone", fallbackVal);
                    pf.put("email", fallbackVal);
                    pf.put("address", fallbackVal);
                    sMap.put("person", pf);
                }
                return sMap;
            }
        }

        // Si la fuente es FollowUpDto o FollowUp, extraer id directamente
        Long studentId = null;
        if (source instanceof FollowUpDto) studentId = ((FollowUpDto) source).getStudentId();
        else if (source instanceof FollowUp) studentId = ((FollowUp) source).getStudentId();

        if (studentId == null) return null;
        java.util.Map<String, Object> studentRef = new java.util.HashMap<>();
        studentRef.put("__typename", "Student");
        studentRef.put("id", String.valueOf(studentId));
        String fallbackStudent = "No especificado";
        studentRef.put("name", fallbackStudent);
        studentRef.put("firstname", fallbackStudent);
        studentRef.put("lastname", fallbackStudent);
        java.util.Map<String, Object> personFallback = new java.util.HashMap<>();
        personFallback.put("__typename", "Person");
        personFallback.put("id", null);
        personFallback.put("document", fallbackStudent);
        personFallback.put("name", fallbackStudent);
        personFallback.put("lastname", fallbackStudent);
        personFallback.put("phone", fallbackStudent);
        personFallback.put("email", fallbackStudent);
        personFallback.put("address", fallbackStudent);
        studentRef.put("person", personFallback);
        return studentRef;
    }

    @DgsData(parentType = "FollowUp", field = "teacher")
    public Map<String, Object> teacher(DgsDataFetchingEnvironment env) {
        Object source = env.getSource();

        // Si la fuente es mapa, intentar extraer teacher id o devolver mapa con fallbacks
        if (source instanceof Map) {
            Object tObj = ((Map<?, ?>) source).get("teacher");
            if (tObj instanceof Map) {
                Map<String,Object> tMap = toStringKeyMap(tObj);
                Object idObj = ((Map<?, ?>) source).get("teacherId");
                if (idObj == null) idObj = ((Map<?, ?>) source).get("teacher_id");
                if (idObj == null) idObj = tMap.get("id");
                Long existingId = null;
                if (idObj instanceof Number) existingId = ((Number) idObj).longValue();
                else if (idObj != null) { existingId = dataConvert.parseLongOrNull(String.valueOf(idObj)); }
                if (existingId != null) {
                    java.util.Map<String,Object> teacherRef = new java.util.HashMap<>();
                    teacherRef.put("__typename","Teacher");
                    teacherRef.put("id", String.valueOf(existingId));
                    String fallbackTeacher = "No especificado";
                    teacherRef.put("name", fallbackTeacher);
                    java.util.Map<String,Object> person = new java.util.HashMap<>();
                    person.put("__typename","Person");
                    person.put("id", null);
                    person.put("document", fallbackTeacher);
                    person.put("name", fallbackTeacher);
                    person.put("lastname", fallbackTeacher);
                    person.put("phone", fallbackTeacher);
                    person.put("email", fallbackTeacher);
                    person.put("address", fallbackTeacher);
                    teacherRef.put("person", person);
                    return teacherRef;
                }
                // no id: fill fallbacks in place
                String fallbackTeacher = "No especificado";
                if (!tMap.containsKey("name")) tMap.put("name", fallbackTeacher);
                if (!tMap.containsKey("person")) {
                    Map<String,Object> pf = new java.util.HashMap<>();
                    pf.put("__typename","Person");
                    pf.put("id", null);
                    pf.put("document", fallbackTeacher);
                    pf.put("name", fallbackTeacher);
                    pf.put("lastname", fallbackTeacher);
                    pf.put("phone", fallbackTeacher);
                    pf.put("email", fallbackTeacher);
                    pf.put("address", fallbackTeacher);
                    tMap.put("person", pf);
                }
                return tMap;
            }
        }

        Long teacherId = null;
        if (source instanceof FollowUpDto) teacherId = ((FollowUpDto) source).getTeacherId();
        else if (source instanceof FollowUp) teacherId = ((FollowUp) source).getTeacherId();

        if (teacherId == null) return null;
        java.util.Map<String, Object> teacherRef = new java.util.HashMap<>();
        teacherRef.put("__typename", "Teacher");
        teacherRef.put("id", String.valueOf(teacherId));
        String fallbackTeacher = "No especificado";
        teacherRef.put("name", fallbackTeacher);
        java.util.Map<String, Object> teacherPerson = new java.util.HashMap<>();
        teacherPerson.put("__typename", "Person");
        teacherPerson.put("id", null);
        teacherPerson.put("document", fallbackTeacher);
        teacherPerson.put("name", fallbackTeacher);
        teacherPerson.put("lastname", fallbackTeacher);
        teacherPerson.put("phone", fallbackTeacher);
        teacherPerson.put("email", fallbackTeacher);
        teacherPerson.put("address", fallbackTeacher);
        teacherRef.put("person", teacherPerson);
        return teacherRef;
    }

    @DgsData(parentType = "FollowUp", field = "coordinator")
    public Map<String, Object> coordinator(DgsDataFetchingEnvironment env) {
        Object source = env.getSource();
        if (source instanceof Map) {
            Object coordObj = ((Map<?, ?>) source).get("coordinator");
            if (coordObj instanceof Map) {
                Map<String,Object> cMap = toStringKeyMap(coordObj);
                Object idObj = ((Map<?, ?>) source).get("coordinatorId");
                if (idObj == null) idObj = ((Map<?, ?>) source).get("coordinator_id");
                if (idObj == null) idObj = cMap.get("id");
                Long existingId = null;
                if (idObj instanceof Number) existingId = ((Number) idObj).longValue();
                else if (idObj != null) { try { existingId = Long.parseLong(String.valueOf(idObj)); } catch (Exception ignored) {} }
                if (existingId != null) return Map.of("__typename","Administrative","id", String.valueOf(existingId));
                return cMap;
            }
        }
        Long coordinatorId = null;
        Object src = env.getSource();
        if (src instanceof FollowUpDto) coordinatorId = ((FollowUpDto) src).getCoordinatorId();
        else if (src instanceof FollowUp) coordinatorId = ((FollowUp) src).getCoordinatorId();
        if (coordinatorId == null) return null;
        return Map.of("__typename","Administrative","id", String.valueOf(coordinatorId));
    }

    // ============================================================
    //     *** AQUÍ SE INTEGRA EL CÓDIGO DEL TIPO DE SEGUIMIENTO ***
    // ============================================================

//    @DgsData(parentType = "FollowUp", field = "followUpType")
//    public Map<String, Object> followUpType(DgsDataFetchingEnvironment env) {
//        Object source = env.getSource();
//        if (source == null) return null;
//
//        // Caso 1: el parent es un Map (cuando FollowUpResolver construye mapas)
//        if (source instanceof Map) {
//            Map<String, Object> m = toStringKeyMap(source);
//            if (m == null) return null;
//            Object full = m.get("followUpType");
//            if (full instanceof Map) {
//                // Asegurar que id esté como String
//                Map<?,?> fm = (Map<?,?>) full;
//                Object idObj = fm.get("id");
//                String id = idObj != null ? String.valueOf(idObj) : null;
//                java.util.Map<String,Object> out = new java.util.HashMap<>();
//                out.put("__typename","FollowUpType");
//                if (id != null) out.put("id", id);
//                if (fm.get("name") != null) out.put("name", String.valueOf(fm.get("name")));
//                if (fm.get("isActive") != null) out.put("isActive", fm.get("isActive"));
//                if (fm.get("description") != null) out.put("description", String.valueOf(fm.get("description")));
//                return out;
//            }
//            // Fallback: buscar followUpTypeId en el mapa
//            Object tid = m.get("followUpTypeId");
//            if (tid == null) tid = m.get("follow_up_type_id");
//            Long typeId = null;
//            if (tid instanceof Number) typeId = ((Number) tid).longValue();
//            else if (tid != null) typeId = dataConvert.parseLongOrNull(String.valueOf(tid));
//            if (typeId != null) return Map.of("__typename","FollowUpType","id", String.valueOf(typeId));
//            return null;
//        }
//
//        // Caso 2: la fuente es FollowUpDto o FollowUp
//        if (source instanceof FollowUpDto) {
//            FollowUpDto dto = (FollowUpDto) source;
//            if (dto.getFollowUpType() != null) {
//                return Map.of(
//                        "__typename", "FollowUpType",
//                        "id", String.valueOf(dto.getFollowUpType().getId()),
//                        "name", dto.getFollowUpType().getName(),
//                        "isActive", dto.getFollowUpType().isActive(),
//                        "description", dto.getFollowUpType().getDescription()
//                );
//            }
//            if (dto.getFollowUpTypeId() != null) {
//                return Map.of("__typename","FollowUpType","id", String.valueOf(dto.getFollowUpTypeId()));
//            }
//        } else if (source instanceof FollowUp) {
//            FollowUp f = (FollowUp) source;
//            if (f.getFollowUpType() != null) {
//                return Map.of("__typename","FollowUpType","id", String.valueOf(f.getFollowUpType().getId()));
//            }
//            if (f.getFollowUpTypeId() != null) {
//                return Map.of("__typename","FollowUpType","id", String.valueOf(f.getFollowUpTypeId()));
//            }
//        }
//
//        return null;
//    }

    // ============================================================
    //          RELACIONES DESDE LOS TIPOS FEDERADOS
    //          (MISMA ESTRUCTURA QUE EN Novelty)
    // ============================================================

    @DgsData(parentType = "Student", field = "followUps")
    public List<FollowUp> followUpsByStudent(DgsDataFetchingEnvironment env) {
        Student student = env.getSource();
        if (student == null || student.getId() == null) {
            throw new RuntimeException("Error fetching student: Student not found");
        }
        Long sid = dataConvert.parseLongOrNull(String.valueOf(student.getId()));
        if (sid == null) throw new RuntimeException("Error fetching student: invalid id");
        return followUpBusiness.findAllByIdStudent(sid);
    }

    @DgsData(parentType = "Teacher", field = "followUps")
    public List<FollowUp> followUpsByTeacher(DgsDataFetchingEnvironment env) {
        Teacher teacher = env.getSource();
        if (teacher == null || teacher.getId() == null) {
            return null;
        }
        Long tid = dataConvert.parseLongOrNull(String.valueOf(teacher.getId()));
        if (tid == null) return null;
        return followUpBusiness.findAllByIdTeacher(tid);
    }

    @DgsData(parentType = "Administrative", field = "followUps")
    public List<FollowUp> followUpsByAdministrative(DgsDataFetchingEnvironment env) {
        Administrative administrative = env.getSource();
        if (administrative == null || administrative.getId() == null) {
            return null;
        }
        Long aid = dataConvert.parseLongOrNull(String.valueOf(administrative.getId()));
        if (aid == null) return null;
        return followUpBusiness.findAllByIdCoordinator(aid);
    }

    // Convierte un Map<?,?> a Map<String,Object> usando toString() en keys no-string
    private Map<String,Object> toStringKeyMap(Object obj) {
        if (!(obj instanceof Map)) return null;
        Map<?,?> raw = (Map<?,?>) obj;
        Map<String,Object> dst = new java.util.HashMap<>();
        for (Map.Entry<?,?> e : raw.entrySet()) {
            Object k = e.getKey();
            String key = (k instanceof String) ? (String) k : String.valueOf(k);
            dst.put(key, e.getValue());
        }
        return dst;
    }
}
