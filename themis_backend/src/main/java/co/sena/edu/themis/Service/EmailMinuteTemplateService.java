package co.sena.edu.themis.Service;

import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.olympo_back.proto.PersonResponse;
import co.sena.edu.olympo_back.Administrative.AdministrativeResponse;
import co.sena.edu.olympo_back.Teacher.TeacherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EmailMinuteTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(EmailMinuteTemplateService.class);

    public String buildMinuteFromEntities(CommitteeEventDto event, List<co.sena.edu.themis.Entity.Novelty> novelties) throws IOException {
        Map<String,String> m = new java.util.HashMap<>();
        if (event != null) {
            m.put("committeeId", event.getCommittee()!=null && event.getCommittee().getId()!=null ? String.valueOf(event.getCommittee().getId()) : "");
            m.put("eventId", event.getId()!=null? String.valueOf(event.getId()):"");
            m.put("eventDate", event.getDate()!=null? event.getDate():"");
            m.put("eventHour", event.getHour()!=null? event.getHour():"");
            m.put("eventSession", event.getSession()!=null? event.getSession():"");
            m.put("coordinationName", event.getCoordinationName()!=null? event.getCoordinationName():"");
        }

        StringBuilder novRows = new StringBuilder();
        int i = 1;
        if (novelties != null) {
            for (co.sena.edu.themis.Entity.Novelty n : novelties) {
                String tipo = n.getNoveltyType()!=null? safeHtml(n.getNoveltyType().getNameNovelty()): "";
                String detalle = "";
                if (n.getNoveltyType()!=null) {
                    if (n.getNoveltyType().getDescription()!=null && !n.getNoveltyType().getDescription().isBlank()) {
                        detalle = safeHtml(n.getNoveltyType().getDescription());
                    } else if (n.getNoveltyType().getProcedureDescription()!=null) {
                        detalle = safeHtml(n.getNoveltyType().getProcedureDescription());
                    }
                }
                String estado = n.getNoveltyStatus()!=null? safeHtml(n.getNoveltyStatus().getName()): "";
                String just = n.getJustification()!=null? safeHtml(n.getJustification()): "";
                String obs = n.getObservation()!=null? safeHtml(n.getObservation()): "";
                novRows.append("<tr>")
                        .append("<td>").append(i++).append("</td>")

                        .append("<td>").append(tipo).append("</td>")
                        .append("<td>").append(detalle).append("</td>")
                        .append("<td>").append(estado).append("</td>")
                        .append("<td>").append(just).append("</td>")
                        .append("<td>").append(obs).append("</td>")
                        .append("</tr>");
            }
        }
        if (novRows.length()==0) novRows.append("<tr><td colspan='6' class='empty'>Sin novedades</td></tr>");
        m.put("noveltiesRows", novRows.toString());
        m.putIfAbsent("observacionesHtml", "");

        m.putIfAbsent("teachersRows", "<tr><td colspan='3' class='empty'>Sin docentes</td></tr>");
        m.putIfAbsent("administrativesRows", "<tr><td colspan='3' class='empty'>Sin administrativos</td></tr>");
        m.putIfAbsent("studentsRows", "<tr><td colspan='3' class='empty'>Sin aprendices</td></tr>");
        return processMinuteTemplate(null, null, null, m);
    }

    public String processMinuteTemplate(
            CommitteeEventDto event,
            List<TeacherResponse> teachers,
            List<AdministrativeResponse> administratives,
            Map<String, String> sectionsContent
    ) throws IOException {
        logger.info("[TEMPLATE] Procesando plantilla HTML actualizada con estructura completa");
        System.out.println("[TEMPLATE] Procesando plantilla HTML actualizada con estructura completa");

        String template = loadTemplate("templates/Email/minute.html");
        if (sectionsContent == null) sectionsContent = new java.util.HashMap<>();

        // Placeholders originales
        String committeeId = sectionsContent.getOrDefault("committeeId", "");
        String eventId = sectionsContent.getOrDefault("eventId", "");
        String eventDate = sectionsContent.getOrDefault("eventDate", "");
        String eventHour = sectionsContent.getOrDefault("eventHour", "");
        String eventSession = sectionsContent.getOrDefault("eventSession", "");
        String coordinationName = sectionsContent.getOrDefault("coordinationName", "");
        String teachersRows = sectionsContent.getOrDefault("teachersRows", "");
        String administrativesRows = sectionsContent.getOrDefault("administrativesRows", "");
        String studentsRows = sectionsContent.getOrDefault("studentsRows", "");
        String noveltiesRows = sectionsContent.getOrDefault("noveltiesRows", "");
        String observacionesHtml = sectionsContent.getOrDefault("observacionesHtml", sectionsContent.getOrDefault("conclusionesHtml", ""));

        // Nuevos placeholders de la plantilla actualizada
        String actaNumero = sectionsContent.getOrDefault("actaNumero", "001");
        String actaAnio = sectionsContent.getOrDefault("actaAnio", String.valueOf(java.time.LocalDate.now().getYear()));
        String nombreReunion = sectionsContent.getOrDefault("nombreReunion", "COMITÉ ACADÉMICO");
        String eventCity = sectionsContent.getOrDefault("eventCity", "Medellín");
        String eventHourInicio = sectionsContent.getOrDefault("eventHourInicio", eventHour);
        String eventHourFin = sectionsContent.getOrDefault("eventHourFin", "");
        String eventPlace = sectionsContent.getOrDefault("eventPlace", "Aula Virtual/Presencial");
        String eventLocation = sectionsContent.getOrDefault("eventLocation", "SENA - Centro de Tecnología e Innovación");
        String meetingObjectives = sectionsContent.getOrDefault("meetingObjectives", "Evaluar y resolver las novedades académicas de los aprendices");
        String trasladoConformidad = sectionsContent.getOrDefault("trasladoConformidad", "conformidad del coordinador académico correspondiente.");
        String conclusionesHtml = sectionsContent.getOrDefault("conclusionesHtml", "Conclusiones del comité académico.");

        // Placeholders para las nuevas tablas
        String reingresosRows = sectionsContent.getOrDefault("reingresosRows", "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin reingresos aprobados</td></tr>");
        String retirosAprobadosRows = sectionsContent.getOrDefault("retirosAprobadosRows", "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin retiros voluntarios aprobados</td></tr>");
        String retirosNoAprobadosRows = sectionsContent.getOrDefault("retirosNoAprobadosRows", "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin retiros voluntarios no aprobados</td></tr>");
        String trasladosAprobadosRows = sectionsContent.getOrDefault("trasladosAprobadosRows", "<tr><td colspan='9' style='text-align: center; font-style: italic;'>Sin traslados aprobados</td></tr>");
        String trasladosNoAprobadosRows = sectionsContent.getOrDefault("trasladosNoAprobadosRows", "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin traslados no aprobados</td></tr>");
        String aplazamientosRows = sectionsContent.getOrDefault("aplazamientosRows", "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin aplazamientos rechazados</td></tr>");
        String levantamientosRows = sectionsContent.getOrDefault("levantamientosRows", "<tr><td colspan='8' style='text-align: center; font-style: italic;'>Sin levantamientos de condicionamiento</td></tr>");
        String compromisosRows = sectionsContent.getOrDefault("compromisosRows", "<tr><td colspan='4' style='text-align: center; font-style: italic;'>Sin compromisos establecidos</td></tr>");
        String asistentesRows = sectionsContent.getOrDefault("asistentesRows", "<tr><td colspan='5' style='text-align: center; font-style: italic;'>Sin asistentes registrados</td></tr>");

        if (teachersRows.isBlank() && teachers != null) {
            StringBuilder sb = new StringBuilder();
            int idx=1; for (TeacherResponse t: teachers){
                String name = extractPersonName(t!=null && t.hasCollaborator()? t.getCollaborator().getPerson(): null);
                String doc = (t!=null && t.hasCollaborator() && t.getCollaborator().getPerson()!=null)? String.valueOf(t.getCollaborator().getPerson().getDocument()):"";
                sb.append("<tr><td>").append(idx++).append("</td><td>").append(escape(name)).append("</td><td>").append(escape(doc)).append("</td></tr>");
            }
            if (sb.length()==0) sb.append("<tr><td colspan='3' class='empty'>Sin docentes</td></tr>");
            teachersRows = sb.toString();
        }
        if (administrativesRows.isBlank() && administratives != null) {
            StringBuilder sb = new StringBuilder();
            int idx=1; for (AdministrativeResponse a: administratives){
                String name = extractPersonName(a!=null && a.hasCollaborator()? a.getCollaborator().getPerson(): null);
                String doc = (a!=null && a.hasCollaborator() && a.getCollaborator().getPerson()!=null)? String.valueOf(a.getCollaborator().getPerson().getDocument()):"";
                sb.append("<tr><td>").append(idx++).append("</td><td>").append(escape(name)).append("</td><td>").append(escape(doc)).append("</td></tr>");
            }
            if (sb.length()==0) sb.append("<tr><td colspan='3' class='empty'>Sin administrativos</td></tr>");
            administrativesRows = sb.toString();
        }

        // Reemplazar TODOS los placeholders de la nueva plantilla
        String html = template
                // Placeholders originales
                .replace("${committeeId}", committeeId)
                .replace("${eventId}", eventId)
                .replace("${eventDate}", eventDate)
                .replace("${eventHour}", eventHour)
                .replace("${eventSession}", eventSession)
                .replace("${coordinationName}", coordinationName)
                .replace("${teachersRows}", teachersRows)
                .replace("${administrativesRows}", administrativesRows)
                .replace("${studentsRows}", studentsRows)
                .replace("${noveltiesRows}", noveltiesRows)
                .replace("${observacionesHtml}", observacionesHtml)

                // Nuevos placeholders de la plantilla actualizada
                .replace("${actaNumero}", actaNumero)
                .replace("${actaAnio}", actaAnio)
                .replace("${nombreReunion}", nombreReunion)
                .replace("${eventCity}", eventCity)
                .replace("${eventHourInicio}", eventHourInicio)
                .replace("${eventHourFin}", eventHourFin)
                .replace("${eventPlace}", eventPlace)
                .replace("${eventLocation}", eventLocation)
                .replace("${meetingObjectives}", meetingObjectives)
                .replace("${trasladoConformidad}", trasladoConformidad)
                .replace("${conclusionesHtml}", conclusionesHtml)
                .replace("${reingresosRows}", reingresosRows)
                .replace("${retirosAprobadosRows}", retirosAprobadosRows)
                .replace("${retirosNoAprobadosRows}", retirosNoAprobadosRows)
                .replace("${trasladosAprobadosRows}", trasladosAprobadosRows)
                .replace("${trasladosNoAprobadosRows}", trasladosNoAprobadosRows)
                .replace("${aplazamientosRows}", aplazamientosRows)
                .replace("${levantamientosRows}", levantamientosRows)
                .replace("${compromisosRows}", compromisosRows)
                .replace("${asistentesRows}", asistentesRows);

        // Verificar si quedaron placeholders sin reemplazar
        int placeholdersRestantes = countMatches(html, "${");
        if (placeholdersRestantes > 0) {
            logger.warn("[TEMPLATE] Quedaron {} placeholders sin reemplazar en minute.html", placeholdersRestantes);
            System.out.println("[TEMPLATE] WARNING: Quedaron " + placeholdersRestantes + " placeholders sin reemplazar");
        } else {
            logger.info("[TEMPLATE] Todos los placeholders fueron reemplazados correctamente");
            System.out.println("[TEMPLATE] ✅ Todos los placeholders reemplazados correctamente");
        }

        logger.info("[TEMPLATE] HTML procesado. Tamaño final: {} caracteres", html.length());
        System.out.println("[TEMPLATE] HTML procesado. Tamaño final: " + html.length() + " caracteres");

        return html;
    }

    private int countMatches(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    private String safeHtml(String v){ if(v==null) return ""; return escape(v.trim()); }

    private String buildAsistentesRows(List<TeacherResponse> teachers, List<AdministrativeResponse> administratives) {
        StringBuilder sb = new StringBuilder();
        if (teachers != null) {
            for (TeacherResponse t : teachers) {
                String name = extractPersonName(t != null && t.hasCollaborator() ? t.getCollaborator().getPerson() : null);
                sb.append("<tr>")
                        .append("<td>").append(escape(name)).append("</td>")
                        .append("<td>Docente</td>")
                        .append("<td></td><td></td><td></td>")
                        .append("</tr>");
            }
        }
        if (administratives != null) {
            for (AdministrativeResponse a : administratives) {
                String name = extractPersonName(a != null && a.hasCollaborator() ? a.getCollaborator().getPerson() : null);
                sb.append("<tr>")
                        .append("<td>").append(escape(name)).append("</td>")
                        .append("<td>Administrativo</td>")
                        .append("<td></td><td></td><td></td>")
                        .append("</tr>");
            }
        }
        if (sb.length() == 0) {
            sb.append("<tr><td colspan=\"5\">Sin asistentes</td></tr>");
        }
        return sb.toString();
    }

    private String extractPersonName(PersonResponse p) {
        if (p == null) return "";
        String name = p.getName();
        String last = p.getLastname();
        if ((name == null || name.isBlank()) && (last == null || last.isBlank())) return "";
        if (name == null || name.isBlank()) return last != null ? last : "";
        if (last == null || last.isBlank()) return name != null ? name : "";
        return name + " " + last;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String getOrDefault(Map<String, String> map, String key, String def) {
        if (map == null) return def;
        String v = map.get(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private String loadTemplate(String templatePath) throws IOException {
        logger.info("[TEMPLATE] Cargando plantilla HTML desde: {}", templatePath);
        System.out.println("[TEMPLATE] Cargando plantilla HTML desde: " + templatePath);

        ClassPathResource resource = new ClassPathResource(templatePath);
        if (!resource.exists()) {
            logger.error("[TEMPLATE] La plantilla no se encontró: {}", templatePath);
            System.out.println("[TEMPLATE] ERROR: La plantilla no se encontró: " + templatePath);
            throw new IOException("La plantilla no se encontró: " + templatePath);
        }

        // Forzar recarga del archivo para que los cambios se reflejen inmediatamente
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        logger.info("[TEMPLATE] Plantilla cargada exitosamente. Tamaño: {} caracteres", content.length());
        System.out.println("[TEMPLATE] Plantilla cargada exitosamente. Tamaño: " + content.length() + " caracteres");

        return content;
    }
}
