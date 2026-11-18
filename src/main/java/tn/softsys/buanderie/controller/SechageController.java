package tn.softsys.buanderie.controller;

import tn.softsys.buanderie.dto.BacScanResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.CycleSechage;
import tn.softsys.buanderie.entity.CycleSechageItem;
import tn.softsys.buanderie.repository.CycleSechageItemRepository;
import tn.softsys.buanderie.service.Process.CycleSechageService;

import java.util.List;


@RestController
@RequestMapping("/api/sechage")
@RequiredArgsConstructor
public class SechageController {

    private final CycleSechageService cycleService;
    private final CycleSechageItemRepository  cycleSechageItemRepository;




    @GetMapping("/scan-bac/{codeBac}")
    public ResponseEntity<BacScanResponse> scanBac(@PathVariable String codeBac) {
        return ResponseEntity.ok(cycleService.scanBac(codeBac));
    }

    /** Séchage d'un bac de tri (circuit blanc). */
    @PostMapping("/start-bac")
    public ResponseEntity<CycleSechage> startCycleBac(
            @RequestParam String codeMachine,
            @RequestParam String codeBac,
            @RequestParam String matriculeAgent,
            @RequestParam(required = false) Double poidsCharge) {
        return ResponseEntity.ok(
                cycleService.startCycleBac(codeMachine, codeBac, matriculeAgent, poidsCharge));
    }

    @PostMapping("/{cycleId}/add-bac")
    public ResponseEntity<CycleSechage> addBac(
            @PathVariable Long cycleId,
            @RequestParam String codeBac,
            @RequestParam(required = false) Double poidsCharge) {
        return ResponseEntity.ok(cycleService.addBacToCycle(cycleId, codeBac, poidsCharge));
    }

    @PutMapping("/{cycleId}/finish")
    public ResponseEntity<CycleSechage> finish(
            @PathVariable Long cycleId) {
        return ResponseEntity.ok(cycleService.finishCycle(cycleId));
    }
    @GetMapping("/cycles/{id}/items")
    public List<CycleSechageItem> getItems(@PathVariable Long id) {
        return cycleSechageItemRepository.findByCycleSechage_Id(id);
    }
}
