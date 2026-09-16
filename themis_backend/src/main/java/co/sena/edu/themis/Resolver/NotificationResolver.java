package co.sena.edu.themis.Resolver;

import co.sena.edu.themis.Business.NotificationBusiness;
import co.sena.edu.themis.Dto.NotificationDto;
import co.sena.edu.themis.Utils.DataConvert;
import co.sena.edu.themis.Utils.Exception.CustomException;
import co.sena.edu.themis.Utils.Http.ResponseHttpApi;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.InputArgument;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DgsComponent
public class NotificationResolver {

    private final NotificationBusiness notificationBusiness;
    private final DataConvert dataConvert;

    public NotificationResolver(NotificationBusiness notificationBusiness, DataConvert dataConvert) {
        this.notificationBusiness = notificationBusiness;
        this.dataConvert = dataConvert;
    }
    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> allNotification(@InputArgument Integer page, @InputArgument Integer size) {
        try {
            Page<NotificationDto> notificationDtoPage = notificationBusiness.findAll(page, size);

            return ResponseHttpApi.responseHttpFindAll(
                    notificationDtoPage.getContent(),
                    ResponseHttpApi.CODE_OK,
                    "Notifications retrieved successfully",
                    notificationDtoPage.getTotalPages(),
                    page,
                    (int) notificationDtoPage.getTotalElements()
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    @PreAuthorize("isAuthenticated()")
    @DgsQuery
    public Map<String, Object> getNotificationById(@InputArgument String id) {
        try {
            Long convertedId = dataConvert.parseLongOrNull(id);
            if (convertedId == null) {
                return ResponseHttpApi.responseHttpError(
                        "Invalid ID format",
                        HttpStatus.BAD_REQUEST
                );
            }
            NotificationDto notification = notificationBusiness.findById(convertedId);
            if (notification != null) {

                Map<String, Object> response = new HashMap<>();
                response.put("data", List.of(notification));
                response.put("code", ResponseHttpApi.CODE_OK);
                response.put("message", "Notification retrieved successfully");
                return response;
            } else {
                return ResponseHttpApi.responseHttpError(
                        "Notification not found",
                        HttpStatus.NOT_FOUND
                );
            }
        } catch (CustomException customE) {
            return ResponseHttpApi.responseHttpError(
                    customE.getMessage(),
                    customE.getHttpStatus()
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> createNotification(
            @InputArgument String notiMessage,
            @InputArgument String notiStatus,
            @InputArgument String dateAttention,
            @InputArgument String registrationDate) {
        try {
            NotificationDto notificationDto = new NotificationDto();
            notificationDto.setNotiMessage(notiMessage);
            notificationDto.setNotiStatus(notiStatus);

            if (dateAttention != null && !dateAttention.isEmpty()) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date dateAtte = dateFormat.parse(dateAttention);
                    notificationDto.setDateAttention(dateAtte);
                } catch (ParseException e) {
                    return ResponseHttpApi.responseHttpError(
                            "Invalid dateAttention format. Use yyyy-MM-dd",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }
            if (registrationDate != null && !registrationDate.isEmpty()) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date regisDate = dateFormat.parse(registrationDate);
                    notificationDto.setRegistrationDate(regisDate);
                } catch (ParseException e) {
                    return ResponseHttpApi.responseHttpError(
                            "Invalid registrationDate format. Use yyyy-MM-dd",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }
            boolean notificationCreated = notificationBusiness.createNotification(notificationDto);
            if (notificationCreated) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", notificationDto.getId());
                response.put("code", String.valueOf(HttpStatus.CREATED.value()));
                response.put("message", "Notification created successfully");
                return response;
            } else {
                return ResponseHttpApi.responseHttpError(
                        "Notification creation failed",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        } catch (CustomException customE) {
            return ResponseHttpApi.responseHttpError(
                    customE.getMessage(),
                    customE.getHttpStatus()
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> updateNotification(
            @InputArgument String id,
            @InputArgument String notiMessage,
            @InputArgument String notiStatus,
            @InputArgument String dateAttention,
            @InputArgument String registrationDate) {
        try {
            Long convertedId = dataConvert.parseLongOrNull(id);
            if (convertedId == null) {
                return ResponseHttpApi.responseHttpError(
                        "Invalid ID format",
                        HttpStatus.BAD_REQUEST
                );
            }
            NotificationDto notificationDto = new NotificationDto();
            notificationDto.setId(convertedId);
            notificationDto.setNotiMessage(notiMessage);
            notificationDto.setNotiStatus(notiStatus);
            if (dateAttention != null && !dateAttention.isEmpty()) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date dateAtte = dateFormat.parse(dateAttention);
                    notificationDto.setDateAttention(dateAtte);
                } catch (ParseException e) {
                    return ResponseHttpApi.responseHttpError(
                            "Invalid dateAttention format. Use yyyy-MM-dd",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }
            if (registrationDate != null && !registrationDate.isEmpty()) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date regisDate = dateFormat.parse(registrationDate);
                    notificationDto.setRegistrationDate(regisDate);
                } catch (ParseException e) {
                    return ResponseHttpApi.responseHttpError(
                            "Invalid registrationDate format. Use yyyy-MM-dd",
                            HttpStatus.BAD_REQUEST
                    );
                }
            }
            boolean notificationUpdated = notificationBusiness.updateNotification(notificationDto);
            if (notificationUpdated) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", convertedId);
                response.put("code", String.valueOf(HttpStatus.OK.value()));
                response.put("message", "Notification updated successfully");
                return response;
            } else {
                return ResponseHttpApi.responseHttpError(
                        "Notification update failed",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        } catch (CustomException customE) {
            return ResponseHttpApi.responseHttpError(
                    customE.getMessage(),
                    customE.getHttpStatus()
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR') or hasAuthority('ADMINISTRADOR') or hasAuthority('ROLE_COORDINADOR') or hasAuthority('COORDINADOR')")
    @DgsMutation
    public Map<String, Object> deleteNotification(@InputArgument String id) {
        try {
            Long convertedId = dataConvert.parseLongOrNull(id);
            if (convertedId == null) {
                return ResponseHttpApi.responseHttpError(
                        "Invalid ID format",
                        HttpStatus.BAD_REQUEST
                );
            }
            boolean notificationDeleted = notificationBusiness.deleteNotificationById(convertedId);
            if (notificationDeleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", convertedId);
                response.put("code", String.valueOf(HttpStatus.OK.value()));
                response.put("message", "Notification deleted successfully");
                return response;
            } else {
                return ResponseHttpApi.responseHttpError(
                        "Notification deletion failed",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        } catch (CustomException customE) {
            return ResponseHttpApi.responseHttpError(
                    customE.getMessage(),
                    customE.getHttpStatus()
            );
        } catch (Exception e) {
            return ResponseHttpApi.responseHttpError(
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}