package tn.softsys.buanderie.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.ChambreRequest;
import tn.softsys.buanderie.dto.LitDetailDTO;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.service.management.ChambreService;
import tn.softsys.buanderie.service.management.LitService;

@RestController
@RequestMapping("/api/chambres")
@RequiredArgsConstructor
public class ChambreController {

    private final ChambreService chambreService;
    private final LitService litService;

    @GetMapping
    public List<Chambre> liste(@RequestParam(defaultValue = "false") boolean tous) {
        return chambreService.findAll(tous);
    }

    @GetMapping("/{id}")
    public Chambre getById(@PathVariable Long id) {
        return chambreService.findById(id);
    }

    @GetMapping("/code/{codeBarre}")
    public Chambre getByCodeBarre(@PathVariable String codeBarre) {
        return chambreService.findByCodeBarre(codeBarre);
    }

    @GetMapping("/{id}/lits")
    public List<LitDetailDTO> lits(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean tous) {
        return litService.lister(id, tous);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Chambre creer(@RequestBody ChambreRequest request) {
        return chambreService.creer(request);
    }

    @PutMapping("/{id}")
    public Chambre modifier(@PathVariable Long id, @RequestBody ChambreRequest request) {
        return chambreService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactiver(@PathVariable Long id) {
        chambreService.desactiver(id);
    }
}
