package tn.softsys.buanderie.Chatbot.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Chatbot.dto.StockCritiqueResponse;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.repository.LingeRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCritiqueTool {

    private final LingeRepository lingeRepository;

    /*
     * Règles métier Buanderie
     */
    private static final long SEUIL_CRITIQUE = 3;
    private static final long SEUIL_ATTENTION = 8;

    public List<StockCritiqueResponse> getStockCritique() {

        List<StockCritiqueResponse> resultats = new ArrayList<>();

        for (CategLinge categorie : CategLinge.values()) {

            long quantite =
                    lingeRepository.countByCategorieAndEtat(
                            categorie,
                            EtatLinge.STOCKE
                    );

            if (quantite <= SEUIL_CRITIQUE) {

                resultats.add(
                        new StockCritiqueResponse(
                                categorie.name(),
                                quantite,
                                "CRITIQUE",
                                SEUIL_CRITIQUE
                        )
                );

            } else if (quantite <= SEUIL_ATTENTION) {

                resultats.add(
                        new StockCritiqueResponse(
                                categorie.name(),
                                quantite,
                                "ATTENTION",
                                SEUIL_ATTENTION
                        )
                );
            }
        }

        return resultats;
    }
}