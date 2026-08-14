package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.Lot.LotScanResponse;
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

    @GetMapping("/scan-lot/{codeLot}")
    public ResponseEntity<LotScanResponse> scanLot(
            @PathVariable String codeLot) {
        return ResponseEntity.ok(cycleService.scanLot(codeLot));
    }

    @PostMapping("/start")
    public ResponseEntity<CycleSechage> startCycle(
            @RequestParam String codeMachine,
            @RequestParam String codeLot,
            @RequestParam String matriculeAgent,
            @RequestParam Double poidsCharge) {
        return ResponseEntity.ok(
                cycleService.startCycle(codeMachine, codeLot,
                        matriculeAgent, poidsCharge));
    }

    @PostMapping("/{cycleId}/add-lot")
    public ResponseEntity<CycleSechage> addLot(
            @PathVariable Long   cycleId,
            @RequestParam String codeLot,
            @RequestParam Double poidsCharge) {
        return ResponseEntity.ok(
                cycleService.addLotToCycle(cycleId, codeLot, poidsCharge));
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
