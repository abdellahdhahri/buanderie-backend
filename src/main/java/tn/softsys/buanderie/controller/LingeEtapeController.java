package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.LingeEtape;
import tn.softsys.buanderie.entity.enums.Zone;
import tn.softsys.buanderie.service.management.LingeEtapeService;

import java.util.List;

@RestController
@RequestMapping("/api/linge-etapes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LingeEtapeController {

    private final LingeEtapeService service;


    @GetMapping
    public ResponseEntity<List<LingeEtape>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }



    @GetMapping("/{id}")
    public ResponseEntity<LingeEtape> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.getById(id));
    }


    @GetMapping("/code/{codeBarre}")
    public ResponseEntity<LingeEtape> getByCodeBarre(
            @PathVariable String codeBarre
    ) {
        return ResponseEntity.ok(
                service.getByCodeBarre(codeBarre)
        );
    }



    @GetMapping("/zone")
    public ResponseEntity<List<LingeEtape>> getByZone(
            @RequestParam Zone zone
    ) {
        return ResponseEntity.ok(
                service.getByZone(zone)
        );
    }


    @GetMapping("/categorie")
    public ResponseEntity<List<LingeEtape>> getByCategorie(
            @RequestParam String categorie
    ) {
        return ResponseEntity.ok(
                service.getByCategorie(categorie)
        );
    }


    @GetMapping("/recherche")
    public ResponseEntity<List<LingeEtape>> getByZoneAndCategorie(
            @RequestParam String zone,
            @RequestParam String categorie
    ) {
        return ResponseEntity.ok(
                service.getByZoneAndCategorie(zone, categorie)
        );
    }



    @PostMapping
    public ResponseEntity<LingeEtape> create(
            @RequestBody LingeEtape lingeEtape
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(lingeEtape));
    }



    @PutMapping("/{id}")
    public ResponseEntity<LingeEtape> update(
            @PathVariable Long id,
            @RequestBody LingeEtape lingeEtape
    ) {
        return ResponseEntity.ok(
                service.update(id, lingeEtape)
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}