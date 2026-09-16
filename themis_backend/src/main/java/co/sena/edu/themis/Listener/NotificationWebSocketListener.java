package co.sena.edu.themis.Listener;

import co.sena.edu.themis.Dto.NotificationPushDto;
import co.sena.edu.themis.Entity.Notification;
import co.sena.edu.themis.Event.NotificationCreatedEvent;
import co.sena.edu.themis.Resolver.NotificationSubscription;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class NotificationWebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketListener.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationSubscription notificationSubscription;

    private final ModelMapper modelMapper = new ModelMapper();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        Notification notification = event.getNotification();
        try {
            NotificationPushDto dto = toPushDto(notification);
            logger.info("NotificationCreatedEvent received for notification id={}", notification != null ? notification.getId() : null);

            // Publicar a GraphQL subscription
            try {
                notificationSubscription.publishNotification(dto);
                logger.info("Published to GraphQL subscription: {}", dto);
            } catch (Exception ee) {
                logger.warn("Error publishing to GraphQL subscription: {}", ee.getMessage());
            }

            // Siempre publicar al topic general para que coordinadores y otros roles que escuchan
            // reciban la notificación. Además, si la notificación está dirigida a un estudiante
            // enviar también al queue del usuario.
            try {
                String topicDestination = "/topic/notifications";
                messagingTemplate.convertAndSend(topicDestination, dto);
                logger.info("Published to STOMP topic {}: {}", topicDestination, dto);

                if (notification != null && notification.getNovelty() != null && notification.getNovelty().getStudentId() != null) {
                    Long studentId = notification.getNovelty().getStudentId();
                    messagingTemplate.convertAndSendToUser(String.valueOf(studentId), "/queue/notifications", dto);
                    logger.info("Published to user queue for studentId={}: {}", studentId, dto);
                }
            } catch (Exception e) {
                logger.warn("Error publishing STOMP message: {}", e.getMessage());
            }

        } catch (Exception ex) {
            logger.error("Error handling NotificationCreatedEvent: {}", ex.getMessage(), ex);
        }
    }

    private NotificationPushDto toPushDto(Notification notification) {
        if (notification == null) return null;
        NotificationPushDto dto = modelMapper.map(notification, NotificationPushDto.class);
        if (notification.getNovelty() != null) dto.setNoveltyId(notification.getNovelty().getId());
        if (notification.getNovelty() != null) dto.setRecipientId(notification.getNovelty().getStudentId());
        return dto;
    }
}
