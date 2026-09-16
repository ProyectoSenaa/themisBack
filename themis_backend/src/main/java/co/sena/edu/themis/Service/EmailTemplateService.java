package co.sena.edu.themis.Service;

import co.sena.edu.themis.Dto.NoveltyDto;
import co.sena.edu.themis.Dto.NoveltyTypeDto;
import co.sena.edu.olympo_back.proto.PersonResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateService.class);
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final Map<String, NoveltyTypeInfo> NOVELTY_TYPES = new HashMap<>();
    private final PersonGrpcService personGrpcService;

    public EmailTemplateService(PersonGrpcService personGrpcService) {
        this.personGrpcService = personGrpcService;
    }

    static {
        NOVELTY_TYPES.put("RETIRO_VOLUNTARIO", new NoveltyTypeInfo(
                "Retiro Voluntario",
                "Capítulo VI, Artículo 21.",
                "Solicitud formal del aprendiz para retirarse definitivamente del programa de formación en cualquiera de sus modalidades."
        ));
        NOVELTY_TYPES.put("DESERCION", new NoveltyTypeInfo(
                "Deserción",
                "Capítulo V, Artículo 41",
                "Solicitud formal del aprendiz para suspender temporalmente su programa de formación por razones excepcionales que le impiden asistir por 20 o más días continuos."
        ));
        NOVELTY_TYPES.put("CONDICIONAMIENTO_DE_MATRICULA", new NoveltyTypeInfo(
                "Condicionamiento de Matrícula",
                "Capítulo V, Artículo 47",
                "Medida académica sancionatoria que se impone al aprendiz que incurra en una falta académica o disciplinaria, previo agotamiento de la aplicación de las medidas formativas o disciplinarias."
        ));
        NOVELTY_TYPES.put("CANCELACION_DE_MATRICULA", new NoveltyTypeInfo(
                "Cancelación de Matrícula",
                "Capítulo V, Artículo 47",
                "Acto administrativo que se origina cuando persisten en el aprendiz las causales que originaron el condicionamiento de la matrícula o por faltas catalogadas como graves o gravísimas de acuerdo con la clasificación determinada, o situaciones que se consideran deserción en este Reglamento."
        ));
        NOVELTY_TYPES.put("TRASLADO", new NoveltyTypeInfo(
                "Traslado",
                "Capítulo VI, Artículo 21",
                "Es la solicitud formal que el Aprendiz eleva a través de oficio radicado en el centro de formación y registra en el sistema de gestión de formación cuando requiere cambio de jornada, de Centro de Formación, en el mismo programa y en la misma modalidad de formación, o en otro que corresponda a la misma red o línea tecnológica."
        ));
        NOVELTY_TYPES.put("APLAZAMIENTO", new NoveltyTypeInfo(
                "Aplazamiento",
                "Capítulo VI, Artículo 21",
                "Es la solicitud formal que el aprendiz presenta para suspender temporalmente su proceso de formación, por causas justificadas."
        ));
        NOVELTY_TYPES.put("REINGRESO", new NoveltyTypeInfo(
                "Reingreso",
                "Capítulo V, Artículo 41",
                "Es la solicitud formal que el aprendiz presenta para retomar su proceso de formación después de un aplazamiento o retiro voluntario."
        ));
    }

    public String processNoveltyCreationTemplate(NoveltyDto noveltyDto, NoveltyTypeDto noveltyTypeDto, PersonResponse student, PersonResponse teacher, PersonResponse administrative, String studentSource, String teacherSource, String administrativeSource) throws IOException {
        // Las fuentes se pasan por compatibilidad/depuración desde EmailService pero actualmente no se usan
        return processNoveltyCreationTemplate(noveltyDto, noveltyTypeDto, student, teacher, administrative);
    }

    public String processNoveltyCreationTemplate(NoveltyDto noveltyDto, NoveltyTypeDto noveltyTypeDto, PersonResponse student, PersonResponse teacher, PersonResponse administrative) throws IOException {
        try {
            String template = loadTemplate("templates/Email/Novelty-Create.html");
            return replaceTemplateVariables(template, noveltyDto, noveltyTypeDto, student, teacher, administrative);
        } catch (IOException e) {
            logger.error("Error al cargar la plantilla de creación de novedad desde 'templates/Email/Novelty-Create.html'", e);
            throw e;
        }
    }

    public String processNoveltyUpdateTemplate(NoveltyDto noveltyDto, NoveltyTypeDto noveltyTypeDto, PersonResponse student, PersonResponse teacher, PersonResponse administrative) throws IOException {
        try {
            String template = loadTemplate("templates/Email/Novelty-Update.html");
            return replaceTemplateVariables(template, noveltyDto, noveltyTypeDto, student, teacher, administrative);
        } catch (IOException e) {
            logger.error("Error al cargar la plantilla de actualización de novedad desde 'templates/Email/Novelty-Update.html'", e);
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

    private String replaceTemplateVariables(String template, NoveltyDto noveltyDto, NoveltyTypeDto noveltyTypeDto, PersonResponse student, PersonResponse teacher, PersonResponse administrative) {
        // Si las PersonResponse recibidas son nulas o no contienen información útil, intentamos resolverlas usando los IDs disponibles en NoveltyDto
        PersonResponse studentResolved = student;
        if (!hasUsefulPersonInfo(studentResolved) && noveltyDto != null && noveltyDto.getStudentId() != null) {
            try {
                studentResolved = getPersonById(noveltyDto.getStudentId());
            } catch (Exception e) {
                logger.warn("No se pudo resolver student por ID {}", noveltyDto.getStudentId(), e);
            }
        }

        PersonResponse teacherResolved = teacher;
        if (!hasUsefulPersonInfo(teacherResolved) && noveltyDto != null && noveltyDto.getTeacherId() != null) {
            try {
                teacherResolved = getPersonById(noveltyDto.getTeacherId());
            } catch (Exception e) {
                logger.warn("No se pudo resolver teacher por ID {}", noveltyDto.getTeacherId(), e);
            }
        }

        PersonResponse administrativeResolved = administrative;
        if (!hasUsefulPersonInfo(administrativeResolved) && noveltyDto != null && noveltyDto.getAdministrativeId() != null) {
            try {
                administrativeResolved = getPersonById(noveltyDto.getAdministrativeId());
            } catch (Exception e) {
                logger.warn("No se pudo resolver administrative por ID {}", noveltyDto.getAdministrativeId(), e);
            }
        }

        NoveltyTypeInfo noveltyInfo = getNoveltyTypeInfo(noveltyTypeDto != null ? noveltyTypeDto.getNameNovelty() : null);

        // Obtener nombres y emails con fallback a los campos de NoveltyDto antes de usar el texto por defecto
        String studentName = getPersonFullName(studentResolved);
        if ((studentName == null || isBlankPersonValue(studentName)) && noveltyDto != null && noveltyDto.getStudentName() != null && !noveltyDto.getStudentName().isBlank()) {
            studentName = noveltyDto.getStudentName();
        }
        String studentEmail = getPersonEmail(studentResolved);
        if ((studentEmail == null || isBlankPersonValue(studentEmail)) && noveltyDto != null && noveltyDto.getStudentEmail() != null && !noveltyDto.getStudentEmail().isBlank()) {
            studentEmail = noveltyDto.getStudentEmail();
        }

        String teacherName = getPersonFullName(teacherResolved);
        if ((teacherName == null || isBlankPersonValue(teacherName)) && noveltyDto != null && noveltyDto.getTeacherName() != null && !noveltyDto.getTeacherName().isBlank()) {
            teacherName = noveltyDto.getTeacherName();
        }
        String teacherEmail = getPersonEmail(teacherResolved);
        if ((teacherEmail == null || isBlankPersonValue(teacherEmail)) && noveltyDto != null && noveltyDto.getTeacherEmail() != null && !noveltyDto.getTeacherEmail().isBlank()) {
            teacherEmail = noveltyDto.getTeacherEmail();
        }

        String administrativeName = getPersonFullName(administrativeResolved);
        if ((administrativeName == null || isBlankPersonValue(administrativeName)) && noveltyDto != null && noveltyDto.getAdministrativeEmail() != null && !noveltyDto.getAdministrativeEmail().isBlank()) {
            // NoveltyDto no tiene administrativeName; usamos email si existe para al menos mostrar algo
            administrativeName = noveltyDto.getAdministrativeEmail();
        }
        String administrativeEmail = getPersonEmail(administrativeResolved);
        if ((administrativeEmail == null || isBlankPersonValue(administrativeEmail)) && noveltyDto != null && noveltyDto.getAdministrativeEmail() != null && !noveltyDto.getAdministrativeEmail().isBlank()) {
            administrativeEmail = noveltyDto.getAdministrativeEmail();
        }

        // Fallback adicional: si no hay nombre pero sí email, usar la parte local del email para mostrar algo legible
        if (isBlankPersonValue(studentName) && !isBlankPersonValue(studentEmail)) {
            int at = studentEmail.indexOf('@');
            studentName = (at > 0) ? studentEmail.substring(0, at) : studentEmail;
        }
        if (isBlankPersonValue(teacherName) && !isBlankPersonValue(teacherEmail)) {
            int at = teacherEmail.indexOf('@');
            teacherName = (at > 0) ? teacherEmail.substring(0, at) : teacherEmail;
        }
        if (isBlankPersonValue(administrativeName) && !isBlankPersonValue(administrativeEmail)) {
            int at = administrativeEmail.indexOf('@');
            administrativeName = (at > 0) ? administrativeEmail.substring(0, at) : administrativeEmail;
        }

        // Debug: registrar los valores finales que se van a inyectar en la plantilla
        try {
            logger.debug("Template variables before replacement -> studentName='{}', studentEmail='{}', teacherName='{}', teacherEmail='{}', administrativeName='{}', administrativeEmail='{}'",
                    studentName, studentEmail, teacherName, teacherEmail, administrativeName, administrativeEmail);
        } catch (Exception e) {
            logger.debug("No se pudo loguear variables de plantilla: {}", e.getMessage());
        }

        return template
                .replace("${noveltyId}", safeLong(noveltyDto.getId()))
                .replace("${noveltyTypeName}", safeString(noveltyInfo.getDisplayName()))
                .replace("${noveltyTypeDescription}", safeString(noveltyInfo.getDescription()))
                .replace("${noveltyArticle}", safeString(noveltyInfo.getArticle()))
                .replace("${justification}", safeString(noveltyDto.getJustification()))
                .replace("${observation}", safeString(noveltyDto.getObservation()))
                .replace("${status}", safeString(noveltyDto.getNoveltyStatus() != null ? noveltyDto.getNoveltyStatus().getName() : "N/A"))
                .replace("${noveltyDate}", formatDate(noveltyDto.getDate()))
                .replace("${studentName}", safePersonString(studentName))
                .replace("${studentEmail}", safePersonString(studentEmail))
                .replace("${teacherName}", safePersonString(teacherName))
                .replace("${teacherEmail}", safePersonString(teacherEmail))
                .replace("${administrativeName}", safePersonString(administrativeName))
                .replace("${administrativeEmail}", safePersonString(administrativeEmail))
                .replace("${currentDate}", formatDate(new Date()));
    }

    // Helper específico para campos de persona: devuelve cadena legible si no hay dato útil
    private String safePersonString(String value) {
        if (value == null) return "No disponible";
        String v = value.trim();
        if (v.isEmpty()) return "No disponible";
        // Evitar propagar la cadena genérica "N/A" en los campos de persona
        if (v.equalsIgnoreCase("N/A")) return "No disponible";
        return v;
    }

    // Helper que indica si el valor de persona está vacío o es equivalente a 'N/A' (no legible)
    private boolean isBlankPersonValue(String v) {
        if (v == null) return true;
        String t = v.trim();
        return t.isEmpty() || t.equalsIgnoreCase("N/A") || t.equalsIgnoreCase("No disponible");
    }

    private NoveltyTypeInfo getNoveltyTypeInfo(String noveltyTypeName) {
        if (noveltyTypeName == null || noveltyTypeName.trim().isEmpty()) {
            logger.warn("Nombre de tipo de novedad nulo o vacío, usando información por defecto");
            return getDefaultNoveltyTypeInfo();
        }
        String normalizedName = noveltyTypeName.trim().toUpperCase().replace(" ", "_");
        NoveltyTypeInfo info = NOVELTY_TYPES.get(normalizedName);
        if (info == null) {
            logger.warn("Tipo de novedad no encontrado: {}, usando información por defecto", noveltyTypeName);
            return getDefaultNoveltyTypeInfo();
        }
        return info;
    }

    private NoveltyTypeInfo getDefaultNoveltyTypeInfo() {
        return new NoveltyTypeInfo(
                "Tipo de novedad no especificado",
                "Artículo no especificado",
                "Descripción no disponible"
        );
    }

    private String safeLong(Long value) {
        return value != null ? value.toString() : "N/A";
    }

    private String safeString(String value) {
        return value != null && !value.trim().isEmpty() ? value : "N/A";
    }

    private String formatDate(Date date) {
        return date != null ? dateFormatter.format(date) : "N/A";
    }

    private String formatDate(String dateString) {
        return dateString != null ? dateString : "N/A";
    }

    private PersonResponse getPersonById(Long personId) {
        if (personId == null) return null;
        try {
            // Asegurarnos de pasar valor primitivo al servicio gRPC (unboxing seguro)
            return personGrpcService.getPersonById(personId.longValue());
        } catch (Exception e) {
            logger.warn("No se pudo obtener la información de la persona con ID: {}", personId, e);
            return null;
        }
    }

    // Helper que determina si la PersonResponse tiene al menos nombre, apellido o email y fue encontrada
    private boolean hasUsefulPersonInfo(PersonResponse p) {
        if (p == null) return false;
        try {
            // Si la respuesta del servicio indica que la persona no fue encontrada, no es útil
            if (!p.getFound()) return false;
            if (p.getName() != null && !p.getName().trim().isEmpty()) return true;
            if (p.getLastname() != null && !p.getLastname().trim().isEmpty()) return true;
            if (p.getEmail() != null && !p.getEmail().trim().isEmpty()) return true;
        } catch (Exception e) {
            // En caso de que PersonResponse implemente accesores que lancen, consideramos no útil
            logger.debug("Error comprobando información de PersonResponse", e);
        }
        return false;
    }

    private String getPersonFullName(PersonResponse person) {
        if (person == null || !person.getFound()) return "";
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
        if (person == null || !person.getFound() || person.getEmail() == null || person.getEmail().isEmpty()) {
            return "";
        }
        return person.getEmail().trim();
    }

    private static class NoveltyTypeInfo {
        private final String displayName;
        private final String article;
        private final String description;

        public NoveltyTypeInfo(String displayName, String article, String description) {
            this.displayName = displayName;
            this.article = article;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getArticle() { return article; }
        public String getDescription() { return description; }
    }
}
