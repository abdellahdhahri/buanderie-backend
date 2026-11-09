package tn.softsys.buanderie.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.EmplacementDetailDTO;
import tn.softsys.buanderie.service.management.EmplacementService;

@RestController
@RequestMapping("/api/emplacements")
@RequiredArgsConstructor
public class EmplacementController {

    private final EmplacementService emplacementService;

    @GetMapping
    public List<EmplacementDetailDTO> lister(
            @RequestParam(required = false) Long arsenalId,
            @RequestParam(defaultValue = "false") boolean tous) {
        return emplacementService.lister(arsenalId, tous);
    }

    @GetMapping("/{id}")
    public EmplacementDetailDTO trouver(@PathVariable Long id) {
        return emplacementService.trouver(id);
    }

    @GetMapping("/code/{codeBarre}")
    public ResponseEntity<EmplacementDetailDTO> parCodeBarre(@PathVariable String codeBarre) {
        return emplacementService.trouverParCodeBarre(codeBarre)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cle/{cle}")
    public ResponseEntity<EmplacementDetailDTO> parCle(@PathVariable Long cle) {
        return emplacementService.trouverParCle(cle)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmplacementDetailDTO creer(@RequestBody EmplacementDetailDTO dto) {
        return emplacementService.creer(dto);
    }

    @PutMapping("/{id}")
    public EmplacementDetailDTO modifier(@PathVariable Long id, @RequestBody EmplacementDetailDTO dto) {
        return emplacementService.modifier(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactiver(@PathVariable Long id) {
        emplacementService.desactiver(id);
    }
}
