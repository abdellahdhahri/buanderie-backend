package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.Collectes.ResumeCollecteDTO;
import tn.softsys.buanderie.service.Process.CollecteChambreService;
import java.util.Map;

@RestController @RequestMapping("/api/collecte/chambre") @RequiredArgsConstructor
public class CollecteChambreController {
    private final CollecteChambreService service;

    @PostMapping("/ouvrir")
    public ResponseEntity<Map<String,Object>> ouvrir(@RequestParam String codeBarre,@RequestParam String matriculeAgent){
        return ResponseEntity.ok(service.ouvrirChambre(codeBarre,matriculeAgent));}

    @PostMapping("/{id}/lit")
    public ResponseEntity<Map<String,Object>> traiterLit(@PathVariable Long id,@RequestParam Long litId,@RequestParam String codeBac){
        return ResponseEntity.ok(service.traiterLit(id,litId,codeBac));}

    @PostMapping("/{id}/propre")
    public ResponseEntity<Map<String,Object>> propre(@PathVariable Long id,@RequestParam String codeOuTag){
        return ResponseEntity.ok(service.declarerPropre(id,codeOuTag));}

    @PostMapping("/{id}/manquant")
    public ResponseEntity<Map<String,Object>> manquant(@PathVariable Long id,@RequestParam Long lingeId){
        return ResponseEntity.ok(service.declarerManquant(id,lingeId));}

    @PostMapping("/{id}/cloturer")
    public ResponseEntity<Map<String,Object>> cloturer(@PathVariable Long id){
        return ResponseEntity.ok(service.cloturer(id));}


}
