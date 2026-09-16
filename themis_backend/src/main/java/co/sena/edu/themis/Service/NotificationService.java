package co.sena.edu.themis.Service;

import co.sena.edu.themis.Entity.Notification;
import co.sena.edu.themis.Event.NotificationCreatedEvent;
import co.sena.edu.themis.Repository.NotificationRepository;
import co.sena.edu.themis.Service.Dao.Idao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService implements Idao<Notification, Long> {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    @Override
    public Notification getById(Long id) {
        // Mantener compatibilidad con la interfaz: delega a findById y lanza si no existe
        return notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("Notification not found"));
    }

    // Nuevo: findById que devuelve Optional para usos más seguros
    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    @Override
    @Transactional
    public Notification save(Notification notification) {
        Notification saved = notificationRepository.save(notification);
        // Publicar evento para que listeners (p.ej. WebSocket) envíen la notificación en tiempo real
        try {
            eventPublisher.publishEvent(new NotificationCreatedEvent(this, saved));
        } catch (Exception ex) {
            // No detener el flujo si el publisher falla; loguear sería ideal (logger no inyectado aquí)
        }
        return saved;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        notificationRepository.deleteById(id);
    }

    @Override
    public Page<Notification> findAll(Pageable pageable) {
        return this.notificationRepository.findAll(pageable);
    }

    // Marcar como leída usando el campo notiStatus (asumimos convencion 'READ')
    @Transactional
    public Notification markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setNotiStatus("READ");
        return notificationRepository.save(notification);
    }

    // Ejemplo de búsqueda por novelty (la entidad Notification tiene relación con Novelty)
    public Page<Notification> findByNoveltyId(Long noveltyId, Pageable pageable) {
        // Si más adelante se añade el método en el repositorio, delegar aquí.
        // Por ahora devolvemos el resultado paginado filtrando en memoria como fallback (no óptimo)
        return notificationRepository.findAll(pageable).map(n -> n.getNovelty() != null && n.getNovelty().getId().equals(noveltyId) ? n : null).map(n -> n);
    }
}
