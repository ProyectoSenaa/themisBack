package co.sena.edu.themis.Service;

import co.sena.edu.themis.Dto.NoveltyTypeDto;
import co.sena.edu.themis.Entity.Novelty;
import co.sena.edu.themis.Repository.NoveltyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class NoveltyReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoveltyReminderScheduler.class);

    private final NoveltyRepository noveltyRepository;
    private final EmailService emailService;

    public NoveltyReminderScheduler(NoveltyRepository noveltyRepository, EmailService emailService) {
        this.noveltyRepository = noveltyRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 15 8 * * *")
    public void sendPendingNoveltyReminders() {
        LocalDate threshold = LocalDate.now().minusDays(3);
        List<Novelty> pending = noveltyRepository.findPendingOlderThan(threshold);
        if (pending == null || pending.isEmpty()) {
            log.debug("No hay novedades pendientes mayores a 3 días para recordar hoy");
            return;
        }
        for (Novelty novelty : pending) {
            try {
                NoveltyTypeDto noveltyTypeDto = new NoveltyTypeDto();
                if (novelty.getNoveltyType() != null) {
                    noveltyTypeDto.setId(novelty.getNoveltyType().getId());
                }
                emailService.sendNoveltyUpdateEmail(novelty, noveltyTypeDto);
                log.info("Recordatorio enviado para novedad {} con fecha {}", novelty.getId(), novelty.getDate());
            } catch (Exception ex) {
                log.error("Error enviando recordatorio para novedad {}: {}", novelty.getId(), ex.getMessage());
            }
        }
    }
}

