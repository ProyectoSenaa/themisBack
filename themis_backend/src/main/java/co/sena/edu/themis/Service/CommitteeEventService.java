package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.Committee;
import co.sena.edu.themis.Entity.CommitteeEvent;
import co.sena.edu.themis.Repository.CommitteeEventRepository;
import co.sena.edu.themis.Service.Dao.Idao;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class CommitteeEventService implements Idao<CommitteeEvent, Long> {

    private final CommitteeEventRepository committeeEventRepository;
    private final CommitteeService committeeService;

    @Autowired
    public CommitteeEventService(CommitteeEventRepository committeeEventRepository, CommitteeService committeeService) {
        this.committeeEventRepository = committeeEventRepository;
        this.committeeService = committeeService;
    }

    // Métodos básicos del Idao
    @Override
    public List<CommitteeEvent> findAll() {
        return committeeEventRepository.findAll();
    }

    @Override
    public CommitteeEvent getById(Long id) {
        // 📊 LOGGING: Búsqueda de evento por ID
        System.out.println("🔍 [SERVICE] Buscando CommitteeEvent con ID: " + id);

        Optional<CommitteeEvent> eventOptional = committeeEventRepository.findById(id);
        if (eventOptional.isPresent()) {
            CommitteeEvent event = eventOptional.get();
            System.out.println("✅ [SERVICE] Evento encontrado:");
            System.out.println("  - ID: " + event.getId());
            System.out.println("  - Fecha: " + event.getDate());
            System.out.println("  - Hora: " + event.getHour());
            System.out.println("  - Sesión: " + event.getSession());
            System.out.println("  - Tiene comité: " + (event.getCommittee() != null));
            if (event.getCommittee() != null) {
                System.out.println("  - ID del comité: " + event.getCommittee().getId());
            }
            return event;
        } else {
            System.out.println("❌ [SERVICE] Evento NO encontrado con ID: " + id);
            throw new IllegalArgumentException("Committee Event not found with id: " + id);
        }
    }

    @Override
    @Transactional
    public CommitteeEvent save(CommitteeEvent committeeEvent) {
        validateCommitteeEvent(committeeEvent);

        if (committeeEvent.getCommittee() != null && committeeEvent.getCommittee().getId() != null) {
            Committee committee = committeeService.getById(committeeEvent.getCommittee().getId());
            if (!committee.isActive()) {
                throw new IllegalArgumentException("Cannot schedule meetings for inactive committees");
            }
        }
        return committeeEventRepository.save(committeeEvent);
    }

    @Transactional
    public CommitteeEvent markFinished(Long eventId) {
        CommitteeEvent event = getById(eventId);
        event.setFinishedAt(LocalDateTime.now());
        return committeeEventRepository.save(event);
    }

    @Override
    public void deleteById(Long id) {
        committeeEventRepository.deleteById(id);
    }

    @Override
    public Page<CommitteeEvent> findAll(Pageable pageable) {
        return committeeEventRepository.findAll(pageable);
    }

    public Optional<CommitteeEvent> findById(Long id) {
        return committeeEventRepository.findById(id);
    }

    public boolean existsById(Long id) {
        return committeeEventRepository.existsById(id);
    }

    public List<CommitteeEvent> getCommitteeMeetings(Long committeeId) {
        committeeService.getById(committeeId);
        return committeeEventRepository.findAllByCommitteeId(committeeId);
    }

    /**
     * Obtener reuniones de un comité en una fecha específica
     */
    public List<CommitteeEvent> getCommitteeMeetingsByDate(Long committeeId, LocalDate date) {
        committeeService.getById(committeeId);
        return committeeEventRepository.findAllByCommitteeIdAndDate(committeeId, date);
    }

    /**
     * Obtener próximas reuniones de un comité
     */
    public List<CommitteeEvent> getUpcomingCommitteeMeetings(Long committeeId) {
        committeeService.getById(committeeId);
        return committeeEventRepository.findUpcomingEventsByCommittee(committeeId, LocalDate.now());
    }

    public List<CommitteeEvent> getTodayCommitteeMeetings(Long committeeId) {
        committeeService.getById(committeeId);
        return committeeEventRepository.findCommitteeEventsByDateOrderByTime(committeeId, LocalDate.now());
    }

    @Transactional
    public CommitteeEvent scheduleCommitteeMeeting(CommitteeEvent meeting) {
        // Validación añadida para el objeto 'meeting'
        if (meeting == null || meeting.getCommittee() == null || meeting.getCommittee().getId() == null) {
            throw new IllegalArgumentException("Cannot schedule a meeting for a non-existent or inactive committee");
        }

        // Validar que el comité existe y está activo
        Committee committee = committeeService.getById(meeting.getCommittee().getId());
        if (!committee.isActive()) {
            throw new IllegalArgumentException("Cannot schedule meetings for inactive committee: " );
        }

        // Validaciones de reglas de negocio
        validateCommitteeEvent(meeting);

        return save(meeting);
    }

    /**
     * Obtener reuniones de comités activos para hoy
     */
    public List<CommitteeEvent> getTodayActiveMeetings() {
        return committeeEventRepository.findEventsByDateOrderByTime(LocalDate.now())
                .stream()
                .filter(meeting -> meeting.getCommittee() != null && meeting.getCommittee().isActive())
                .toList();
    }

    public List<CommitteeEvent> getMeetingsForPerson(Long personId) {
        // Obtener comités donde participa la persona
        List<Committee> userCommittees = committeeService.findCommitteesByStudentId(personId);
        userCommittees.addAll(committeeService.findCommitteesByTeacherId(personId));
        userCommittees.addAll(committeeService.findCommitteesByAdministrativeId(personId));

        // Obtener IDs únicos de los comités
        List<Long> committeeIds = userCommittees.stream()
                .map(Committee::getId)
                .distinct()
                .toList();

        if (committeeIds.isEmpty()) {
            return List.of();
        }

        return committeeEventRepository.findEventsByCommitteeIds(committeeIds);
    }

    /**
     * Obtener próximas reuniones para una persona específica
     */
    public List<CommitteeEvent> getUpcomingMeetingsForPerson(Long personId) {
        return getMeetingsForPerson(personId).stream()
                .filter(meeting -> !meeting.getDate().isBefore(LocalDate.now()))
                .toList();
    }

    public Long countCommitteeMeetings(Long committeeId) {
        committeeService.getById(committeeId); // Validar que existe
        return committeeEventRepository.countEventsByCommittee(committeeId);
    }

    /**
     * Obtener reuniones de la semana actual
     */
    public List<CommitteeEvent> getThisWeekMeetings() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        return committeeEventRepository.findEventsInWeek(startOfWeek, endOfWeek);
    }

    // Validaciones de colisiones (fecha+hora) por comité
    public boolean existsEventCollision(Long committeeId, LocalDate date, Time hour) {
        if (committeeId == null || date == null || hour == null) return false;
        return committeeEventRepository.existsByCommitteeAndDateAndHour(committeeId, date, hour);
    }

    public boolean existsEventCollisionExcluding(Long eventId, Long committeeId, LocalDate date, Time hour) {
        if (eventId == null || committeeId == null || date == null || hour == null) return false;
        return committeeEventRepository.existsAnotherByCommitteeAndDateAndHour(eventId, committeeId, date, hour);
    }

    /**
     * Obtener todos los eventos a partir de hoy (inclusive)
     */
    public List<CommitteeEvent> getUpcomingMeetingsAll() {
        return committeeEventRepository.findAllByDateGreaterThanEqual(LocalDate.now());
    }

    /**
     * Obtener eventos sin comité asignado
     */
    public List<CommitteeEvent> findEventsWithoutCommittee() {
        return committeeEventRepository.findByCommitteeIsNull();
    }

    /**
     * Método optimizado para obtener múltiples eventos por IDs
     */
    public List<CommitteeEvent> findEventsByIds(List<Long> eventIds) {
        return committeeEventRepository.findAllByIdIn(eventIds);
    }

    // ------------------ VALIDACIONES DE NEGOCIO ------------------
    private void validateCommitteeEvent(CommitteeEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Committee Event is required");
        }
        LocalDate date = event.getDate();
        Time time = event.getHour();

        if (date == null) {
            throw new IllegalArgumentException("Event date is required");
        }
        if (time == null) {
            throw new IllegalArgumentException("Event hour is required");
        }

        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Event date cannot be in the past");
        }
        if (date.isEqual(today)) {
            LocalTime now = LocalTime.now();
            if (time.toLocalTime().isBefore(now)) {
                throw new IllegalArgumentException("Event hour cannot be in the past");
            }
        }

        // Al menos 15 días de anticipación
        if (date.isBefore(today.plusDays(15))) {
            throw new IllegalArgumentException("CommitteeEvent must be scheduled at least 15 days in advance");
        }

        // Validación de personas repetidas en misma fecha y hora
        if (event.getCommittee() != null && event.getCommittee().getId() != null) {
            Committee newCommittee = committeeService.getById(event.getCommittee().getId());
            Set<Long> newStudents = new HashSet<>(Objects.requireNonNullElse(newCommittee.getStudentsIds(), List.of()));
            Set<Long> newTeachers = new HashSet<>(Objects.requireNonNullElse(newCommittee.getTeachersIds(), List.of()));
            Set<Long> newAdmins = new HashSet<>(Objects.requireNonNullElse(newCommittee.getAdministrativesIds(), List.of()));

            List<CommitteeEvent> sameSlot = committeeEventRepository.findAllByDateAndHour(date, time);
            for (CommitteeEvent existing : sameSlot) {
                if (existing.getId() != null && event.getId() != null && existing.getId().equals(event.getId())) {
                    continue; // evitar auto-colisión en updates
                }
                Committee existingCommittee = existing.getCommittee();
                if (existingCommittee == null) continue;
                Set<Long> exStudents = new HashSet<>(Objects.requireNonNullElse(existingCommittee.getStudentsIds(), List.of()));
                Set<Long> exTeachers = new HashSet<>(Objects.requireNonNullElse(existingCommittee.getTeachersIds(), List.of()));
                Set<Long> exAdmins = new HashSet<>(Objects.requireNonNullElse(existingCommittee.getAdministrativesIds(), List.of()));

                boolean studentClash = intersects(newStudents, exStudents);
                boolean teacherClash = intersects(newTeachers, exTeachers);
                boolean adminClash = intersects(newAdmins, exAdmins);

                if (studentClash || teacherClash || adminClash) {
                    throw new IllegalArgumentException("A participant is already assigned to another committee event at the same date and hour");
                }
            }
        }
    }

    private boolean intersects(Set<Long> a, Set<Long> b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        for (Long v : a) {
            if (b.contains(v)) return true;
        }
        return false;
    }
}