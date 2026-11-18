package tn.softsys.buanderie.controller;

import tn.softsys.buanderie.dto.BacInfecteRequest;

import tn.softsys.buanderie.dto.BacInfecteDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.service.Process.CollecteServiceService;
import java.util.Map;

@RestController
@RequestMapping("/api/collecte/service")
@RequiredArgsConstructor
public class CollecteServiceController {

    private final CollecteServiceService service;


    @PostMapping("/ouvrir")
    public ResponseEntity<Map<String, Object>> ouvrir(
            @RequestParam String matriculeAgent,
            @RequestParam(required = false) Long   serviceId,
            @RequestParam(required = false) String refOuCode) {
        return ResponseEntity.ok(
                service.ouvrirService(matriculeAgent, serviceId, refOuCode));
    }

    @PostMapping("/chambre/ouvrir")
    public ResponseEntity<Map<String, Object>> ouvrirChambre(
            @RequestParam String matriculeAgent,
            @RequestParam String codeChambre) {
        return ResponseEntity.ok(service.ouvrirChambre(matriculeAgent, codeChambre));
    }

    @PostMapping("/{id}/bac")
    public ResponseEntity<Map<String, Object>> scanBac(
            @PathVariable Long id,
            @RequestParam String codeOuTag) {
        return ResponseEntity.ok(service.scanBac(id, codeOuTag));
    }

    @PostMapping("/{id}/blanc")
    public ResponseEntity<Map<String, Object>> scanBlanc(
            @PathVariable Long id,
            @RequestParam Long   bacId,
            @RequestParam String codeOuTag) {
        return ResponseEntity.ok(service.scanLingeBlanc(id, bacId, codeOuTag));
    }

    @PostMapping("/{id}/infecte")
    public ResponseEntity<BacInfecteDTO> quantitesInfectes(
            @PathVariable Long id,
            @RequestBody BacInfecteRequest req) {
        return ResponseEntity.ok(service.declarerQuantitesInfectes(id, req));
    }

    @PostMapping("/{id}/propre")
    public ResponseEntity<Map<String, Object>> propre(
            @PathVariable Long id,
            @RequestParam String codeOuTag) {
        return ResponseEntity.ok(service.declarerPropre(id, codeOuTag));
    }

    @PostMapping("/{id}/manquant")
    public ResponseEntity<Map<String, Object>> manquant(
            @PathVariable Long id,
            @RequestParam Long lingeId) {
        return ResponseEntity.ok(service.declarerManquant(id, lingeId));
    }

    @PostMapping("/{id}/cloturer")
    public ResponseEntity<Map<String, Object>> cloturer(@PathVariable Long id) {
        return ResponseEntity.ok(service.cloturer(id));
    }
}
