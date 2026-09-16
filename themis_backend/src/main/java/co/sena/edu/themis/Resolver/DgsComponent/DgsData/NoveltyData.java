package co.sena.edu.themis.Resolver.DgsComponent.DgsData;

import co.sena.edu.themis.Business.NoveltyBusiness;
import co.sena.edu.themis.Dto.NoveltyDto;
import co.sena.edu.themis.Dto.OlympoFederated.Administrative;
import co.sena.edu.themis.Dto.OlympoFederated.Student;
import co.sena.edu.themis.Dto.OlympoFederated.Teacher;
import co.sena.edu.themis.Entity.Novelty;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@DgsComponent
public class NoveltyData {
    private final NoveltyBusiness noveltyBusiness;

    @Autowired
    public NoveltyData(NoveltyBusiness noveltyBusiness) {
        this.noveltyBusiness = noveltyBusiness;
    }

    @DgsData(parentType = "Novelty", field = "student")
    public Map<String, Object> student(DgsDataFetchingEnvironment env) {
        NoveltyDto novelty = env.getSource();
        if (novelty == null || novelty.getStudentId() == null) {
            throw new RuntimeException("Error fetching student: Student not found");
        }
        return Map.of(
            "__typename", "Student",
            "id", novelty.getStudentId()
        );
    }

    @DgsData(parentType = "Novelty", field = "teacher")
    public Map<String, Object> teacher(DgsDataFetchingEnvironment env) {
        NoveltyDto novelty = env.getSource();
        if (novelty == null || novelty.getTeacherId() == null) {
            return null;
        }
        return Map.of(
            "__typename", "Teacher",
            "id", novelty.getTeacherId()
        );
    }

    @DgsData(parentType = "Novelty", field = "administrative")
    public Map<String, Object> administrative(DgsDataFetchingEnvironment env) {
        NoveltyDto novelty = env.getSource();
        if (novelty == null || novelty.getAdministrativeId() == null) {
            return null;
        }
        return Map.of(
            "__typename", "Administrative",
            "id", novelty.getAdministrativeId()
        );
    }

    @DgsData(parentType = "Student", field = "novelties")
    public List<Novelty> noveltyStudentData(DgsDataFetchingEnvironment env) {
        Student student = env.getSource();
        if (student == null || student.getId() == null) {
            throw new RuntimeException("Error fetching student: Student not found");
        }
        return noveltyBusiness.findAllByIdStudent(student.getId());
    }

    @DgsData(parentType = "Teacher", field = "novelties")
    public List<Novelty> noveltyTeacherData(DgsDataFetchingEnvironment env) {
        Teacher teacher = env.getSource();
        Long idTeacher;
        try {
            idTeacher = Long.valueOf(teacher.getId());
        } catch (Exception e) {
            return null;
        }
        return noveltyBusiness.findAllByIdTeacher(idTeacher);
    }

    @DgsData(parentType = "Administrative", field = "novelties")
    public List<Novelty> noveltyAdministrativeData(DgsDataFetchingEnvironment env) {
        Administrative administrative = env.getSource();
        Long idAdministrative = administrative.getId();
        return noveltyBusiness.findAllByIdAdministrative(idAdministrative);
    }
}
