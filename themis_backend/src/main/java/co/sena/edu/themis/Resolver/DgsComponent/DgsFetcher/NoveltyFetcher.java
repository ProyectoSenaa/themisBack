package co.sena.edu.themis.Resolver.DgsComponent.DgsFetcher;

import co.sena.edu.themis.Business.NoveltyBusiness;
import co.sena.edu.themis.Dto.OlympoFederated.Administrative;
import co.sena.edu.themis.Dto.OlympoFederated.Person;
import co.sena.edu.themis.Dto.OlympoFederated.Student;
import co.sena.edu.themis.Dto.OlympoFederated.Teacher;
import co.sena.edu.themis.Entity.Novelty;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsEntityFetcher;
import org.modelmapper.ModelMapper;

import java.util.HashMap;
import java.util.Map;

@DgsComponent
public class NoveltyFetcher {
    private final ModelMapper modelMapper = new ModelMapper();
    private final NoveltyBusiness noveltyBusiness;

    public NoveltyFetcher(NoveltyBusiness noveltyBusiness) {
        this.noveltyBusiness = noveltyBusiness;
    }

    @DgsEntityFetcher(name = "Novelty")
    public Novelty getNovelty(Map<String, Object> values) {
        System.out.println("fetcher novelty");
        Object idObj = values.get("id");
        Long id = null;
        try {
            if (idObj instanceof String s) {
                id = s.isEmpty() ? null : Long.parseLong(s);
            } else if (idObj instanceof Number n) {
                id = n.longValue();
            }
        } catch (Exception ignored) {
            // leave id as null
        }
        return modelMapper.map(noveltyBusiness.noveltyById(id), Novelty.class);
    }
    @DgsEntityFetcher(name = "Student")
    public Student getStudent(Map<String, Object> values) {
        System.out.println("fetcher student");
        Object idObj = values.get("id");
        Long idLong = null;
        try {
            if (idObj instanceof String s) {
                idLong = s.isEmpty() ? null : Long.parseLong(s);
            } else if (idObj instanceof Number n) {
                idLong = n.longValue();
            }
        } catch (Exception ignored) {
            // keep null
        }
        if (idLong == null) {
            return null;
        }
        Student student = new Student();
        student.setId(idLong);
        return student;
    }

    @DgsEntityFetcher(name = "Teacher")
    public Teacher getTeacher(Map<String, Object> values) {
        System.out.println("fetcher teacher");
        Object idObj = values.get("id");
        Long idLong = null;
        try {
            if (idObj instanceof String s) {
                idLong = s.isEmpty() ? null : Long.parseLong(s);
            } else if (idObj instanceof Number n) {
                idLong = n.longValue();
            }
        } catch (Exception ignored) {}
        if (idLong == null) {
            return null;
        }
        Teacher teacher = new Teacher();
        teacher.setId(idLong);
        return teacher;
    }

    @DgsEntityFetcher(name = "Administrative")
    public Administrative getAdministrative(Map<String, Object> values) {
        System.out.println("fetcher administrative");
        Object idObj = values.get("id");
        Long idLong = null;
        try {
            if (idObj instanceof String s) {
                idLong = s.isEmpty() ? null : Long.parseLong(s);
            } else if (idObj instanceof Number n) {
                idLong = n.longValue();
            }
        } catch (Exception ignored) {}
        if (idLong == null) {
            return null;
        }
        Administrative administrative = new Administrative();
        administrative.setId(idLong);
        return administrative;
    }

    // Added Coordination entity fetcher to satisfy federation @key requirement
    @DgsEntityFetcher(name = "Coordination")
    public Map<String, Object> getCoordination(Map<String, Object> values) {
        System.out.println("fetcher coordination");
        Object idObj = values.get("id");
        Long idLong = null;
        try {
            if (idObj instanceof String s) {
                idLong = s.isEmpty() ? null : Long.parseLong(s);
            } else if (idObj instanceof Number n) {
                idLong = n.longValue();
            }
        } catch (Exception ignored) {}
        if (idLong == null) {
            return null;
        }
        Map<String, Object> coordination = new HashMap<>();
        coordination.put("id", idLong);
        coordination.put("__typename", "Coordination");
        return coordination;
    }
}
