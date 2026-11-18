package tn.softsys.buanderie.controller;

import tn.softsys.buanderie.dto.BacScanResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.CycleLavage;
import tn.softsys.buanderie.entity.CycleLavageItem;
import tn.softsys.buanderie.entity.enums.StatutCycle;
import tn.softsys.buanderie.repository.CycleLavageItemRepository;
import tn.softsys.buanderie.repository.CycleLavageRepository;
import tn.softsys.buanderie.service.Process.CycleLavageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lavage")
@RequiredArgsConstructor
public class CycleLavageController {

    private final CycleLavageService cycleService;
    private final CycleLavageItemRepository cycleLavageItemRepository;
    private final CycleLavageRepository cycleRepo;
    private final tn.softsys.buanderie.service.management.ProduitNettoyageService produitService;
    @GetMapping("/cycles")
    public ResponseEntity<List<CycleLavage>> getAllCycles() {
        return ResponseEntity.ok(cycleService.getAllCycles());
    }
    @GetMapping("/cycles/{id}")
    public ResponseEntity<CycleLavage> getCycleById(@PathVariable Long id) {
        return ResponseEntity.ok(cycleService.getCycleById(id));
    }

    @GetMapping("/cycles/{id}/linges")
    public ResponseEntity<List<Map<String, Object>>> getLingeDuCycle(
            @PathVariable Long id) {
        return ResponseEntity.ok(cycleService.getLingeDuCycle(id));
    }

    @GetMapping("/cycles/statut/{statut}")
    public ResponseEntity<List<CycleLavage>> getByStatut(
            @PathVariable StatutCycle statut) {
        return ResponseEntity.ok(cycleRepo.findByStatut(statut));
    }


    /** Lavage direct d'un bac infecté (désinfection), sans tri ni lot. */
    @PostMapping("/start-bac-infecte")
    public ResponseEntity<CycleLavage> startCycleBacInfecte(
            @RequestParam String codeMachine,
            @RequestParam String codeBac,
            @RequestParam String matriculeAgent,
            @RequestParam(required = false) Double poidsCharge,
            @RequestParam(required = false) List<Long> produitIds
    ) {
        var produits = produitService.verifier(produitIds);
        CycleLavage cycle = cycleService.startCycleBacInfecte(
                codeMachine,
                codeBac,
                matriculeAgent,
                poidsCharge
        );
        produitService.associer(cycle, produits);
        return ResponseEntity.ok(cycle);
    }



    @GetMapping("/scan-bac/{codeBac}")
    public ResponseEntity<BacScanResponse> scanBac(@PathVariable String codeBac) {
        return ResponseEntity.ok(cycleService.scanBac(codeBac));
    }

    /** Lavage d'un bac de tri (circuit blanc). */
    @PostMapping("/start-bac")
    public ResponseEntity<CycleLavage> startCycleBac(
            @RequestParam String codeMachine,
            @RequestParam String codeBac,
            @RequestParam String matriculeAgent,
            @RequestParam(required = false) Double poidsCharge,
            @RequestParam(required = false) List<Long> produitIds
    ) {
        var produits = produitService.verifier(produitIds);
        CycleLavage cycle = cycleService.startCycleBac(codeMachine, codeBac, matriculeAgent, poidsCharge);
        produitService.associer(cycle, produits);
        return ResponseEntity.ok(cycle);
    }

    /** Produits de nettoyage utilisés dans ce cycle. */
    @GetMapping("/cycles/{id}/produits")
    public ResponseEntity<List<tn.softsys.buanderie.entity.ProduitNettoyage>> produitsDuCycle(@PathVariable Long id) {
        return ResponseEntity.ok(produitService.produitsDuCycle(id));
    }

    @PostMapping("/{cycleId}/add-bac")
    public ResponseEntity<CycleLavage> addBacToCycle(
            @PathVariable Long cycleId,
            @RequestParam String codeBac,
            @RequestParam(required = false) Double poidsCharge
    ) {
        return ResponseEntity.ok(cycleService.addBacToCycle(cycleId, codeBac, poidsCharge));
    }

    @PutMapping("/{cycleId}/finish")
    public ResponseEntity<CycleLavage> finishCycle(
            @PathVariable Long cycleId,
            @RequestParam(required = false) String codeBac
    ) {

        return ResponseEntity.ok(
                cycleService.finishCycle(cycleId, codeBac)
        );
    }

    @GetMapping("/cycles/{id}/items")
    public List<CycleLavageItem> getItems(@PathVariable Long id) {
        return cycleLavageItemRepository.findByCycleLavage_Id(id);
    }
}