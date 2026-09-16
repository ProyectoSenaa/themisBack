package co.sena.edu.themis.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPushDto {
    private Long id;
    private String notiMessage;
    private String notiStatus;
    private Date registrationDate;
    private Long noveltyId; // id de novelty si aplica
    private Long recipientId; // id del destinatario (ej. studentId)
}
