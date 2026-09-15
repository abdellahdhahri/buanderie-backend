package tn.softsys.buanderie.Chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LingeBloqueDTO {

    private Long lingeId;

    private String codeBarre;

    private String categorie;

    private String etat;

    private Long serviceId;

    private LocalDateTime dernierChangementEtat;

    private long dureeHeures;

    private long seuilBlocageHeures;
}