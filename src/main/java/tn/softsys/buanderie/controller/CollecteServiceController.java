package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.SacsInfectes.SacInfecteDTO;
import tn.softsys.buanderie.dto.SacsInfectes.SacInfecteRequest;
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
    public ResponseEntity<SacInfecteDTO> sacInfecte(
            @PathVariable Long id,
            @RequestBody SacInfecteRequest req) {
        return ResponseEntity.ok(service.declarerSacInfecte(id, req));
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
