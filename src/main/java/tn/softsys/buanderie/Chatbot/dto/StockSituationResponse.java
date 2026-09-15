package tn.softsys.buanderie.Chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class StockSituationResponse {

    private long total;

    private Map<String, Long> parCategorie;
}