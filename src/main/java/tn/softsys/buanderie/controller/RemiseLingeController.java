package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.softsys.buanderie.dto.StatutCollecteDTO;
import tn.softsys.buanderie.entity.RemiseLinge;
import tn.softsys.buanderie.entity.RemiseLingeItem;
import tn.softsys.buanderie.service.management.RemiseLingeService;

import java.util.List;

@RestController
@RequestMapping("/api/remises-linge")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RemiseLingeController {

    private final RemiseLingeService remiseLingeService;

    @PostMapping
    public ResponseEntity<RemiseLinge> creerRemise(
            @RequestParam String matriculeAgentRemise,
            @RequestParam Long id
    ) {

        RemiseLinge remise =
                remiseLingeService.creerRemise(
                        matriculeAgentRemise,
                        id
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(remise);
    }
    @GetMapping("/collecte/{collecteId}/statut")
    public ResponseEntity<StatutCollecteDTO> getStatutCollecte(
            @PathVariable Long collecteId
    ) {

        return ResponseEntity.ok(
                remiseLingeService.getStatutCollecte(collecteId)
        );
    }
    @PostMapping("/{remiseId}/scanner")
    public ResponseEntity<RemiseLingeItem> scannerLinge(
            @PathVariable Long remiseId,
            @RequestParam String codeBarre
    ) {

        RemiseLingeItem item =
                remiseLingeService.scannerLinge(
                        remiseId,
                        codeBarre
                );

        return ResponseEntity.ok(item);
    }

    public ResponseEntity<RemiseLinge> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                remiseLingeService.getById(id)
        );
    }


    @GetMapping("/collecte/{collecteId}")
    public ResponseEntity<List<RemiseLinge>> getByCollecte(
            @PathVariable Long collecteId
    ) {

        return ResponseEntity.ok(
                remiseLingeService.getByCollecte(
                        collecteId
                )
        );
    }



    @GetMapping("/collecte/{collecteId}/reste")
    public ResponseEntity<List<RemiseLingeItem>> getResteAremettre(
            @PathVariable Long collecteId
    ) {

        return ResponseEntity.ok(
                remiseLingeService.getResteAremettre(
                        collecteId
                )
        );
    }


    @PutMapping("/{id}/terminer")
    public ResponseEntity<RemiseLinge> terminerRemise(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                remiseLingeService.terminerRemise(id)
        );
    }
}