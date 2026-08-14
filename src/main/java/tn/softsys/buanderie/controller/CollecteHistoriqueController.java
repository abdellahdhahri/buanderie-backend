package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.CollecteHistoriqueDTO;
import tn.softsys.buanderie.dto.CollecteListeDTO;
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

    @GetMapping("/{id}/sacs")
    public ResponseEntity<List<CollecteHistoriqueDTO.SacInfecteResumeDTO>> getSacs(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getDetailCollecte(id).getSacsInfectes());
    }

    @GetMapping("/agent/{matricule}")
    public ResponseEntity<List<CollecteListeDTO>> getParAgent(
            @PathVariable String matricule) {
        return ResponseEntity.ok(service.getCollectesParAgent(matricule));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<CollecteListeDTO>> getParService(
            @PathVariable Long serviceId) {
        return ResponseEntity.ok(service.getCollectesParService(serviceId));
    }
}
