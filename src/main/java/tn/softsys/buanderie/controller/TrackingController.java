package tn.softsys.buanderie.controller;

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

    /**
     * GET /tracking/sac/{numeroSac}
     * Historique complet d'un sac infecté
     */
    @GetMapping("/sac/{numeroSac}")
    public ResponseEntity<List<HistoriqueLinge>> historiqueParSac(@PathVariable String numeroSac) {
        return ResponseEntity.ok(
            historiqueLingeRepository.findBySacInfecte_NumeroSacOrderByDateOperationDesc(numeroSac)
        );
    }
}
