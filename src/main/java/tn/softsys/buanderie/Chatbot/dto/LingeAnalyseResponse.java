package tn.softsys.buanderie.Chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LingeAnalyseResponse {

    private long totalLinges;

    private Map<String, Long> parEtat;

    private Map<String, Long> parCategorie;

    private Map<String, Long> parService;

    private long totalBloques;

    private Map<String, Long> bloquesParEtat;

    private List<LingeBloqueDTO> lingesBloques;
}