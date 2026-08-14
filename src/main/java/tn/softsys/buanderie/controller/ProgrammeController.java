package tn.softsys.buanderie.controller.management;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.ProgrammeLavage;
import tn.softsys.buanderie.entity.ProgrammeSechage;
import tn.softsys.buanderie.service.management.ProgrammeService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/programmes")
@RequiredArgsConstructor
public class ProgrammeController {

    private final ProgrammeService programmeService;

    // ==================== PROGRAMMES DE LAVAGE ====================

    @GetMapping("/lavage")
    public ResponseEntity<List<ProgrammeLavage>> getAllProgrammesLavage() {
        log.info("Récupération de tous les programmes de lavage");
        List<ProgrammeLavage> programmes = programmeService.findAllLavage();
        return ResponseEntity.ok(programmes);
    }

    @GetMapping("/lavage/{id}")
    public ResponseEntity<ProgrammeLavage> getProgrammeLavageById(@PathVariable Long id) {
        log.info("Récupération du programme de lavage avec l'ID : {}", id);
        ProgrammeLavage programme = programmeService.findLavageById(id);
        return ResponseEntity.ok(programme);
    }

    @PostMapping("/lavage")
    public ResponseEntity<ProgrammeLavage> createProgrammeLavage(@Valid @RequestBody ProgrammeLavage programme) {
        log.info("Création d'un nouveau programme de lavage : {}", programme.getNom());
        ProgrammeLavage created = programmeService.creerLavage(programme);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/lavage/{id}")
    public ResponseEntity<ProgrammeLavage> updateProgrammeLavage(
            @PathVariable Long id,
            @Valid @RequestBody ProgrammeLavage programme) {
        log.info("Mise à jour du programme de lavage avec l'ID : {}", id);
        ProgrammeLavage updated = programmeService.modifierLavage(id, programme);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/lavage/{id}")
    public ResponseEntity<ProgrammeLavage> partialUpdateProgrammeLavage(
            @PathVariable Long id,
            @RequestBody ProgrammeLavage programme) {
        log.info("Mise à jour partielle du programme de lavage avec l'ID : {}", id);
        ProgrammeLavage updated = programmeService.modifierLavage(id, programme);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/lavage/{id}")
    public ResponseEntity<Void> deleteProgrammeLavage(@PathVariable Long id) {
        log.info("Suppression du programme de lavage avec l'ID : {}", id);
        programmeService.supprimerLavage(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== PROGRAMMES DE SÉCHAGE ====================

    @GetMapping("/sechage")
    public ResponseEntity<List<ProgrammeSechage>> getAllProgrammesSechage() {
        log.info("Récupération de tous les programmes de séchage");
        List<ProgrammeSechage> programmes = programmeService.findAllSechage();
        return ResponseEntity.ok(programmes);
    }

    @GetMapping("/sechage/{id}")
    public ResponseEntity<ProgrammeSechage> getProgrammeSechageById(@PathVariable Long id) {
        log.info("Récupération du programme de séchage avec l'ID : {}", id);
        ProgrammeSechage programme = programmeService.findSechageById(id);
        return ResponseEntity.ok(programme);
    }

    @PostMapping("/sechage")
    public ResponseEntity<ProgrammeSechage> createProgrammeSechage(@Valid @RequestBody ProgrammeSechage programme) {
        log.info("Création d'un nouveau programme de séchage : {}", programme.getNom());
        ProgrammeSechage created = programmeService.creerSechage(programme);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/sechage/{id}")
    public ResponseEntity<ProgrammeSechage> updateProgrammeSechage(
            @PathVariable Long id,
            @Valid @RequestBody ProgrammeSechage programme) {
        log.info("Mise à jour du programme de séchage avec l'ID : {}", id);
        ProgrammeSechage updated = programmeService.modifierSechage(id, programme);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/sechage/{id}")
    public ResponseEntity<ProgrammeSechage> partialUpdateProgrammeSechage(
            @PathVariable Long id,
            @RequestBody ProgrammeSechage programme) {
        log.info("Mise à jour partielle du programme de séchage avec l'ID : {}", id);
        ProgrammeSechage updated = programmeService.modifierSechage(id, programme);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/sechage/{id}")
    public ResponseEntity<Void> deleteProgrammeSechage(@PathVariable Long id) {
        log.info("Suppression du programme de séchage avec l'ID : {}", id);
        programmeService.supprimerSechage(id);
        return ResponseEntity.noContent().build();
    }
}