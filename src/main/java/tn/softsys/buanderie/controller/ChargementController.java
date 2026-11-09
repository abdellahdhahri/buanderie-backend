package tn.softsys.buanderie.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.ChargementDTO;
import tn.softsys.buanderie.dto.PeseeChargementRequest;
import tn.softsys.buanderie.dto.ScanChargementRequest;
import tn.softsys.buanderie.dto.ValidationChargementRequest;
import tn.softsys.buanderie.service.Process.ChargementService;

@RestController
@RequestMapping("/api/chargements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChargementController {

    private final ChargementService chargementService;

    @PostMapping("/scan")
    public ResponseEntity<ChargementDTO> scanner(@RequestBody ScanChargementRequest request) {
        return ResponseEntity.ok(chargementService.scanner(request));
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<ChargementDTO>> enAttente() {
        return ResponseEntity.ok(chargementService.enAttenteDeLavage());
    }

    @GetMapping("/{codeLot}")
    public ResponseEntity<ChargementDTO> consulter(@PathVariable String codeLot) {
        return ResponseEntity.ok(chargementService.consulter(codeLot));
    }

    @PostMapping("/{codeLot}/pesee")
    public ResponseEntity<ChargementDTO> peser(
            @PathVariable String codeLot,
            @RequestBody PeseeChargementRequest request) {
        return ResponseEntity.ok(chargementService.peser(codeLot, request));
    }

    @PostMapping("/{codeLot}/validation")
    public ResponseEntity<ChargementDTO> valider(
            @PathVariable String codeLot,
            @RequestBody ValidationChargementRequest request) {
        return ResponseEntity.ok(chargementService.valider(codeLot, request));
    }

    @DeleteMapping("/{codeLot}/linges/{codeOuTag}")
    public ResponseEntity<ChargementDTO> retirer(
            @PathVariable String codeLot,
            @PathVariable String codeOuTag,
            @RequestParam String matriculeAgent) {
        return ResponseEntity.ok(chargementService.retirer(codeLot, codeOuTag, matriculeAgent));
    }
}
