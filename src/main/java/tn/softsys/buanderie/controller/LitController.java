package tn.softsys.buanderie.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.InfectionRequest;
import tn.softsys.buanderie.dto.LitDetailDTO;
import tn.softsys.buanderie.dto.LitRequest;
import tn.softsys.buanderie.service.management.LitService;

@RestController
@RequestMapping("/api/lits")
@RequiredArgsConstructor
public class LitController {

    private final LitService litService;

    @GetMapping
    public List<LitDetailDTO> liste(
            @RequestParam(required = false) Long chambreId,
            @RequestParam(defaultValue = "false") boolean tous) {
        return litService.lister(chambreId, tous);
    }

    @GetMapping("/chambre/{chambreId}")
    public List<LitDetailDTO> getByChambre(
            @PathVariable Long chambreId,
            @RequestParam(defaultValue = "false") boolean tous) {
        return litService.lister(chambreId, tous);
    }

    @GetMapping("/infectes")
    public List<LitDetailDTO> infectes() {
        return litService.listerInfectes();
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<LitDetailDTO> parCode(@PathVariable String code) {
        return litService.trouverParCode(code)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public LitDetailDTO trouver(@PathVariable Long id) {
        return litService.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LitDetailDTO creer(@RequestBody LitRequest request, @RequestParam(required = false) Long chambreId) {
        if (request.getChambreId() == null) {
            request.setChambreId(chambreId);
        }
        return litService.creer(request);
    }

    @PutMapping("/{id}")
    public LitDetailDTO modifier(@PathVariable Long id, @RequestBody LitRequest request) {
        return litService.modifier(id, request);
    }

    @PatchMapping("/{id}/infection")
    public LitDetailDTO infection(@PathVariable Long id, @RequestBody InfectionRequest request) {
        return litService.definirInfection(id, request.getInfecte());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactiver(@PathVariable Long id) {
        litService.desactiver(id);
    }
}
