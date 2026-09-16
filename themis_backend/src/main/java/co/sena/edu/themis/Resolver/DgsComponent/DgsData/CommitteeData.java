package co.sena.edu.themis.Resolver.DgsComponent.DgsData;

import co.sena.edu.themis.Business.CommitteeBusiness;
import co.sena.edu.themis.Dto.CommitteeDto;
import co.sena.edu.themis.Dto.OlympoFederated.Administrative;
import co.sena.edu.themis.Dto.OlympoFederated.Student;
import co.sena.edu.themis.Dto.OlympoFederated.Teacher;
import co.sena.edu.themis.Entity.Committee;
import co.sena.edu.themis.Utils.mapper.CommitteeMapper;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DgsComponent
public class CommitteeData {

    private final CommitteeBusiness committeeBusiness;

    @Autowired
    public CommitteeData(CommitteeBusiness committeeBusiness) {
        this.committeeBusiness = committeeBusiness;
    }

    /**
     * Resuelve la coordinación para un comité específico
     */
    @DgsData(parentType = "Committee", field = "coordination")
    public Map<String, Object> coordination(DgsDataFetchingEnvironment env) {
        CommitteeDto committee = env.getSource();

        assert committee != null;
        System.out.println("Committee Coordination ID: " + committee.getCoordinationId());

        return Map.of(
                "__typename", "Coordination",
                "id", committee.getCoordinationId()
        );
    }

    /**
     * Resuelve la lista de estudiantes para un comité específico
     */
    @DgsData(parentType = "Committee", field = "students")
    public List<Map<String, Object>> students(DgsDataFetchingEnvironment env) {
        CommitteeDto committee = env.getSource();

        assert committee != null;
        System.out.println("Committee Students IDs: " + committee.getStudentsIds());

        return committee.getStudentsIds().stream()
                .map(studentId -> {
                    Map<String, Object> studentMap = Map.of(
                            "__typename", "Student",
                            "id", (Object) studentId
                    );
                    return studentMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Resuelve la lista de profesores para un comité específico
     */
    @DgsData(parentType = "Committee", field = "teachers")
    public List<Map<String, Object>> teachers(DgsDataFetchingEnvironment env) {
        CommitteeDto committee = env.getSource();

        assert committee != null;
        System.out.println("Committee Teachers IDs: " + committee.getTeachersIds());

        return committee.getTeachersIds().stream()
                .map(teacherId -> {
                    Map<String, Object> teacherMap = Map.of(
                            "__typename", "Teacher",
                            "id", (Object) teacherId
                    );
                    return teacherMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Resuelve la lista de administrativos para un comité específico
     */
    @DgsData(parentType = "Committee", field = "administratives")
    public List<Map<String, Object>> administratives(DgsDataFetchingEnvironment env) {
        CommitteeDto committee = env.getSource();

        assert committee != null;
        System.out.println("Committee Administratives IDs: " + committee.getAdministrativesIds());

        return committee.getAdministrativesIds().stream()
                .map(administrativeId -> {
                    Map<String, Object> adminMap = Map.of(
                            "__typename", "Administrative",
                            "id", (Object) administrativeId
                    );
                    return adminMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Resuelve los comités para un estudiante específico
     */
    @DgsData(parentType = "Student", field = "committees")
    public List<Committee> committeeStudentData(DgsDataFetchingEnvironment env) {
        try {
            Student student = env.getSource();
            assert student != null;
            Long studentId = student.getId();
            System.out.println("Finding committees for student ID: " + studentId);
            System.out.println("adwawdawdawdawdawdawdawda");

            List<CommitteeDto> committeeDtos = committeeBusiness.findCommitteesByStudentId(studentId);

            return committeeDtos.stream()
                    .map(CommitteeMapper.INSTANCE::toEntity)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Resuelve los comités para un profesor específico
     */
    @DgsData(parentType = "Teacher", field = "committees")
    public List<Committee> committeeTeacherData(DgsDataFetchingEnvironment env) {
        Teacher teacher = env.getSource();
        Long teacherId;
        try {
            teacherId = Long.valueOf(teacher.getId());
        } catch (Exception e) {
            return null;
        }

        List<CommitteeDto> committeeDtos = committeeBusiness.findCommitteesByTeacherId(teacherId);
        return committeeDtos.stream()
                .map(CommitteeMapper.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }


    @DgsData(parentType = "Administrative", field = "committees")
    public List<Committee> committeeAdministrativeData(DgsDataFetchingEnvironment env) {
        Administrative administrative = env.getSource();
        Long administrativeId = administrative.getId();

        List<CommitteeDto> committeeDtos = committeeBusiness.findCommitteesByAdministrativeId(administrativeId);
        return committeeDtos.stream()
                .map(CommitteeMapper.INSTANCE::toEntity)
                .collect(Collectors.toList());
    }


    @DgsData(parentType = "Coordination", field = "committees")
    public List<Committee> committeeCoordinationData(DgsDataFetchingEnvironment env) {
        try {
            Map<String, Object> coordination = env.getSource();
            assert coordination != null;
            Long coordinationId = Long.valueOf(coordination.get("id").toString());
            System.out.println("Finding committees for coordination ID: " + coordinationId);

            List<CommitteeDto> committeeDtos = committeeBusiness.findCommitteesByCoordinationId(coordinationId);
            return committeeDtos.stream()
                    .map(CommitteeMapper.INSTANCE::toEntity)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}