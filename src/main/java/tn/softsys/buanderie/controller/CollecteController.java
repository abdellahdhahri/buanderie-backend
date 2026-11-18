package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.AjoutLingesCollecteRequest;
import tn.softsys.buanderie.dto.Collectes.ResumeCollecteDTO;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.repository.CollecteRepository;
import tn.softsys.buanderie.service.management.CollecteAjoutManuelService;
import tn.softsys.buanderie.service.management.CollecteResumeService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collecte") @RequiredArgsConstructor
public class CollecteController {

    private final CollecteResumeService collecteResumeService;
    private  final CollecteRepository collecteRepository;
    private final CollecteAjoutManuelService service;


    @GetMapping
    public List<Collecte> getResume(){

        return collecteRepository.findAll();

    }
    @GetMapping("/{id}/resume")
    public ResumeCollecteDTO getResume(@PathVariable Long id){

        return collecteResumeService.getResume(id);

    }
    @PostMapping("/{collecteId}/ajouter-linges")
    public ResponseEntity<Map<String, Object>> ajouterLinges(
            @PathVariable Long collecteId,
            @RequestBody AjoutLingesCollecteRequest request) {

        return ResponseEntity.ok(service.ajouterLinges(collecteId, request));
    }
}
