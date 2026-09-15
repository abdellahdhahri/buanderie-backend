package tn.softsys.buanderie.Chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LingeTrackingResponse {

    private Long lingeId;

    private String codeBarre;

    private String categorie;

    private String etatActuel;

    private List<LingeEtapeTrackingDTO> parcours;
}