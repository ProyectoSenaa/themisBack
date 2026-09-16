package co.sena.edu.themis.Service;

import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Utils.Exception.CustomException;
import co.sena.edu.olympo_back.proto.PersonResponse;
import co.sena.edu.olympo_back.Administrative.AdministrativeResponse;
import co.sena.edu.olympo_back.Teacher.TeacherResponse;
import co.sena.edu.olympo_back.Student.StudentResponse;
import co.sena.edu.olympo_back.Collaborator.CollaboratorResponse;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
public class EmailCommitteeService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCommitteeService.class);
    private static final String DEFAULT_EMAIL_RECIPIENT = "senathemis@gmail.com";

    private final String fromEmail;

    private final JavaMailSender mailSender;
    private final EmailCommitteeTemplateService emailCommitteeTemplateService;
    private final TeacherGrpcService teacherGrpcService;
    private final AdministrativeGrpcService administrativeGrpcService;
    private final StudentGrpcService studentGrpcService;
    private final PersonGrpcService personGrpcService; // Nuevo: para fallback por personId
    private final CommitteeService committeeService;

    public EmailCommitteeService(
            @Value("${spring.mail.username}") String fromEmail,
            JavaMailSender mailSender,
            EmailCommitteeTemplateService emailCommitteeTemplateService,
            TeacherGrpcService teacherGrpcService,
            AdministrativeGrpcService administrativeGrpcService,
            StudentGrpcService studentGrpcService,
            PersonGrpcService personGrpcService,
            CommitteeService committeeService) {
        this.fromEmail = fromEmail;
        this.mailSender = mailSender;
        this.emailCommitteeTemplateService = emailCommitteeTemplateService;
        this.teacherGrpcService = teacherGrpcService;
        this.administrativeGrpcService = administrativeGrpcService;
        this.studentGrpcService = studentGrpcService;
        this.personGrpcService = personGrpcService;
        this.committeeService = committeeService;
    }

    public void sendHtmlEmailToMultipleRecipients(final List<String> toEmails, final String subject, final String htmlContent) {
        // Normalizar y deduplicar
        Set<String> recipients = new LinkedHashSet<>();
        if (toEmails != null) {
            for (String e : toEmails) {
                if (e == null) continue;
                String t = e.trim();
                if (t.isEmpty()) continue;
                // Validación simple de formato de email
                if (!t.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    logger.warn("Se ignora email inválido: {}", t);
                    continue;
                }
                recipients.add(t);
            }
        }

        if (recipients.isEmpty()) {
            logger.warn("No se encontraron destinatarios válidos; usando destinatario por defecto: {}", DEFAULT_EMAIL_RECIPIENT);
            recipients.add(DEFAULT_EMAIL_RECIPIENT);
        }

        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Usar from configurado o fallback
            String actualFrom = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : DEFAULT_EMAIL_RECIPIENT;
            if (fromEmail == null || fromEmail.isBlank()) {
                logger.warn("spring.mail.username no configurado. Usando from fallback: {}", actualFrom);
            }

            helper.setFrom(actualFrom);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            logger.info("Enviando correo a {} destinatarios: {}", recipients.size(), recipients);
            mailSender.send(message);
            logger.info("Correo enviado exitosamente a {} destinatarios.", recipients.size());
        } catch (MailException e) {
            // No lanzar excepción que rompa el flujo; escribir fallback a disco y loguear
            logger.error("Error al enviar el correo electrónico: {}. Destinatarios: {}. Se escribirá un archivo de fallback.", e.getMessage(), recipients, e);
            try {
                Path dir = Path.of("/tmp/themis_emails");
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                }
                String stamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                String filename = "email_" + stamp.replaceAll(":", "-") + ".html";
                Path file = dir.resolve(filename);
                StringBuilder sb = new StringBuilder();
                sb.append("Subject: ").append(subject).append("\n");
                sb.append("To: ").append(String.join(",", recipients)).append("\n\n");
                sb.append(htmlContent);
                Files.writeString(file, sb.toString(), StandardOpenOption.CREATE_NEW);
                logger.info("Fallback: email escrito a fichero: {}", file.toString());
            } catch (Exception ex) {
                logger.error("No se pudo escribir el email de fallback en disco: {}", ex.getMessage(), ex);
            }
            // No rethrow: evitar romper la operación principal (ej. creación de evento)
        } catch (Exception e) {
            logger.error("Ocurrió un error inesperado al configurar el correo: {}. Destinatarios: {}", e.getMessage(), recipients, e);
            try {
                Path dir = Path.of("/tmp/themis_emails");
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                }
                String stamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                String filename = "email_error_" + stamp.replaceAll(":", "-") + ".html";
                Path file = dir.resolve(filename);
                StringBuilder sb = new StringBuilder();
                sb.append("Subject: ").append(subject).append("\n");
                sb.append("To: ").append(String.join(",", recipients)).append("\n\n");
                sb.append(htmlContent);
                Files.writeString(file, sb.toString(), StandardOpenOption.CREATE_NEW);
                logger.info("Fallback: email escrito a fichero (error path): {}", file.toString());
            } catch (Exception ex) {
                logger.error("No se pudo escribir el email de fallback en disco (error path): {}", ex.getMessage(), ex);
            }
            // No rethrow
        }
    }

    public void sendCommitteeEventCreationEmail(final CommitteeEventDto committeeEventDto) {
        if (committeeEventDto == null || committeeEventDto.getCommittee() == null) {
            logger.warn("No se puede enviar correo de registro de evento de comité, el DTO o el comité asociado es nulo.");
            return;
        }

        try {
            // Intentar obtener listas de IDs desde el DTO
            List<Long> studentsIds = committeeEventDto.getCommittee().getStudentsIds();
            List<Long> teachersIds = committeeEventDto.getCommittee().getTeachersIds();
            List<Long> administrativesIds = committeeEventDto.getCommittee().getAdministrativesIds();

            // Fallback: si no vienen en el input, recuperar el comité desde BD y usar sus listas
            if ((studentsIds == null || studentsIds.isEmpty()) ||
                (teachersIds == null || teachersIds.isEmpty()) ||
                (administrativesIds == null || administrativesIds.isEmpty())) {
                try {
                    Long committeeId = committeeEventDto.getCommittee().getId();
                    if (committeeId != null) {
                        var committee = committeeService.getById(committeeId);
                        if (studentsIds == null || studentsIds.isEmpty()) studentsIds = committee.getStudentsIds();
                        if (teachersIds == null || teachersIds.isEmpty()) teachersIds = committee.getTeachersIds();
                        if (administrativesIds == null || administrativesIds.isEmpty()) administrativesIds = committee.getAdministrativesIds();
                    } else {
                        logger.warn("El DTO del comité no contiene ID, no es posible recuperar listas de participantes desde BD.");
                    }
                } catch (Exception ex) {
                    logger.error("No fue posible recuperar el comité para completar los IDs de participantes: {}", ex.getMessage());
                }
            }

            // Llamadas a los servicios gRPC para obtener las listas de respuestas
            List<StudentResponse> students = getStudentsData(studentsIds);
            List<TeacherResponse> teachers = getTeachersData(teachersIds);
            List<AdministrativeResponse> administratives = getAdministrativesData(administrativesIds);

            final String htmlContent = emailCommitteeTemplateService.processCommitteeEventTemplate(
                    committeeEventDto,
                    students,
                    teachers,
                    administratives
            );

            final Set<String> recipientSet = new LinkedHashSet<>();
            addEmailsFromStudents(students, recipientSet);
            addEmailsFromTeachers(teachers, recipientSet);
            addEmailsFromAdministratives(administratives, recipientSet);

            final List<String> recipientEmails = new ArrayList<>(recipientSet);

            logger.debug("Destinatarios a notificar ({}): {}", recipientEmails.size(), recipientEmails);

            if (recipientEmails.isEmpty()) {
                recipientEmails.add(DEFAULT_EMAIL_RECIPIENT);
            }

            final String subject = "Nuevo Evento de Comité Programado";
            sendHtmlEmailToMultipleRecipients(recipientEmails, subject, htmlContent);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al preparar y enviar correo de registro de evento de comité: {}", e.getMessage(), e);
            throw new CustomException("Error al enviar correo de notificación de evento de comité", "EMAIL_COMMITTEE_EVENT_FAILURE", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void sendCommitteeEventReminderEmail(final CommitteeEventDto committeeEventDto) {
        if (committeeEventDto == null || committeeEventDto.getCommittee() == null) {
            logger.warn("No se puede enviar correo de recordatorio, el DTO o el comité asociado es nulo.");
            return;
        }
        try {
            List<Long> studentsIds = committeeEventDto.getCommittee().getStudentsIds();
            List<Long> teachersIds = committeeEventDto.getCommittee().getTeachersIds();
            List<Long> administrativesIds = committeeEventDto.getCommittee().getAdministrativesIds();

            if ((studentsIds == null || studentsIds.isEmpty()) ||
                (teachersIds == null || teachersIds.isEmpty()) ||
                (administrativesIds == null || administrativesIds.isEmpty())) {
                try {
                    Long committeeId = committeeEventDto.getCommittee().getId();
                    if (committeeId != null) {
                        var committee = committeeService.getById(committeeId);
                        if (studentsIds == null || studentsIds.isEmpty()) studentsIds = committee.getStudentsIds();
                        if (teachersIds == null || teachersIds.isEmpty()) teachersIds = committee.getTeachersIds();
                        if (administrativesIds == null || administrativesIds.isEmpty()) administrativesIds = committee.getAdministrativesIds();
                    }
                } catch (Exception ex) {
                    logger.error("No fue posible recuperar el comité para completar los IDs de participantes (recordatorio): {}", ex.getMessage());
                }
            }

            List<StudentResponse> students = getStudentsData(studentsIds);
            List<TeacherResponse> teachers = getTeachersData(teachersIds);
            List<AdministrativeResponse> administratives = getAdministrativesData(administrativesIds);

            final String htmlContent = emailCommitteeTemplateService.processCommitteeEventTemplate(
                    committeeEventDto,
                    students,
                    teachers,
                    administratives
            );

            final Set<String> recipientSet = new LinkedHashSet<>();
            addEmailsFromStudents(students, recipientSet);
            addEmailsFromTeachers(teachers, recipientSet);
            addEmailsFromAdministratives(administratives, recipientSet);

            final List<String> recipientEmails = new ArrayList<>(recipientSet);

            if (recipientEmails.isEmpty()) {
                recipientEmails.add(DEFAULT_EMAIL_RECIPIENT);
            }

            final String subject = "Recordatorio: Evento de Comité";
            sendHtmlEmailToMultipleRecipients(recipientEmails, subject, htmlContent);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al enviar correo de recordatorio de evento de comité: {}", e.getMessage(), e);
            throw new CustomException("Error al enviar correo de recordatorio", "EMAIL_COMMITTEE_REMINDER_FAILURE", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<StudentResponse> getStudentsData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<StudentResponse> result = ids.stream()
                .map(id -> {
                    try {
                        StudentResponse student = studentGrpcService.getStudentById(id);
                        if (student != null && student.getFound()) {
                            return student;
                        }
                        PersonResponse person = personGrpcService.getPersonById(id);
                        if (person != null && person.getFound()) {
                            return StudentResponse.newBuilder()
                                    .setId(id)
                                    .setFound(true)
                                    .setPerson(person)
                                    .build();
                        }
                        // Respuesta vacía para que al menos se muestre N/A
                        return StudentResponse.newBuilder().setId(id).setFound(true).build();
                    } catch (Exception e) {
                        logger.error("Error obteniendo datos del estudiante con ID {}: {}", id, e.getMessage());
                        return StudentResponse.newBuilder().setId(id).setFound(true).build();
                    }
                })
                .collect(Collectors.toList());
        logger.info("Estudiantes recopilados para correo: {}", result.size());
        return result;
    }

    private List<TeacherResponse> getTeachersData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<TeacherResponse> result = ids.stream()
                .map(id -> {
                    try {
                        TeacherResponse teacher = teacherGrpcService.getTeacherById(id);
                        // Si lo encontró pero no trae persona, intentar completar con PersonService
                        if (teacher != null && teacher.getFound()) {
                            boolean hasPerson = teacher.hasCollaborator() && teacher.getCollaborator().hasPerson() && teacher.getCollaborator().getPerson().getFound();
                            if (!hasPerson) {
                                PersonResponse person = personGrpcService.getPersonById(id);
                                if (person != null && person.getFound()) {
                                    CollaboratorResponse collaborator = CollaboratorResponse.newBuilder()
                                            .setPerson(person)
                                            .setFound(true)
                                            .build();
                                    return TeacherResponse.newBuilder(teacher)
                                            .setCollaborator(collaborator)
                                            .setFound(true)
                                            .setId(id)
                                            .build();
                                }
                            }
                            return teacher;
                        }

                        // Fallback completo: usar PersonService
                        PersonResponse person = personGrpcService.getPersonById(id);
                        if (person != null && person.getFound()) {
                            CollaboratorResponse collaborator = CollaboratorResponse.newBuilder()
                                    .setPerson(person)
                                    .setFound(true)
                                    .build();
                            return TeacherResponse.newBuilder()
                                    .setFound(true)
                                    .setId(id)
                                    .setCollaborator(collaborator)
                                    .build();
                        }
                        // Respuesta vacía
                        return TeacherResponse.newBuilder().setId(id).setFound(true).build();
                    } catch (Exception e) {
                        logger.error("Error obteniendo datos del profesor con ID {}: {}", id, e.getMessage());
                        return TeacherResponse.newBuilder().setId(id).setFound(true).build();
                    }
                })
                .collect(Collectors.toList());
        logger.info("Instructores recopilados para correo: {}", result.size());
        return result;
    }

    private List<AdministrativeResponse> getAdministrativesData(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<AdministrativeResponse> result = ids.stream()
                .map(id -> {
                    try {
                        AdministrativeResponse administrative = administrativeGrpcService.getAdministrativeById(id);
                        // Si lo encontró pero no trae persona, intentar completar con PersonService
                        if (administrative != null && administrative.getFound()) {
                            boolean hasPerson = administrative.hasCollaborator() && administrative.getCollaborator().hasPerson() && administrative.getCollaborator().getPerson().getFound();
                            if (!hasPerson) {
                                PersonResponse person = personGrpcService.getPersonById(id);
                                if (person != null && person.getFound()) {
                                    CollaboratorResponse collaborator = CollaboratorResponse.newBuilder()
                                            .setPerson(person)
                                            .setFound(true)
                                            .build();
                                    return AdministrativeResponse.newBuilder(administrative)
                                            .setCollaborator(collaborator)
                                            .setFound(true)
                                            .setId(id)
                                            .build();
                                }
                            }
                            return administrative;
                        }

                        // Fallback completo: usar PersonService
                        PersonResponse person = personGrpcService.getPersonById(id);
                        if (person != null && person.getFound()) {
                            CollaboratorResponse collaborator = CollaboratorResponse.newBuilder()
                                    .setPerson(person)
                                    .setFound(true)
                                    .build();
                            return AdministrativeResponse.newBuilder()
                                    .setFound(true)
                                    .setId(id)
                                    .setCollaborator(collaborator)
                                    .build();
                        }
                        // Respuesta vacía
                        return AdministrativeResponse.newBuilder().setId(id).setFound(true).build();
                    } catch (Exception e) {
                        logger.error("Error obteniendo datos del administrativo con ID {}: {}", id, e.getMessage());
                        return AdministrativeResponse.newBuilder().setId(id).setFound(true).build();
                    }
                })
                .collect(Collectors.toList());
        logger.info("Administrativos recopilados para correo: {}", result.size());
        return result;
    }

    private void addEmailsFromStudents(List<StudentResponse> students, Set<String> emailRecipients) {
        if (students != null) {
            students.forEach(student -> {
                try {
                    if (student != null && student.getFound() && student.getPerson() != null) {
                        String email = student.getPerson().getEmail();
                        if (email != null && !email.trim().isEmpty()) {
                            emailRecipients.add(email.trim());
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("No se pudo extraer email de un estudiante: {}", ex.getMessage());
                }
            });
        }
    }

    private void addEmailsFromTeachers(List<TeacherResponse> teachers, Set<String> emailRecipients) {
        if (teachers != null) {
            teachers.forEach(teacher -> {
                try {
                    if (teacher != null && teacher.getFound() && teacher.getCollaborator() != null && teacher.getCollaborator().getPerson() != null) {
                        String email = teacher.getCollaborator().getPerson().getEmail();
                        if (email != null && !email.trim().isEmpty()) {
                            emailRecipients.add(email.trim());
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("No se pudo extraer email de un instructor: {}", ex.getMessage());
                }
            });
        }
    }

    private void addEmailsFromAdministratives(List<AdministrativeResponse> administratives, Set<String> emailRecipients) {
        if (administratives != null) {
            administratives.forEach(administrative -> {
                try {
                    if (administrative != null && administrative.getFound() && administrative.getCollaborator() != null && administrative.getCollaborator().getPerson() != null) {
                        String email = administrative.getCollaborator().getPerson().getEmail();
                        if (email != null && !email.trim().isEmpty()) {
                            emailRecipients.add(email.trim());
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("No se pudo extraer email de un administrativo: {}", ex.getMessage());
                }
            });
        }
    }
}
