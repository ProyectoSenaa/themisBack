package co.sena.edu.themis.Event;

import co.sena.edu.themis.Entity.Notification;
import org.springframework.context.ApplicationEvent;

public class NotificationCreatedEvent extends ApplicationEvent {
    private final Notification notification;

    public NotificationCreatedEvent(Object source, Notification notification) {
        super(source);
        this.notification = notification;
    }

    public Notification getNotification() {
        return notification;
    }
}

