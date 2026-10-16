
package tn.softsys.buanderie.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.softsys.buanderie.Sqlserver.Cubix.ArsenalCubixService;
import tn.softsys.buanderie.Sqlserver.dtos.ArsenalDto;
import tn.softsys.buanderie.Sqlserver.dtos.EmplacementDto;
import tn.softsys.buanderie.dto.ReceptionArsenalRequestDTO;
import tn.softsys.buanderie.entity.ReceptionArsenal;
import tn.softsys.buanderie.repository.ReceptionArsenalRepository;
import tn.softsys.buanderie.service.Process.ReceptionArsenalService;

@RestController
@RequestMapping("/api/receptions-arsenal")
@RequiredArgsConstructor
public class ReceptionArsenalController {

    private final ReceptionArsenalService receptionArsenalService;
    private final ArsenalCubixService arsenalCubixService;
    private final ReceptionArsenalRepository receptionArsenalRepository;

    // 1. Lister les arsenaux
    @GetMapping("/arsenaux")
    public ResponseEntity<List<ArsenalDto>> getArsenaux() {
        return ResponseEntity.ok(arsenalCubixService.getArsenaux());
    }

    // 2. Lister les emplacements d'un arsenal
    @GetMapping("/arsenaux/{arsenalID}/emplacements")
    public ResponseEntity<List<EmplacementDto>> getEmplacements(
            @PathVariable Long arsenalID) {

        return ResponseEntity.ok(
                arsenalCubixService.getEmplacements(arsenalID));
    }

    // 3. Enregistrer une réception
    @PostMapping
    public ResponseEntity<Map<String, Object>> enregistrerReception(
            @Valid @RequestBody ReceptionArsenalRequestDTO request) {

        ReceptionArsenal reception =
                receptionArsenalService.enregistrerReception(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Réception enregistrée avec succès.");
        response.put("receptionId", reception.getId());
        response.put("remiseLingeId", reception.getRemiseLinge().getId());
        response.put("arsenalID", reception.getArsenalID());
        response.put("dateReception", reception.getDateReception());
        response.put("statutReception", reception.getStatutReception());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 4. Consulter l'historique des réceptions d'une remise
    @GetMapping("/remise/{remiseLingeId}")
    public ResponseEntity<List<Map<String, Object>>> historique(
            @PathVariable Long remiseLingeId) {

        List<ReceptionArsenal> receptions =
                receptionArsenalRepository
                        .findByRemiseLinge_IdOrderByDateReceptionDesc(
                                remiseLingeId);

        List<Map<String, Object>> result = receptions.stream()
                .map(reception -> {
                    Map<String, Object> ligne = new LinkedHashMap<>();
                    ligne.put("receptionId", reception.getId());
                    ligne.put("remiseLingeId",
                            reception.getRemiseLinge().getId());
                    ligne.put("arsenalID", reception.getArsenalID());
                    ligne.put("dateReception", reception.getDateReception());
                    ligne.put("statutReception",
                            reception.getStatutReception());
                    ligne.put("observations", reception.getObservations());
                    return ligne;
                })
                .toList();


        return ResponseEntity.ok(result);
    }

    @GetMapping("/agent/{badgeAgent}/remises")
    public ResponseEntity<Map<String, Object>> getRemisesAgent(@PathVariable String badgeAgent) {
        return ResponseEntity.ok(receptionArsenalService.getRemisesAgent(badgeAgent));
    }

    @GetMapping("/remise/{remiseLingeId}/linges-a-ranger")
    public ResponseEntity<Map<String, Object>> getLingesARanger(@PathVariable Long remiseLingeId) {
        return ResponseEntity.ok(receptionArsenalService.getLingesARanger(remiseLingeId));
    }

    @PostMapping("/remise/{remiseLingeId}/ranger-tout")
    public ResponseEntity<Map<String, Object>> rangerTout(
            @PathVariable Long remiseLingeId,
            @RequestParam Long arsenalID,
            @RequestParam String emplacement,
            @RequestParam Integer quantiteComptee,
            @RequestParam(required = false) String badgeAgent) {
        return ResponseEntity.ok(receptionArsenalService.rangerTout(
                remiseLingeId, arsenalID, emplacement, quantiteComptee, badgeAgent));
    }

    @PostMapping("/remise/{remiseLingeId}/ranger-linge")
    public ResponseEntity<Map<String, Object>> rangerLinge(
            @PathVariable Long remiseLingeId,
            @RequestParam Long arsenalID,
            @RequestParam String emplacement,
            @RequestParam String codeLinge,
            @RequestParam(required = false) String badgeAgent) {
        return ResponseEntity.ok(receptionArsenalService.rangerLinge(
                remiseLingeId, arsenalID, emplacement, codeLinge, badgeAgent));
    }

    @GetMapping("/arsenaux/{arsenalID}/stock")
    public ResponseEntity<Map<String, Object>> getStockArsenal(@PathVariable Long arsenalID) {
        return ResponseEntity.ok(receptionArsenalService.getStockArsenal(arsenalID));
    }

    @GetMapping("/emplacements/{arsenalEmplacementID}/linges")
    public ResponseEntity<List<Map<String, Object>>> getContenuEmplacement(
            @PathVariable Long arsenalEmplacementID) {
        return ResponseEntity.ok(receptionArsenalService.getContenuEmplacement(arsenalEmplacementID));
    }

    @GetMapping("/linge/{codeLinge}/localisation")
    public ResponseEntity<Map<String, Object>> localiserLinge(@PathVariable String codeLinge) {
        return ResponseEntity.ok(receptionArsenalService.localiserLinge(codeLinge));
    }

    // Retourner une erreur métier sous forme de réponse HTTP 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> gererErreurMetier(
            IllegalArgumentException exception) {

        Map<String, String> erreur = new LinkedHashMap<>();
        erreur.put("erreur", exception.getMessage());

        return ResponseEntity.badRequest().body(erreur);
    }
}
