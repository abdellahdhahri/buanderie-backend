package tn.softsys.buanderie.Chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollecteAnalyseResponse {

    private String periode;

    private long totalCollectes;

    private long collectesOuvertes;

    private long collectesTerminees;

    private long totalPiecesScannees;

    private long totalAttendus;

    private long totalPropres;

    private long totalInfectesPresumes;

    private long totalManquants;

    private Map<String, Long> parEtat;

    private Map<String, Long> parType;

    private Map<String, Long> parService;

    private Map<String, Long> parCategorie;
}