
package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.service.Process.CollecteChambreService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collecte/chambre")
@RequiredArgsConstructor
public class CollecteChambreController {

    private final CollecteChambreService service;

    @PostMapping("/ouvrir")
    public ResponseEntity<Map<String, Object>> ouvrir(
            @RequestParam String codeBarre,
            @RequestParam String matriculeAgent) {

        return ResponseEntity.ok(
                service.ouvrirChambre(
                        codeBarre,
                        matriculeAgent
                )
        );
    }


    @PostMapping("/{collecteId}/scanner-lit")
    public ResponseEntity<Map<String, Object>> scannerLit(
            @PathVariable Long collecteId,
            @RequestParam Long litId) {

        return ResponseEntity.ok(
                service.scannerLit(
                        collecteId,
                        litId
                )
        );
    }


    @PostMapping("/{collecteId}/lit")
    public ResponseEntity<Map<String, Object>> traiterLit(
            @PathVariable Long collecteId,
            @RequestParam Long litId,
            @RequestParam String codeContenant,
            @RequestBody(required = false) List<Long> lingeIds) {

        return ResponseEntity.ok(
                service.traiterLit(
                        collecteId,
                        litId,
                        codeContenant,
                        lingeIds
                )
        );
    }


    @PostMapping("/{collecteId}/propre")
    public ResponseEntity<Map<String, Object>> declarerPropre(
            @PathVariable Long collecteId,
            @RequestParam String codeOuTag) {

        return ResponseEntity.ok(
                service.declarerPropre(
                        collecteId,
                        codeOuTag
                )
        );
    }


    @PostMapping("/{collecteId}/manquant")
    public ResponseEntity<Map<String, Object>> declarerManquant(
            @PathVariable Long collecteId,
            @RequestParam Long lingeId) {

        return ResponseEntity.ok(
                service.declarerManquant(
                        collecteId,
                        lingeId
                )
        );
    }


    @PostMapping("/{collecteId}/cloturer")
    public ResponseEntity<Map<String, Object>> cloturer(
            @PathVariable Long collecteId) {

        return ResponseEntity.ok(
                service.cloturer(collecteId)
        );
    }
    @PostMapping("/{collecteId}/annuler-si-vide")
    public ResponseEntity<Map<String, Object>> annulerCollecteSiVide(
            @PathVariable Long collecteId) {

        return ResponseEntity.ok(
                service.annulerCollecteSiVide(
                        collecteId
                )
        );
    }

    @PostMapping("/declarer-linge-infecte")
    public ResponseEntity<Map<String, Object>> declarerLingeInfecte(
            @RequestParam Long collecteId,
            @RequestParam Long lingeId,
            @RequestParam String codeSacInfecte) {

        Map<String, Object> result =
                service.declarerLingeInfecte(
                        collecteId,
                        lingeId,
                        codeSacInfecte
                );

        return ResponseEntity.ok(result);
    }
}

