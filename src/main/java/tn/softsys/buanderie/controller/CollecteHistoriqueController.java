package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.softsys.buanderie.dto.Collectes.CollecteHistoriqueDTO;
import tn.softsys.buanderie.dto.Collectes.CollecteListeDTO;
import tn.softsys.buanderie.service.Reporting.CollecteHistoriqueService;

import java.util.List;


@RestController
@RequestMapping("/api/historique/collectes")
@RequiredArgsConstructor
public class CollecteHistoriqueController {

    private final CollecteHistoriqueService service;

    @GetMapping
    public ResponseEntity<List<CollecteListeDTO>> getAll() {
        return ResponseEntity.ok(service.getAllCollectes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollecteHistoriqueDTO> getDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getDetailCollecte(id));
    }

    @GetMapping("/{id}/linges")
    public ResponseEntity<List<CollecteHistoriqueDTO.CollecteLingeDTO>> getLinges(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getDetailCollecte(id).getLinges());
    }

    @GetMapping("/{id}/bacs-infectes")
    public ResponseEntity<List<CollecteHistoriqueDTO.BacInfecteResumeDTO>> getBacsInfectes(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getDetailCollecte(id).getBacsInfectes());
    }

    @GetMapping("/agent/{matricule}")
    public ResponseEntity<List<CollecteListeDTO>> getParAgent(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getCollectesParAgent(id));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<CollecteListeDTO>> getParService(
            @PathVariable Long serviceId) {
        return ResponseEntity.ok(service.getCollectesParService(serviceId));
    }
}
