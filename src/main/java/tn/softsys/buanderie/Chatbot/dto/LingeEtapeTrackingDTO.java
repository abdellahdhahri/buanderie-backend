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
public class LingeEtapeTrackingDTO {

    private String etat;

    private LocalDateTime date;

    private Long personnelId;

    private String operation;

    private String observations;
}