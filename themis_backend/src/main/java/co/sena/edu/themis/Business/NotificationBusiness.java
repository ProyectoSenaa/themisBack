package co.sena.edu.themis.Business;

import co.sena.edu.themis.Dto.NotificationDto;
import co.sena.edu.themis.Entity.Notification;
import co.sena.edu.themis.Service.NotificationService;
import co.sena.edu.themis.Utils.Exception.CustomException;
import jakarta.persistence.EntityNotFoundException;
import org.apache.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class NotificationBusiness {

    @Autowired
    private NotificationService notificationService;

    private final ModelMapper modelMapper = new ModelMapper();
    private static final Logger logger = Logger.getLogger(NotificationBusiness.class);


    public Page<NotificationDto> findAll(int page, int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Notification> notifications = notificationService.findAll(pageRequest);

            if (notifications.isEmpty()) {
                logger.info("Notifications not found!");
            }

            Page<NotificationDto> notificationDtoPage = notifications.map(Notification -> modelMapper.map(Notification, NotificationDto.class));
            return notificationDtoPage;
        } catch (Exception e) {
            logger.info(e.getMessage());
            throw new CustomException("Error", "Error getting notifications", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public NotificationDto findById(Long id) {
        try {
            Notification notification = notificationService.getById(id);
            logger.info("Notification: {}" + notification);
            if (notification != null){
                return modelMapper.map(notification, NotificationDto.class);
            } else {
                throw new CustomException("Not Found", "Not Found notification with that id", HttpStatus.NOT_FOUND);
            }
        } catch (EntityNotFoundException entNotFound) {
            logger.info(entNotFound.getMessage());
            throw new CustomException("Not Found", "Not found notification with that id", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomException("Error", "Error getting notification by id", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public boolean createNotification(NotificationDto notificationDto){
        try {
            Notification notification = modelMapper.map(notificationDto, Notification.class);
            notificationService.save(notification);
            logger.info("Notification created successfully");
            return true;
        } catch (Exception e){
            logger.error(e.getMessage());
            throw new CustomException("Error", "Error creating notification", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean updateNotification(NotificationDto notificationDto){
        try {
            if (notificationDto.getId() == null){
                logger.info("Can't update notification because the id is null!");
            }

            Notification existingNotification = notificationService.getById(notificationDto.getId());
            logger.info("Notification: {}" + existingNotification);

            Notification updatedNotification = modelMapper.map(notificationDto, Notification.class);
            notificationService.save(updatedNotification);
            return true;
        } catch (EntityNotFoundException entNotFound) {
            logger.info("The notification you are trying to update is not registered");
            throw new CustomException("Not Found", "Can't update the notification because it isn't registered", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomException("Error", "Error update notification", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public boolean deleteNotificationById(Long id) {
        try {
            if (id ==  null) {
                logger.info("Can't delete notification because the id is null!");
            }

            Notification deletingNotification = notificationService.getById(id);
            logger.info("Notification: {}" + deletingNotification);

            notificationService.deleteById(id);
            logger.info("Notification deleted successfully");
            return true;
        } catch (EntityNotFoundException entNotFound) {
            logger.info("The notification you are trying to delete is not registered");
            throw new CustomException("Not Found", "Can't delete the notification because it isn't registered", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CustomException("Error", "Error delete notification", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
