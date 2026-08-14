package tn.softsys.buanderie.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private String titre;

    private String message;

    private String type;

    private LocalDateTime dateCreation;

    private Long lingeId;

    private String codeLinge;

    private String categorie;

}