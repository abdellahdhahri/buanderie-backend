package tn.softsys.buanderie.Chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockServiceResponse {

    private Long serviceId;
    private long total;
    private Map<String, Long> parCategorie;
}