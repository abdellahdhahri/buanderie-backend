package tn.softsys.buanderie.Chatbot.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Chatbot.dto.StockServiceResponse;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.repository.LingeRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockServiceTool {

    private final LingeRepository lingeRepository;

    public StockServiceResponse getStockByService(Long serviceId) {

        List<Linge> linges =
                lingeRepository.findByServiceIdAndEtat(
                        serviceId,
                        EtatLinge.STOCKE
                );

        Map<String, Long> parCategorie =
                new LinkedHashMap<>();

        for (Linge linge : linges) {

            if (linge.getCategorie() == null) {
                continue;
            }

            String categorie =
                    linge.getCategorie().name();

            parCategorie.merge(
                    categorie,
                    1L,
                    Long::sum
            );
        }

        return new StockServiceResponse(
                serviceId,
                linges.size(),
                parCategorie
        );
    }


    public List<StockServiceResponse> getStockByServices(
            List<Long> serviceIds
    ) {

        List<StockServiceResponse> resultats =
                new ArrayList<>();

        for (Long serviceId : serviceIds) {

            resultats.add(
                    getStockByService(serviceId)
            );
        }

        return resultats;
    }
}