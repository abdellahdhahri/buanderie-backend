package tn.softsys.buanderie.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.AffectationBacRequest;
import tn.softsys.buanderie.dto.BacDTO;
import tn.softsys.buanderie.service.Process.TriService;

@RestController
@RequestMapping("/api/tri")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TriController {

    private final TriService triService;

    @Operation(summary = "Trier un linge dans un bac de tri")
    @PostMapping
    public ResponseEntity<BacDTO> affecterAuBac(@RequestBody AffectationBacRequest request) {
        return ResponseEntity.ok(triService.affecterAuBac(request));
    }

    @GetMapping("/bacs/suggestion")
    public ResponseEntity<List<BacDTO>> suggererBacs(@RequestParam String codeOuTag) {
        return ResponseEntity.ok(triService.suggererBacs(codeOuTag));
    }
}
