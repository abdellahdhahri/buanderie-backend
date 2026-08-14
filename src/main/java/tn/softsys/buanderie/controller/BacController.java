package tn.softsys.buanderie.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.softsys.buanderie.dto.BacDTO;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.repository.LingeRepository;
import tn.softsys.buanderie.service.management.BacService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bacs")
@RequiredArgsConstructor
public class BacController {

    private final BacService bacService;
    private final LingeRepository lingeRepository;

    @GetMapping
    public List<Bac> listeAll() {
        return bacService.findAll();
    }

    @GetMapping("/{id}")
    public Bac getById(@PathVariable Long id) {
        return bacService.findById(id);
    }

    @GetMapping("/rfid/{code}")
    public Bac getBycode(@PathVariable  String code){
        return bacService.findByRfidTag(code);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        bacService.delete(id);
        return ResponseEntity.ok("Bac supprimé avec succès.");
    }


    /* @GetMapping("/reference/{ref}")
    public Bac getByRef(@PathVariable String ref) {
        return bacService.findByReference(ref);
       }*/

    @GetMapping("/etat/{etat}")
    public List<Bac> getByEtat(@PathVariable EtatBac etat) {
        return bacService.findByEtat(etat);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Bac creer(@RequestBody BacDTO bacDTO) {
        return bacService.creer(bacDTO);
    }
    @GetMapping("/code/{code}")
    public Optional<Bac> getByCode(@PathVariable String code ) {
        return bacService.getByCodeBac(code);
    }
    @GetMapping("/bacs/{id}/linges")
    public List<Linge> contenuBac(@PathVariable Long id) {
        return lingeRepository.findByBacId(id);
    }

    @Data
    public static class CreerBacRequest {
        @NotBlank private String reference;
        private Long chambreId;
        private String observations;
    }

    @Transactional
    @GetMapping("/bacs/{id}/vider")
    public Bac ViderBac(@PathVariable Long id) {
        return bacService.viderBac(id);
    }
}
