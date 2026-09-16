package co.sena.edu.themis.Service;

import co.sena.edu.olympo_back.proto.PersonResponse;
import co.sena.edu.themis.Dto.NoveltyDto;
import co.sena.edu.themis.Dto.NoveltyTypeDto;
import co.sena.edu.themis.Dto.NoveltyStatusDto;
import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Entity.NoveltyStatus;
import co.sena.edu.themis.Entity.NoveltyType;
import co.sena.edu.themis.Repository.NoveltyStatusRepository;
import co.sena.edu.themis.Repository.NoveltyTypeRepository;
import co.sena.edu.themis.Utils.Exception.CustomException;
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
import java.util.List;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String DEFAULT_EMAIL_RECIPIENT = "senathemis@gmail.com";

    @Value("${spring.mail.username}")
    private final String fromEmail;

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;
    private final PersonGrpcService personGrpcService;
    private final StudentGrpcService studentGrpcService;
    private final TeacherGrpcService teacherGrpcService;
    private final AdministrativeGrpcService administrativeGrpcService;
    private final NoveltyTypeRepository noveltyTypeRepository;
    private final NoveltyStatusRepository noveltyStatusRepository; // Agregado: Repositorio para el estado de la novedad

    public EmailService(
            @Value("${spring.mail.username}") String fromEmail,
            JavaMailSender mailSender,
            EmailTemplateService emailTemplateService,
            PersonGrpcService personGrpcService,
            StudentGrpcService studentGrpcService,
            TeacherGrpcService teacherGrpcService,
            AdministrativeGrpcService administrativeGrpcService,
            NoveltyTypeRepository noveltyTypeRepository,
            NoveltyStatusRepository noveltyStatusRepository) {
        this.fromEmail = fromEmail;
        this.mailSender = mailSender;
        this.emailTemplateService = emailTemplateService;
        this.personGrpcService = personGrpcService;
        this.studentGrpcService = studentGrpcService;
        this.teacherGrpcService = teacherGrpcService;
        this.administrativeGrpcService = administrativeGrpcService;
        this.noveltyTypeRepository = noveltyTypeRepository;
        this.noveltyStatusRepository = noveltyStatusRepository;

    }

    /**
     * Envía un correo electrónico HTML a una lista de destinatarios.
     * @param toEmails La lista de direcciones de correo electrónico.
     * @param subject El asunto del correo.
     * @param htmlContent El contenido HTML del cuerpo del correo.
     */
    public void sendHtmlEmailToMultipleRecipients(final List<String> toEmails, final String subject, final String htmlContent) {
        if (toEmails == null || toEmails.isEmpty()) {
            logger.warn("La lista de destinatarios está vacía o es nula. No se enviará ningún correo.");
            return;
        }

        try {
            // Debug: log recipient list and subject before sending
            logger.info("Preparando envío de correo. Asunto='{}'. Destinatarios={}", subject, toEmails);
            if (htmlContent == null || htmlContent.isEmpty()) {
                logger.warn("El contenido HTML del correo está vacío o nulo.");
            } else {
                logger.debug("HTML content length: {}", htmlContent.length());
            }

            if (mailSender == null) {
                logger.error("JavaMailSender no está inicializado (mailSender == null). No se puede enviar correo.");
                throw new CustomException("Mail sender no inicializado", "MAIL_SENDER_NULL", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Ensure we have a valid from address; fallback to DEFAULT_EMAIL_RECIPIENT
            String actualFrom = (fromEmail != null && !fromEmail.trim().isEmpty()) ? fromEmail.trim() : DEFAULT_EMAIL_RECIPIENT;
            if (actualFrom.equals(DEFAULT_EMAIL_RECIPIENT)) {
                logger.warn("Using fallback from address: {}", DEFAULT_EMAIL_RECIPIENT);
            }
            helper.setFrom(actualFrom);
            helper.setTo(toEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Correo enviado exitosamente. Asunto='{}' DestinatariosCount={} Destinatarios={}", subject, toEmails.size(), toEmails);
        } catch (MailException e) {
            logger.error("Error al enviar el correo electrónico: {}", e.getMessage(), e);
            throw new CustomException("Error al enviar correo electrónico", "EMAIL_SEND_FAILURE", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("Ocurrió un error inesperado al configurar el correo: {}", e.getMessage(), e);
            throw new CustomException("Error de configuración de correo", "EMAIL_CONFIGURATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Prepara y envía un correo de notificación para una novedad recién creada.
     * Ahora acepta prefills opcionales para estudiante, docente y administrativo.
     * @param novelty La entidad de la novedad con los datos.
     * @param noveltyTypeDto El DTO para el tipo de novedad.
     * @param studentPrefill Prefill opcional (puede contener nombre/email conocidos desde el frontend).
     * @param teacherPrefill Prefill opcional.
     * @param administrativePrefill Prefill opcional.
     */
    public void sendNoveltyCreationEmail(final Novelty novelty,
                                         final NoveltyTypeDto noveltyTypeDto,
                                         final PersonResponse studentPrefill,
                                         final PersonResponse teacherPrefill,
                                         final PersonResponse administrativePrefill,
                                         final co.sena.edu.themis.Dto.NoveltyDto inputDto) {
        if (novelty == null || noveltyTypeDto == null) {
            logger.warn("No se puede enviar correo de creación de novedad, la entidad Novelty o noveltyTypeDto es nula.");
            return;
        }

        try {
            // Obtener el ID del tipo de novedad de la novedad.
            Long noveltyTypeId = novelty.getNoveltyType().getId();

            // Buscar el tipo de novedad completo por ID en la base de datos.
            Optional<NoveltyType> noveltyTypeOptional = Optional.empty();
            if (noveltyTypeId != null) {
                noveltyTypeOptional = noveltyTypeRepository.findById(noveltyTypeId);
            }

            // Convertir la entidad a DTO y poblar el nombre de la novedad.
            final NoveltyTypeDto fullNoveltyTypeDto = new NoveltyTypeDto();
            fullNoveltyTypeDto.setId(noveltyTypeDto.getId());
            fullNoveltyTypeDto.setNameNovelty(noveltyTypeOptional.isPresent() ? noveltyTypeOptional.get().getNameNovelty() : "Tipo de novedad no especificado");

            // --- INICIO DE CÓDIGO AÑADIDO ---
            // Obtener el ID del estado de novedad
            Long noveltyStatusId = novelty.getNoveltyStatus().getId();

            // Buscar el estado de novedad completo por ID en la base de datos
            Optional<NoveltyStatus> noveltyStatusOptional = Optional.empty();
            if (noveltyStatusId != null) {
                noveltyStatusOptional = noveltyStatusRepository.findById(noveltyStatusId);
            }

            // Convertir la entidad a DTO y poblar el nombre del estado
            final NoveltyStatusDto fullNoveltyStatusDto = new NoveltyStatusDto();
            fullNoveltyStatusDto.setId(noveltyStatusId);
            fullNoveltyStatusDto.setName(noveltyStatusOptional.isPresent() ? noveltyStatusOptional.get().getName() : "N/A");
            // --- FIN DE CÓDIGO AÑADIDO ---

            final NoveltyDto noveltyDto = new NoveltyDto();
            noveltyDto.setId(novelty.getId());
            noveltyDto.setDate(novelty.getDate() != null ? novelty.getDate().toString() : null);
            noveltyDto.setObservation(novelty.getObservation());
            noveltyDto.setJustification(novelty.getJustification());
            noveltyDto.setIsActive(novelty.getIsActive());
            noveltyDto.setNoveltyFiles(novelty.getNoveltyFiles());
            noveltyDto.setStudentId(novelty.getStudentId());
            noveltyDto.setTeacherId(novelty.getTeacherId());
            noveltyDto.setAdministrativeId(novelty.getAdministrativeId());
            noveltyDto.setNoveltyType(fullNoveltyTypeDto); // Usar el DTO con el nombre completo del tipo
            noveltyDto.setNoveltyStatus(fullNoveltyStatusDto); // Agregado: Usar el DTO con el nombre completo del estado

            // --- Rellenar campos de nombre/email en el DTO a partir de los merged para que la plantilla pueda usarlos como fallback ---
            // (Se hace después de calcular merged responses)
            // Obtiene los datos completos de las tres personas desde el servicio gRPC
            final PersonResponse student = fetchPersonWithFallback(novelty.getStudentId(), studentPrefill, "student");
            final PersonResponse teacher = fetchPersonWithFallback(novelty.getTeacherId(), teacherPrefill, "teacher");
            final PersonResponse administrative = fetchPersonWithFallback(novelty.getAdministrativeId(), administrativePrefill, "administrative");

            // Si alguno no fue encontrado por gRPC, usar el prefill si está disponible
            // Merge gRPC result with prefill: prefer non-empty fields from gRPC, fallback to prefill
            PersonResponse studentMerged = mergePersonResponses(student, studentPrefill);
            PersonResponse teacherMerged = mergePersonResponses(teacher, teacherPrefill);
            PersonResponse administrativeMerged = mergePersonResponses(administrative, administrativePrefill);

            // Apply DTO fallback for names (frontend may provide names even if gRPC empty)
            studentMerged = applyDtoFallback(studentMerged, inputDto != null ? inputDto.getStudentName() : null, inputDto != null ? inputDto.getStudentId() : null);
            teacherMerged = applyDtoFallback(teacherMerged, inputDto != null ? inputDto.getTeacherName() : null, inputDto != null ? inputDto.getTeacherId() : null);
            administrativeMerged = applyDtoFallback(administrativeMerged, null, inputDto != null ? inputDto.getAdministrativeId() : null);

            // Rellenar en el DTO los nombres y correos para que la plantilla los utilice si PersonResponse está ausente
            if (studentMerged != null && studentMerged.getFound()) {
                noveltyDto.setStudentName(composeFullName(studentMerged));
                noveltyDto.setStudentEmail(studentMerged.getEmail() != null && !studentMerged.getEmail().isEmpty() ? studentMerged.getEmail() : null);
            }
            if (teacherMerged != null && teacherMerged.getFound()) {
                noveltyDto.setTeacherName(composeFullName(teacherMerged));
                noveltyDto.setTeacherEmail(teacherMerged.getEmail() != null && !teacherMerged.getEmail().isEmpty() ? teacherMerged.getEmail() : null);
            }
            if (administrativeMerged != null && administrativeMerged.getFound()) {
                // NoveltyDto no tiene administrativeName, pero sí administrativeEmail
                noveltyDto.setAdministrativeEmail(administrativeMerged.getEmail() != null && !administrativeMerged.getEmail().isEmpty() ? administrativeMerged.getEmail() : null);
            }

            // Determinar fuente usada para cada persona (gRPC, prefill, default) basándonos en campos útiles
            String studentSource = determinePersonSource(student, studentPrefill, studentMerged);
            String teacherSource = determinePersonSource(teacher, teacherPrefill, teacherMerged);
            String adminSource = determinePersonSource(administrative, administrativePrefill, administrativeMerged);

            logger.debug("Email persona sources -> student: {}, teacher: {}, administrative: {}", studentSource, teacherSource, adminSource);

            // Debug: log merged person contents (id/document/name/email/found) to help diagnose missing data
            if (studentMerged != null) logger.debug("Merged student -> personId={}, document={}, name='{}', lastname='{}', email='{}', found={}", studentMerged.getPersonId(), studentMerged.getDocument(), studentMerged.getName(), studentMerged.getLastname(), studentMerged.getEmail(), studentMerged.getFound());
            if (teacherMerged != null) logger.debug("Merged teacher -> personId={}, document={}, name='{}', lastname='{}', email='{}', found={}", teacherMerged.getPersonId(), teacherMerged.getDocument(), teacherMerged.getName(), teacherMerged.getLastname(), teacherMerged.getEmail(), teacherMerged.getFound());
            if (administrativeMerged != null) logger.debug("Merged administrative -> personId={}, document={}, name='{}', lastname='{}', email='{}', found={}", administrativeMerged.getPersonId(), administrativeMerged.getDocument(), administrativeMerged.getName(), administrativeMerged.getLastname(), administrativeMerged.getEmail(), administrativeMerged.getFound());

            final String subject = "Nueva Novedad Creada - " + (fullNoveltyTypeDto.getNameNovelty() != null ? fullNoveltyTypeDto.getNameNovelty() : "Sin Tipo");

            // Pasa los tres objetos de persona a la plantilla
            // Pasamos además las fuentes para que la plantilla pueda mostrar de dónde viene cada dato (útil para debug)
            final String htmlContent = emailTemplateService.processNoveltyCreationTemplate(noveltyDto, fullNoveltyTypeDto, studentMerged, teacherMerged, administrativeMerged, studentSource, teacherSource, adminSource);

            // Obtiene los correos para el envío
            final List<String> recipientEmails = new ArrayList<>();
            if (studentMerged != null && studentMerged.getFound() && !studentMerged.getEmail().trim().isEmpty()) {
                recipientEmails.add(studentMerged.getEmail().trim());
            }

            if (teacherMerged != null && teacherMerged.getFound() && !teacherMerged.getEmail().trim().isEmpty()) {
                recipientEmails.add(teacherMerged.getEmail().trim());
            }

            if (administrativeMerged != null && administrativeMerged.getFound() && !administrativeMerged.getEmail().trim().isEmpty()) {
                recipientEmails.add(administrativeMerged.getEmail().trim());
            }

            if (recipientEmails.isEmpty()) {
                logger.warn("No se encontraron correos para destinatarios reales; usando destinatario por defecto: {}", DEFAULT_EMAIL_RECIPIENT);
                recipientEmails.add(DEFAULT_EMAIL_RECIPIENT);
            }

            logger.info("Enviando correo con subject='{}' a destinatarios={}", subject, recipientEmails);
            sendHtmlEmailToMultipleRecipients(recipientEmails, subject, htmlContent);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al preparar y enviar correo de notificación de nueva novedad: {}", e.getMessage(), e);
            throw new CustomException("Error al enviar correo de notificación", "EMAIL_NOTIFICATION_FAILURE", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Sobrecarga para mantener compatibilidad con llamadas antiguas que sólo pasaban la novedad y el tipo
    public void sendNoveltyCreationEmail(final Novelty novelty, final NoveltyTypeDto noveltyTypeDto) {
        // Delegar a la firma completa sin prefills ni inputDto
        sendNoveltyCreationEmail(novelty, noveltyTypeDto, null, null, null, null);
    }

    /**
     * Prepara y envía un correo de notificación para una actualización de novedad.
     * @param novelty La entidad de la novedad con los datos actualizados.
     * @param noveltyTypeDto El DTO para el tipo de novedad.
     */
    public void sendNoveltyUpdateEmail(final Novelty novelty, final NoveltyTypeDto noveltyTypeDto) {
        if (novelty == null || noveltyTypeDto == null) {
            logger.warn("No se puede enviar correo de actualización de novedad, la entidad Novelty o noveltyTypeDto es nula.");
            return;
        }

        try {

            Long noveltyTypeId = novelty.getNoveltyType().getId();

            Optional<NoveltyType> noveltyTypeOptional = Optional.empty();
            if (noveltyTypeId != null) {
                noveltyTypeOptional = noveltyTypeRepository.findById(noveltyTypeId);
            }

            final NoveltyTypeDto fullNoveltyTypeDto = new NoveltyTypeDto();
            fullNoveltyTypeDto.setId(noveltyTypeDto.getId());
            fullNoveltyTypeDto.setNameNovelty(noveltyTypeOptional.isPresent() ? noveltyTypeOptional.get().getNameNovelty() : "Tipo de novedad no especificado");

            Long noveltyStatusId = novelty.getNoveltyStatus().getId();

            Optional<NoveltyStatus> noveltyStatusOptional = Optional.empty();
            if (noveltyStatusId != null) {
                noveltyStatusOptional = noveltyStatusRepository.findById(noveltyStatusId);
            }

            final NoveltyStatusDto fullNoveltyStatusDto = new NoveltyStatusDto();
            fullNoveltyStatusDto.setId(noveltyStatusId);
            fullNoveltyStatusDto.setName(noveltyStatusOptional.isPresent() ? noveltyStatusOptional.get().getName() : "N/A");

            final NoveltyDto noveltyDto = new NoveltyDto();
            noveltyDto.setId(novelty.getId());
            noveltyDto.setDate(novelty.getDate() != null ? novelty.getDate().toString() : null);
            noveltyDto.setObservation(novelty.getObservation());
            noveltyDto.setJustification(novelty.getJustification());
            noveltyDto.setIsActive(novelty.getIsActive());
            noveltyDto.setNoveltyFiles(novelty.getNoveltyFiles());
            noveltyDto.setStudentId(novelty.getStudentId());
            noveltyDto.setTeacherId(novelty.getTeacherId());
            noveltyDto.setAdministrativeId(novelty.getAdministrativeId());
            noveltyDto.setNoveltyType(fullNoveltyTypeDto);
            noveltyDto.setNoveltyStatus(fullNoveltyStatusDto);
            // Use same fetch+merge logic as creation to improve reliability
            final PersonResponse student = fetchPersonWithFallback(novelty.getStudentId(), null, "student");
            final PersonResponse teacher = fetchPersonWithFallback(novelty.getTeacherId(), null, "teacher");
            final PersonResponse administrative = fetchPersonWithFallback(novelty.getAdministrativeId(), null, "administrative");

            final PersonResponse studentMerged = mergePersonResponses(student, null);
            final PersonResponse teacherMerged = mergePersonResponses(teacher, null);
            final PersonResponse administrativeMerged = mergePersonResponses(administrative, null);

            // Rellenar en el DTO los nombres y correos para que la plantilla los utilice si PersonResponse está ausente
            if (studentMerged != null && studentMerged.getFound()) {
                noveltyDto.setStudentName(composeFullName(studentMerged));
                noveltyDto.setStudentEmail(studentMerged.getEmail() != null && !studentMerged.getEmail().isEmpty() ? studentMerged.getEmail() : null);
            }
            if (teacherMerged != null && teacherMerged.getFound()) {
                noveltyDto.setTeacherName(composeFullName(teacherMerged));
                noveltyDto.setTeacherEmail(teacherMerged.getEmail() != null && !teacherMerged.getEmail().isEmpty() ? teacherMerged.getEmail() : null);
            }
            if (administrativeMerged != null && administrativeMerged.getFound()) {
                noveltyDto.setAdministrativeEmail(administrativeMerged.getEmail() != null && !administrativeMerged.getEmail().isEmpty() ? administrativeMerged.getEmail() : null);
            }

            final String subject = "Actualización de Novedad - " + (fullNoveltyTypeDto.getNameNovelty() != null ? fullNoveltyTypeDto.getNameNovelty() : "Sin Tipo");

            final String htmlContent = emailTemplateService.processNoveltyUpdateTemplate(noveltyDto, fullNoveltyTypeDto, studentMerged, teacherMerged, administrativeMerged);

            final List<String> recipientEmails = new ArrayList<>();
            if (studentMerged != null && studentMerged.getFound() && !studentMerged.getEmail().trim().isEmpty()) {
                recipientEmails.add(studentMerged.getEmail().trim());
            }
            if (teacherMerged != null && teacherMerged.getFound() && !teacherMerged.getEmail().trim().isEmpty()) {
                recipientEmails.add(teacherMerged.getEmail().trim());
            }
            if (administrativeMerged != null && administrativeMerged.getFound() && !administrativeMerged.getEmail().trim().isEmpty()) {
                recipientEmails.add(administrativeMerged.getEmail().trim());
            }

            if (recipientEmails.isEmpty()) {
                logger.warn("No se encontraron correos para destinatarios reales; usando destinatario por defecto: {}", DEFAULT_EMAIL_RECIPIENT);
                recipientEmails.add(DEFAULT_EMAIL_RECIPIENT);
            }

            logger.info("Enviando correo con subject='{}' a destinatarios={}", subject, recipientEmails);
            sendHtmlEmailToMultipleRecipients(recipientEmails, subject, htmlContent);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al preparar y enviar correo de actualización de novedad: {}", e.getMessage(), e);
            throw new CustomException("Error al enviar correo de actualización", "EMAIL_UPDATE_FAILURE", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Merge fields from gRPC response and prefill: prefer prefill values when provided, otherwise use gRPC
    private PersonResponse mergePersonResponses(PersonResponse grpc, PersonResponse prefill) {
        try {
            co.sena.edu.olympo_back.proto.PersonResponse.Builder b = PersonResponse.newBuilder();
            long personId = 0L;
            // prefer prefill personId if present
            if (prefill != null && prefill.getPersonId() != 0L) personId = prefill.getPersonId();
            else if (grpc != null && grpc.getPersonId() != 0L) personId = grpc.getPersonId();
            if (personId != 0L) b.setPersonId(personId);

            long document = 0L;
            if (prefill != null && prefill.getDocument() != 0L) document = prefill.getDocument();
            else if (grpc != null && grpc.getDocument() != 0L) document = grpc.getDocument();
            if (document != 0L) b.setDocument(document);

            // Prefer prefill name/email/lastname/phone when provided (frontend may carry canonical info)
            String name = (prefill != null && !prefill.getName().isEmpty()) ? prefill.getName() : (grpc != null ? grpc.getName() : "");
            if (name != null && !name.isEmpty()) b.setName(name);

            String lastname = (prefill != null && !prefill.getLastname().isEmpty()) ? prefill.getLastname() : (grpc != null ? grpc.getLastname() : "");
            if (lastname != null && !lastname.isEmpty()) b.setLastname(lastname);

            String email = (prefill != null && !prefill.getEmail().isEmpty()) ? prefill.getEmail() : (grpc != null ? grpc.getEmail() : "");
            if (email != null && !email.isEmpty()) b.setEmail(email);

            String phone = (prefill != null && !prefill.getPhone().isEmpty()) ? prefill.getPhone() : (grpc != null ? grpc.getPhone() : "");
            if (phone != null && !phone.isEmpty()) b.setPhone(phone);

            // mark found true if we have any meaningful data
            boolean found = (b.getPersonId() != 0L) || (b.getDocument() != 0L) || (b.getName() != null && !b.getName().isEmpty()) || (b.getEmail() != null && !b.getEmail().isEmpty());
            b.setFound(found);

            return b.build();
        } catch (Exception e) {
            logger.warn("Error merging person responses: {}", e.getMessage());
            return PersonResponse.newBuilder().setFound(false).build();
        }
    }

    // Determina la fuente usada para una persona basándonos en si prefill aporta datos útiles o gRPC
    private String determinePersonSource(PersonResponse grpcResponse, PersonResponse prefill, PersonResponse merged) {
        if (prefill != null && ((!prefill.getName().isEmpty()) || (!prefill.getEmail().isEmpty()))) return "prefill";
        if (grpcResponse != null && grpcResponse.getFound() && ((!grpcResponse.getName().isEmpty()) || (!grpcResponse.getEmail().isEmpty()))) return "gRPC";
        return (merged != null && merged.getFound()) ? "merged" : "default";
    }

    // Intentar obtener persona por ID y, de no existir, por documento; si ambos fallan, usar prefill
    private PersonResponse fetchPersonWithFallback(Long id, PersonResponse prefill, String role) {
        try {
            if (id == null) {
                logger.debug("No se proporcionó id para {}, usando prefill", role);
                return prefill;
            }

            // Intento por ID
            try {
                PersonResponse byId = personGrpcService.getPersonById(id);
                if (byId != null && byId.getFound()) {
                    logger.debug("PersonService found {} by id={} (email={})", role, id, byId.getEmail());
                    return byId;
                }
                logger.debug("PersonService did not find {} by id={}", role, id);
            } catch (Exception e) {
                logger.warn("Error obteniendo {} por id {}: {}", role, id, e.getMessage());
            }

            // Try role-specific services that may contain embedded Person
            try {
                if ("student".equals(role) && studentGrpcService != null) {
                    var sResp = studentGrpcService.getStudentById(id);
                    if (sResp != null && sResp.getFound() && sResp.getPerson() != null && sResp.getPerson().getFound()) {
                        logger.debug("StudentService provided person for id {} (email={})", id, sResp.getPerson().getEmail());
                        return sResp.getPerson();
                    }
                }
                if ("teacher".equals(role) && teacherGrpcService != null) {
                    var tResp = teacherGrpcService.getTeacherById(id);
                    if (tResp != null && tResp.getFound() && tResp.getCollaborator() != null && tResp.getCollaborator().getPerson() != null && tResp.getCollaborator().getPerson().getFound()) {
                        logger.debug("TeacherService provided person for id {} (email={})", id, tResp.getCollaborator().getPerson().getEmail());
                        return tResp.getCollaborator().getPerson();
                    }
                }
                if ("administrative".equals(role) && administrativeGrpcService != null) {
                    var aResp = administrativeGrpcService.getAdministrativeById(id);
                    if (aResp != null && aResp.getFound() && aResp.getCollaborator() != null && aResp.getCollaborator().getPerson() != null && aResp.getCollaborator().getPerson().getFound()) {
                        logger.debug("AdministrativeService provided person for id {} (email={})", id, aResp.getCollaborator().getPerson().getEmail());
                        return aResp.getCollaborator().getPerson();
                    }
                }
            } catch (Exception e) {
                logger.warn("Error consultando servicios alternativos para {} id {}: {}", role, id, e.getMessage());
            }

            // Intento por documento (fallback) - algunos sistemas usan document como referencia
            try {
                PersonResponse byDocument = personGrpcService.getPersonByDocument(id);
                if (byDocument != null && byDocument.getFound()) {
                    logger.debug("PersonService found {} by document={} (email={})", role, id, byDocument.getEmail());
                    return byDocument;
                }
                logger.debug("PersonService did not find {} by document={}", role, id);
            } catch (Exception e) {
                logger.warn("Error obteniendo {} por documento {}: {}", role, id, e.getMessage());
            }

            // Finalmente, usar prefill
            if (prefill != null) {
                logger.debug("Usando prefill para {} (name={}, email={})", role, prefill.getName(), prefill.getEmail());
                return prefill;
            }

            logger.debug("No se encontró {} por id/document y no hay prefill: {}", role, id);
            return PersonResponse.newBuilder().setFound(false).build();
        } catch (Exception e) {
            logger.error("Error inesperado al intentar obtener datos de persona para {} id={}: {}", role, id, e.getMessage(), e);
            return PersonResponse.newBuilder().setFound(false).build();
        }
    }

    // If merged lacks name but frontend DTO provided a name, apply it
    private PersonResponse applyDtoFallback(PersonResponse merged, String dtoName, Long dtoId) {
        try {
            if ((merged == null || !merged.getFound()) && (dtoName == null || dtoName.isBlank()) && (dtoId == null)) {
                return merged != null ? merged : PersonResponse.newBuilder().setFound(false).build();
            }

            co.sena.edu.olympo_back.proto.PersonResponse.Builder b = PersonResponse.newBuilder();
            if (merged != null) {
                if (merged.getPersonId() != 0L) b.setPersonId(merged.getPersonId());
                if (merged.getDocument() != 0L) b.setDocument(merged.getDocument());
                if (merged.getName() != null && !merged.getName().isEmpty()) b.setName(merged.getName());
                if (merged.getLastname() != null && !merged.getLastname().isEmpty()) b.setLastname(merged.getLastname());
                if (merged.getEmail() != null && !merged.getEmail().isEmpty()) b.setEmail(merged.getEmail());
                if (merged.getPhone() != null && !merged.getPhone().isEmpty()) b.setPhone(merged.getPhone());
            }

            if ((b.getName() == null || b.getName().isEmpty()) && dtoName != null && !dtoName.isBlank()) {
                b.setName(dtoName);
            }
            if (b.getPersonId() == 0L && dtoId != null) {
                b.setPersonId(dtoId);
            }

            boolean found = (b.getPersonId() != 0L) || (b.getDocument() != 0L) || (b.getName() != null && !b.getName().isEmpty()) || (b.getEmail() != null && !b.getEmail().isEmpty());
            b.setFound(found);
            return b.build();
        } catch (Exception e) {
            logger.warn("Error applying DTO fallback to merged person: {}", e.getMessage());
            return merged != null ? merged : PersonResponse.newBuilder().setFound(false).build();
        }
    }

    // Helper para componer el nombre completo desde PersonResponse
    private String composeFullName(PersonResponse p) {
        if (p == null || !p.getFound()) return null;
        StringBuilder sb = new StringBuilder();
        if (p.getName() != null && !p.getName().isBlank()) sb.append(p.getName().trim());
        if (p.getLastname() != null && !p.getLastname().isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.getLastname().trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
