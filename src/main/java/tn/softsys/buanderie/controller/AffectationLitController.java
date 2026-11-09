package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.service.Process.AffectationLitService;
import java.util.Map;

/**
 * Affectation au lit — ferme la boucle distribution → collecte
 *
 * GET  /api/affectation/chambre          ?codeBarre&matriculeAgent
 * POST /api/affectation/lit              ?litId&chambreId&codeOuTag&matriculeAgent
 * GET  /api/affectation/chambre/{id}/etat
 */
@RestController @RequestMapping("/api/affectation") @RequiredArgsConstructor
public class AffectationLitController {
    private final AffectationLitService service;

    @GetMapping("/chambre")
    public ResponseEntity<Map<String,Object>> scanChambre(@RequestParam String codeBarre,@RequestParam String matriculeAgent){
        return ResponseEntity.ok(service.scanChambre(codeBarre,matriculeAgent));}

    @PostMapping("/lit")
    public ResponseEntity<Map<String,Object>> affecter(@RequestParam Long litId,@RequestParam Long chambreId,
            @RequestParam String codeOuTag,@RequestParam String matriculeAgent){
        return ResponseEntity.ok(service.affecterLinge(litId,chambreId,codeOuTag,matriculeAgent));}

    @GetMapping("/chambre/{chambreId}/etat")
    public ResponseEntity<Map<String,Object>> etat(@PathVariable Long chambreId){
        return ResponseEntity.ok(service.getEtatChambre(chambreId));}
}


