package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.service.Process.CollecteUniformeService;
import java.util.Map;

@RestController @RequestMapping("/api/collecte/uniforme") @RequiredArgsConstructor
public class CollecteUniformeController {
    private final CollecteUniformeService service;

    @PostMapping("/ouvrir")
    public ResponseEntity<Map<String,Object>> ouvrir(@RequestParam String matriculeAgent,@RequestParam String codeBadge){
        return ResponseEntity.ok(service.ouvrirUniforme(matriculeAgent,codeBadge));}

    @PostMapping("/{id}/traiter")
    public ResponseEntity<Map<String,Object>> traiter(@PathVariable Long id,@RequestParam String codeUniforme,@RequestParam String codeBac){
        return ResponseEntity.ok(service.traiterUniforme(id,codeUniforme,codeBac));}

    @PostMapping("/{id}/manquant")
    public ResponseEntity<Map<String,Object>> manquant(@PathVariable Long id,@RequestParam Long lingeId){
        return ResponseEntity.ok(service.declarerManquant(id,lingeId));}

    @PostMapping("/{id}/cloturer")
    public ResponseEntity<Map<String,Object>> cloturer(@PathVariable Long id){
        return ResponseEntity.ok(service.cloturer(id));}
}
