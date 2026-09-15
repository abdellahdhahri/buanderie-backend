package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.SacsInfectes.AffectationLingesPropresRequest;
import tn.softsys.buanderie.dto.SacsInfectes.SacInfecteDTO;
import tn.softsys.buanderie.dto.SacsInfectes.SacInfecteRequest;
import tn.softsys.buanderie.dto.VerificationLavageRequest;
import tn.softsys.buanderie.entity.SacInfecte;
import tn.softsys.buanderie.service.management.SacInfecteService;

import java.util.List;

@RestController
@RequestMapping("/api/sacs-infectes")
@RequiredArgsConstructor
public class SacInfecteController {

    private final SacInfecteService sacInfecteService;
    @PostMapping
    public SacInfecte creer(@RequestBody SacInfecteDTO request) {
        return sacInfecteService.creerSac(request);
    }
    @PostMapping("/declarer")
    public ResponseEntity<SacInfecteDTO> declarerSac(@RequestBody SacInfecteRequest request) {
        return ResponseEntity.ok(sacInfecteService.declarerSacInfecte(request));
    }

    @PutMapping("/recevoir")
    public ResponseEntity<SacInfecteDTO> recevoirSac(
            @RequestParam String numeroSac, @RequestParam String matricule) {
        return ResponseEntity.ok(sacInfecteService.recevoirSacInfecte(numeroSac, matricule));
    }

    @PutMapping("/laver")
    public ResponseEntity<SacInfecteDTO> mettreEnLavage(
            @RequestParam String numeroSac,
            @RequestParam String matricule,
            @RequestParam String codeMachine) {
        return ResponseEntity.ok(
                sacInfecteService.mettreEnLavage(numeroSac, matricule, codeMachine));
    }

    @PostMapping("/verifier")
    public ResponseEntity<SacInfecteDTO> verifierApresLavage(@RequestBody VerificationLavageRequest request) {
        return ResponseEntity.ok(sacInfecteService.verifierApresLavage(request));
    }

    @PostMapping("/affecter-linges-propres")
    public ResponseEntity<SacInfecteDTO> affecterLingesPropres(@RequestBody AffectationLingesPropresRequest request) {
        return ResponseEntity.ok(sacInfecteService.affecterLingesPropres(request));
    }

    @GetMapping
    public ResponseEntity<List<SacInfecteDTO>> getTousSacs() {
        return ResponseEntity.ok(sacInfecteService.getTousSacs());
    }

    @GetMapping("/detail")
    public ResponseEntity<SacInfecteDTO> getSacByNumero(@RequestParam String numeroSac) {
        return ResponseEntity.ok(sacInfecteService.getSacByNumero(numeroSac));
    }

    //tous les sacs d'une collecte
    @GetMapping("/par-collecte")
    public ResponseEntity<List<SacInfecteDTO>> getSacsParCollecte(@RequestParam Long collecteId) {
        return ResponseEntity.ok(sacInfecteService.getSacsParCollecte(collecteId));
    }

    //tous les sacs d'un agent
    @GetMapping("/agent")
    public ResponseEntity<List<SacInfecteDTO>> getSacsParAgent(@RequestParam Long matricule) {
        return ResponseEntity.ok(sacInfecteService.getSacsParAgent(matricule));
    }

    @GetMapping("/en-attente-verification")
    public ResponseEntity<List<SacInfecteDTO>> getSacsEnAttenteVerification() {
        return ResponseEntity.ok(sacInfecteService.getSacsEnAttenteVerification());
    }

    @GetMapping("/non-laves")
    public ResponseEntity<List<SacInfecteDTO>> getSacsNonLaves() {
        return ResponseEntity.ok(sacInfecteService.getSacsNonLaves());
    }

    @GetMapping("/ecarts")
    public ResponseEntity<List<SacInfecteDTO>> getSacsAvecEcart() {
        return ResponseEntity.ok(sacInfecteService.getSacsAvecEcart());
    }
}
