package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.Lot.LotScanResponse;
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
    @GetMapping("/scan-lot/{codeLot}")
    public ResponseEntity<LotScanResponse> scanLot(@PathVariable String codeLot) {

        return ResponseEntity.ok(
                cycleService.scanLot(codeLot)
        );
    }

    @PostMapping("/start")
    public ResponseEntity<CycleLavage> startCycle(
            @RequestParam String codeMachine,
            @RequestParam String codeLot,
            @RequestParam String matriculeAgent,
            @RequestParam Double poidsCharge
    ) {

        return ResponseEntity.ok(
                cycleService.startCycle(
                        codeMachine,
                        codeLot,
                        matriculeAgent,
                        poidsCharge
                )
        );
    }

    @PostMapping("/{cycleId}/add-lot")
    public ResponseEntity<CycleLavage> addLotToCycle(
            @PathVariable Long cycleId,
            @RequestParam String codeLot,
            @RequestParam Double poidsCharge
    ) {

        return ResponseEntity.ok(
                cycleService.addLotToCycle(
                        cycleId,
                        codeLot,
                        poidsCharge
                )
        );
    }

    @PutMapping("/{cycleId}/finish")
    public ResponseEntity<CycleLavage> finishCycle(
            @PathVariable Long cycleId
    ) {

        return ResponseEntity.ok(
                cycleService.finishCycle(cycleId)
        );
    }

    @GetMapping("/cycles/{id}/items")
    public List<CycleLavageItem> getItems(@PathVariable Long id) {
        return cycleLavageItemRepository.findByCycleLavage_Id(id);
    }
}
