package tn.softsys.buanderie.controller;

import java.util.Comparator;

import java.util.ArrayList;

import tn.softsys.buanderie.repository.LingeRepository;

import tn.softsys.buanderie.entity.Linge;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.HistoriqueLinge;
import tn.softsys.buanderie.repository.HistoriqueLingeRepository;
import tn.softsys.buanderie.service.management.LingeService;
import tn.softsys.buanderie.service.management.TrackingService;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final LingeService lingeService;
    private final TrackingService trackingService;
    private final HistoriqueLingeRepository historiqueLingeRepository;
    private final LingeRepository lingeRepository;

    @GetMapping("/historique/code/{codeBarre}")
    public List<HistoriqueLinge> historiqueParCode(@PathVariable String codeBarre) {
        return trackingService.getHistoriqueParCodeBarre(codeBarre);
    }

    /**
     * GET /tracking/collecte/{collecteId}
     * Tout l'historique d'une collecte : linges blancs + linges infectés
     */
    @GetMapping("/collecte/{collecteId}")
    public ResponseEntity<List<HistoriqueLinge>> historiqueParCollecte(@PathVariable Long collecteId) {
        return ResponseEntity.ok(
                historiqueLingeRepository.findByCollecte_IdOrderByDateOperationDesc(collecteId)
        );
    }

 
    @GetMapping("/bac/{codeBac}")
    public ResponseEntity<List<HistoriqueLinge>> historiqueParBac(@PathVariable String codeBac) {
        List<HistoriqueLinge> resultat = new ArrayList<>();
        for (Linge linge : lingeRepository.findByBac_Code(codeBac)) {
            resultat.addAll(historiqueLingeRepository.findByLingeIdOrderByDateOperationDesc(linge.getId()));
        }
        resultat.sort(Comparator.comparing(HistoriqueLinge::getDateOperation).reversed());
        return ResponseEntity.ok(resultat);
    }
}
