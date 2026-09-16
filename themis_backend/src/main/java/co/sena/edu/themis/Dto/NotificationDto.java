package co.sena.edu.themis.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;
    private String notiMessage;
    private String notiStatus;
    private Date dateAttention;
    private Date registrationDate;
    private NoveltyDto fk_id_novelty;
}
