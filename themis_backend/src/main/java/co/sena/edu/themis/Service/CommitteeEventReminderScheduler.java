package co.sena.edu.themis.Service;

import co.sena.edu.themis.Dto.CommitteeEventDto;
import co.sena.edu.themis.Entity.CommitteeEvent;
import co.sena.edu.themis.Utils.mapper.CommitteeEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class CommitteeEventReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(CommitteeEventReminderScheduler.class);

    private final CommitteeEventService committeeEventService;
    private final CommitteeEventMapper committeeEventMapper;
    private final EmailCommitteeService emailCommitteeService;

    public CommitteeEventReminderScheduler(CommitteeEventService committeeEventService,
                                           CommitteeEventMapper committeeEventMapper,
                                           EmailCommitteeService emailCommitteeService) {
        this.committeeEventService = committeeEventService;
        this.committeeEventMapper = committeeEventMapper;
        this.emailCommitteeService = emailCommitteeService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendRemindersForUpcomingEvents() {
        LocalDate today = LocalDate.now();
        List<CommitteeEvent> upcoming = committeeEventService.getUpcomingMeetingsAll();
        if (upcoming == null || upcoming.isEmpty()) {
            log.debug("No hay eventos de comité próximos para enviar recordatorios hoy");
            return;
        }

        for (CommitteeEvent event : upcoming) {
            try {
                if (event.getDate() == null) continue;
                long daysDiff = ChronoUnit.DAYS.between(today, event.getDate());

                if (daysDiff >= 0 && daysDiff % 2 == 0) {
                    if (event.getCommittee() == null) {
                        log.debug("Evento {} sin comité asociado; se omite recordatorio.", event.getId());
                        continue;
                    }
                    CommitteeEventDto dto = committeeEventMapper.toDto(event);
                    emailCommitteeService.sendCommitteeEventReminderEmail(dto);
                    log.info("Recordatorio enviado para evento {} programado para {}", event.getId(), event.getDate());
                }
            } catch (Exception ex) {
                log.error("Error enviando recordatorio para evento {}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}

