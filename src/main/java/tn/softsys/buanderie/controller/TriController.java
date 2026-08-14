package tn.softsys.buanderie.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.Lot.AffectationLotRequest;
import tn.softsys.buanderie.entity.Lot;
import tn.softsys.buanderie.service.Process.TriService;

@RestController
@RequestMapping("/api/tri")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TriController {

    private final TriService triService;

    @Operation(summary = "trier les linges dans des lots de tri ")
    @PostMapping
    public ResponseEntity<Lot> affecterAuLot(
            @RequestBody AffectationLotRequest request) {

        Lot lot = triService.affecterAuLot(request);

        return ResponseEntity.ok(lot);
    }
    @GetMapping("/lots/suggestion")
    public ResponseEntity<List<Lot>> suggererLots(
            @RequestParam String codeOuTag) {

        return ResponseEntity.ok(
                triService.suggererLots(codeOuTag));
    }
}
