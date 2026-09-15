package tn.softsys.buanderie.Chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockCritiqueResponse {

    private String categorie;

    private long quantite;

    private String niveau;

    private long seuil;
}