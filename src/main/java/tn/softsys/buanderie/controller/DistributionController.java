package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.Livraison.DistributionRequest;
import tn.softsys.buanderie.entity.Distribution;
import tn.softsys.buanderie.service.Process.DistributionService;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionService distributionService;

    // ── CHAMBRE ───────────────────────────────────────────────────────

    @GetMapping("/api/distribution/chambre")
    public ResponseEntity<Map<String, Object>> scanChambre(
            @RequestParam String codeBarre,
            @RequestParam String matriculeAgent) {
        return ResponseEntity.ok(
                distributionService.scanChambrePourDistribution(codeBarre, matriculeAgent));
    }

    @PostMapping("/api/distribution/chambre/lit")
    public ResponseEntity<Map<String, Object>> distribuerSurLit(
            @RequestParam Long   chambreId,
            @RequestParam Long   litId,
            @RequestParam String codeOuTag,
            @RequestParam String matriculeAgent) {
        return ResponseEntity.ok(
                distributionService.distribuerSurLit(chambreId, litId, codeOuTag, matriculeAgent));
    }

    // ── SERVICE ───────────────────────────────────────────────────────

    @GetMapping("/api/distribution/service")
    public ResponseEntity<Map<String, Object>> lingesService(
            @RequestParam Long   serviceId,
            @RequestParam String matriculeAgent) {
        return ResponseEntity.ok(
                distributionService.getLingesStockePourService(serviceId, matriculeAgent));
    }

    @PostMapping("/api/distribution/service")
    public ResponseEntity<Map<String, Object>> distribuerService(
            @RequestParam Long   serviceId,
            @RequestParam String codeOuTag,
            @RequestParam String matriculeAgent) {
        return ResponseEntity.ok(
                distributionService.distribuerAuService(serviceId, codeOuTag, matriculeAgent));
    }


    @GetMapping("/api/distribution/uniforme")
    public ResponseEntity<Map<String, Object>> uniformesPersonnel(
            @RequestParam String codeBadge,
            @RequestParam String matriculeAgent) {
        return ResponseEntity.ok(
                distributionService.getUniformesPourPersonnel(codeBadge, matriculeAgent));
    }

    @PostMapping("/api/distribution/uniforme")
    public ResponseEntity<Map<String, Object>> distribuerUniforme(
            @RequestParam Long   porteurId,
            @RequestParam String porteurNom,
            @RequestParam String codeOuTag,
            @RequestParam String matriculeAgent) {
        return ResponseEntity.ok(
                distributionService.distribuerUniforme(porteurId, porteurNom,
                        codeOuTag, matriculeAgent));
    }


    @PostMapping("/api/distributions")
    public ResponseEntity<Distribution> creer(@RequestBody DistributionRequest req) {
        return ResponseEntity.ok(distributionService.creerDistribution(req));
    }

    @PostMapping("/api/distributions/{id}/scan")
    public ResponseEntity<String> scan(
            @PathVariable Long id,
            @RequestParam String codeOuTag,
            @RequestParam(required = false) Long litId,
            @RequestParam(required = false) Long chambreId) {
        distributionService.scannerLinge(id, codeOuTag, litId, chambreId);
        return ResponseEntity.ok("Linge livré");
    }

    @GetMapping("/api/distributions/{id}")
    public ResponseEntity<Distribution> get(@PathVariable Long id) {
        return ResponseEntity.ok(distributionService.getById(id));
    }

    @GetMapping("/api/distributions/{id}/comparatif")
    public ResponseEntity<java.util.List<Map<String, Object>>> comparatif(@PathVariable Long id) {
        return ResponseEntity.ok(distributionService.getComparatif(id));
    }
}
