package tn.softsys.buanderie.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.ActivationEtapeRequest;
import tn.softsys.buanderie.dto.LingeEtapeDTO;
import tn.softsys.buanderie.dto.OperationEtapeDTO;
import tn.softsys.buanderie.dto.ParcoursEtapeDTO;
import tn.softsys.buanderie.dto.SuivantesEtapeRequest;
import tn.softsys.buanderie.entity.LingeEtape;
import tn.softsys.buanderie.entity.enums.Zone;
import tn.softsys.buanderie.service.management.LingeEtapeService;
import tn.softsys.buanderie.service.management.ParcoursService;

@RestController
@RequestMapping("/api/linge-etapes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LingeEtapeController {

    private final LingeEtapeService service;
    private final ParcoursService parcoursService;

    @GetMapping
    public ResponseEntity<List<LingeEtapeDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/parcours")
    public ResponseEntity<List<ParcoursEtapeDTO>> parcours() {
        return ResponseEntity.ok(parcoursService.parcours());
    }

    @GetMapping("/operations")
    public ResponseEntity<List<OperationEtapeDTO>> operations() {
        return ResponseEntity.ok(parcoursService.operations());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<LingeEtapeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/code/{codeBarre}")
    public ResponseEntity<LingeEtapeDTO> getByCodeBarre(@PathVariable String codeBarre) {
        return ResponseEntity.ok(service.getByCodeBarre(codeBarre));
    }

    @GetMapping("/zone")
    public ResponseEntity<List<LingeEtapeDTO>> getByZone(@RequestParam Zone zone) {
        return ResponseEntity.ok(service.getByZone(zone));
    }

    @GetMapping("/categorie")
    public ResponseEntity<List<LingeEtapeDTO>> getByCategorie(@RequestParam String categorie) {
        return ResponseEntity.ok(service.getByCategorie(categorie));
    }

    @GetMapping("/recherche")
    public ResponseEntity<List<LingeEtapeDTO>> getByZoneAndCategorie(
            @RequestParam String zone,
            @RequestParam String categorie) {
        return ResponseEntity.ok(service.getByZoneAndCategorie(zone, categorie));
    }

    @PostMapping
    public ResponseEntity<LingeEtapeDTO> create(@RequestBody LingeEtape lingeEtape) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(lingeEtape));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LingeEtapeDTO> update(@PathVariable Long id, @RequestBody LingeEtape lingeEtape) {
        return ResponseEntity.ok(service.update(id, lingeEtape));
    }

    @PutMapping("/{id}/activation")
    public ResponseEntity<LingeEtapeDTO> activation(
            @PathVariable Long id,
            @RequestBody ActivationEtapeRequest request) {
        parcoursService.definirActivation(id, request.getActif());
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}/suivantes")
    public ResponseEntity<LingeEtapeDTO> suivantes(
            @PathVariable Long id,
            @RequestBody SuivantesEtapeRequest request) {
        parcoursService.definirSuivantes(id, request.getSuivantes());
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
