package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.*;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingService {

    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final  LingeRepository lingeRepository;



    public List<HistoriqueLinge> getHistoriqueParCodeBarre(
            String codeBarre) {

        Linge linge = lingeRepository
                .findByCodeBarre(codeBarre)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Linge",
                                "codeBarre",
                                codeBarre));

        return historiqueLingeRepository
                .findByLingeIdOrderByDateOperationDesc(
                        linge.getId());
    }
}
