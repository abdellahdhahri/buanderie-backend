package tn.softsys.buanderie.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.Referentiel.PersonnelRequest;
import tn.softsys.buanderie.entity.Personnel;
import tn.softsys.buanderie.service.management.PersonnelService;

@RestController
@RequestMapping("/api/personnel")
@RequiredArgsConstructor
public class PersonnelController {

    private final PersonnelService personnelService;

    @GetMapping
    public List<Personnel> getAll(@RequestParam(defaultValue = "false") boolean actifs) {
        return personnelService.getAll(actifs);
    }

    @GetMapping("/{id}")
    public Personnel getById(@PathVariable Long id) {
        return personnelService.getById(id);
    }

    @GetMapping("/code/{codeBarre}")
    public Personnel getByCodeBarre(@PathVariable String codeBarre) {
        return personnelService.getByCodeBarre(codeBarre);
    }

    @PostMapping
    public ResponseEntity<Personnel> creer(@Valid @RequestBody PersonnelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personnelService.creer(request));
    }

    @PutMapping("/{id}")
    public Personnel modifier(@PathVariable Long id, @Valid @RequestBody PersonnelRequest request) {
        return personnelService.modifier(id, request);
    }

    @PutMapping("/{id}/etat")
    public Personnel changerEtat(@PathVariable Long id, @RequestParam boolean actif) {
        return personnelService.changerEtat(id, actif);
    }
}
