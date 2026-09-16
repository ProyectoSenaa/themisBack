package co.sena.edu.themis.Service;

import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.olympo_back.proto.PersonResponse;
import co.sena.edu.olympo_back.Administrative.AdministrativeResponse;
import co.sena.edu.olympo_back.Teacher.TeacherResponse;
import co.sena.edu.olympo_back.Student.StudentResponse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;

@Service
public class EmailCommitteeTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCommitteeTemplateService.class);

    private final PersonGrpcService personGrpcService;

    public EmailCommitteeTemplateService(PersonGrpcService personGrpcService) {
        this.personGrpcService = personGrpcService;
    }

    public String processCommitteeEventTemplate(
            CommitteeEventDto committeeEventDto,
            List<StudentResponse> students,
            List<TeacherResponse> teachers,
            List<AdministrativeResponse> administratives) throws IOException {
        try {
            String template = loadTemplate("templates/Email/committee-Create.html");

            logger.info("Renderizando plantilla de evento: students={}, teachers={}, administratives={}",
                    (students != null ? students.size() : 0),
                    (teachers != null ? teachers.size() : 0),
                    (administratives != null ? administratives.size() : 0));

            String studentsListHtml = createStudentListHtml(students);
            String teachersListHtml = createTeacherListHtml(teachers);
            String administrativesListHtml = createAdministrativeListHtml(administratives);

            String currentDate = LocalDate.now().toString();

            return template
                    .replace("${eventDate}", safeDate(committeeEventDto.getDate() != null ? LocalDate.parse(committeeEventDto.getDate()) : null))
                    .replace("${hour}", safeString(committeeEventDto.getHour()))
                    .replace("${session}", safeString(committeeEventDto.getSession()))
                    .replace("${studentsList}", studentsListHtml)
                    .replace("${teachersList}", teachersListHtml)
                    .replace("${administrativesList}", administrativesListHtml)
                    .replace("${currentDate}", currentDate);
        } catch (IOException e) {
            logger.error("Error al cargar la plantilla de registro de evento de comité desde 'templates/Email/committee-Create.html'", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error al procesar la plantilla de evento de comité: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String loadTemplate(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        if (!resource.exists()) {
            throw new IOException("La plantilla no se encontró: " + templatePath);
        }
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }


    private String createStudentListHtml(List<StudentResponse> students) {
        if (students == null || students.isEmpty()) {
            return "<li>No hay estudiantes registrados.</li>";
        }
        StringBuilder listHtml = new StringBuilder();
        for (StudentResponse student : students) {
            try {
                PersonResponse person = (student != null) ? student.getPerson() : null;
                // If person is missing or lacks useful fields, try fallback via PersonGrpcService
                if ((person == null || !person.getFound() || (isBlank(person.getName()) && isBlank(person.getLastname()) && isBlank(person.getEmail()))) && student != null) {
                    try {
                        // First try resolving by embedded personId if present
                        if (student.hasPerson() && student.getPerson() != null && student.getPerson().getPersonId() != 0L) {
                            PersonResponse p = personGrpcService.getPersonById(student.getPerson().getPersonId());
                            if (p != null && p.getFound()) person = p;
                        }
                        // If still null, try resolving by student id as candidate personId
                        if ((person == null || !person.getFound()) && student.getId() != 0L) {
                            PersonResponse p2 = personGrpcService.getPersonById(student.getId());
                            if (p2 != null && p2.getFound()) person = p2;
                        }
                    } catch (Exception e) {
                        logger.debug("Fallback person resolution failed for student id={}: {}", (student != null ? student.getId() : null), e.getMessage());
                    }
                }

                String fullName = getPersonFullName(person);
                String email = getPersonEmail(person);
                // If name is missing, show the email local-part as fallback to make list readable
                if ((fullName == null || fullName.isBlank()) && email != null && !email.isBlank()) {
                    int at = email.indexOf('@');
                    fullName = (at > 0) ? email.substring(0, at) : email;
                }
                listHtml.append("<li>").append(fullName).append(" (").append(email).append(")</li>");
            } catch (Exception ex) {
                logger.warn("No se pudo renderizar un estudiante en la lista: {}", ex.getMessage());
            }
        }
        return listHtml.toString();
    }

    private String createTeacherListHtml(List<TeacherResponse> teachers) {
        if (teachers == null || teachers.isEmpty()) {
            return "<li>No hay profesores registrados.</li>";
        }
        StringBuilder listHtml = new StringBuilder();
        for (TeacherResponse teacher : teachers) {
            try {
                PersonResponse person = null;
                if (teacher != null && teacher.getCollaborator() != null) {
                    person = teacher.getCollaborator().getPerson();
                }

                if ((person == null || !person.getFound() || (isBlank(person.getName()) && isBlank(person.getLastname()) && isBlank(person.getEmail()))) && teacher != null) {
                    try {
                        // Try embedded personId if present
                        if (teacher.hasCollaborator() && teacher.getCollaborator() != null && teacher.getCollaborator().hasPerson() && teacher.getCollaborator().getPerson().getPersonId() != 0L) {
                            PersonResponse p = personGrpcService.getPersonById(teacher.getCollaborator().getPerson().getPersonId());
                            if (p != null && p.getFound()) person = p;
                        }
                        // Fallback: try resolving by teacher id as personId
                        if ((person == null || !person.getFound()) && teacher.getId() != 0L) {
                            PersonResponse p2 = personGrpcService.getPersonById(teacher.getId());
                            if (p2 != null && p2.getFound()) person = p2;
                        }
                    } catch (Exception e) {
                        logger.debug("Fallback person resolution failed for teacher id={}: {}", (teacher != null ? teacher.getId() : null), e.getMessage());
                    }
                }

                String fullName = getPersonFullName(person);
                String email = getPersonEmail(person);
                if ((fullName == null || fullName.isBlank()) && email != null && !email.isBlank()) {
                    int at = email.indexOf('@');
                    fullName = (at > 0) ? email.substring(0, at) : email;
                }
                listHtml.append("<li>").append(fullName).append(" (").append(email).append(")</li>");
            } catch (Exception ex) {
                logger.warn("No se pudo renderizar un profesor en la lista: {}", ex.getMessage());
            }
        }
        return listHtml.toString();
    }

    private String createAdministrativeListHtml(List<AdministrativeResponse> administratives) {
        if (administratives == null || administratives.isEmpty()) {
            return "<li>No hay administrativos registrados.</li>";
        }
        StringBuilder listHtml = new StringBuilder();
        for (AdministrativeResponse administrative : administratives) {
            try {
                PersonResponse person = null;
                if (administrative != null && administrative.getCollaborator() != null) {
                    person = administrative.getCollaborator().getPerson();
                }

                if ((person == null || !person.getFound() || (isBlank(person.getName()) && isBlank(person.getLastname()) && isBlank(person.getEmail()))) && administrative != null) {
                    try {
                        if (administrative.hasCollaborator() && administrative.getCollaborator() != null && administrative.getCollaborator().hasPerson() && administrative.getCollaborator().getPerson().getPersonId() != 0L) {
                            PersonResponse p = personGrpcService.getPersonById(administrative.getCollaborator().getPerson().getPersonId());
                            if (p != null && p.getFound()) person = p;
                        }
                        if ((person == null || !person.getFound()) && administrative.getId() != 0L) {
                            PersonResponse p2 = personGrpcService.getPersonById(administrative.getId());
                            if (p2 != null && p2.getFound()) person = p2;
                        }
                    } catch (Exception e) {
                        logger.debug("Fallback person resolution failed for administrative id={}: {}", (administrative != null ? administrative.getId() : null), e.getMessage());
                    }
                }

                String fullName = getPersonFullName(person);
                String email = getPersonEmail(person);
                if ((fullName == null || fullName.isBlank()) && email != null && !email.isBlank()) {
                    int at = email.indexOf('@');
                    fullName = (at > 0) ? email.substring(0, at) : email;
                }
                listHtml.append("<li>").append(fullName).append(" (").append(email).append(")</li>");
            } catch (Exception ex) {
                logger.warn("No se pudo renderizar un administrativo en la lista: {}", ex.getMessage());
            }
        }
        return listHtml.toString();
    }

    private String safeDate(LocalDate date) {
        return date != null ? date.toString() : "";
    }

    private String safeTime(Time time) {
        return time != null ? time.toString() : "";
    }

    private String safeString(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : "";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String getPersonFullName(PersonResponse person) {
        if (person == null) return "";
        StringBuilder fullName = new StringBuilder();
        if (person.getName() != null && !person.getName().isEmpty()) {
            fullName.append(person.getName().trim());
        }
        if (person.getLastname() != null && !person.getLastname().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(person.getLastname().trim());
        }
        return fullName.length() > 0 ? fullName.toString() : "";
    }

    private String getPersonEmail(PersonResponse person) {
        if (person == null || person.getEmail() == null || person.getEmail().isEmpty()) {
            return "";
        }
        return person.getEmail().trim();
    }
}