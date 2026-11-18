package tn.softsys.buanderie.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.RemiseHistoriqueDTO;
import tn.softsys.buanderie.entity.enums.EtatLivraison;
import tn.softsys.buanderie.service.management.RemiseHistoriqueService;

/** Historique des remises (livraisons de linge aux services). */
@RestController
@RequestMapping("/api/historique/remises")
@RequiredArgsConstructor
public class RemiseHistoriqueController {

    private final RemiseHistoriqueService service;

    @GetMapping
    public Page<RemiseHistoriqueDTO> rechercher(
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) Long personnelId,
            @RequestParam(required = false) EtatLivraison etat,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) String numero,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.rechercher(serviceId, personnelId, etat, dateDebut, dateFin, numero, page, size);
    }

    @GetMapping("/{id}")
    public RemiseHistoriqueDTO detail(@PathVariable Long id) {
        return service.detail(id);
    }
}
