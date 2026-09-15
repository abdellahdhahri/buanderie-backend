package tn.softsys.buanderie.Chatbot.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Chatbot.dto.StockSituationResponse;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.repository.LingeRepository;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockAssistantTool {

    private final LingeRepository lingeRepository;

    public StockSituationResponse getStockSituation() {

        long total = lingeRepository.countByEtat(EtatLinge.STOCKE);

        Map<String, Long> parCategorie = new LinkedHashMap<>();

        for (CategLinge categorie : CategLinge.values()) {

            long quantite =
                    lingeRepository.countByCategorieAndEtat(
                            categorie,
                            EtatLinge.STOCKE
                    );

            if (quantite > 0) {
                parCategorie.put(
                        categorie.name(),
                        quantite
                );
            }
        }

        return new StockSituationResponse(
                total,
                parCategorie
        );
    }
}