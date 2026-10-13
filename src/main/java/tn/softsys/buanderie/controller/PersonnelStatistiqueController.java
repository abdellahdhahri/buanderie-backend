package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.PersonnelStatDTO;
import tn.softsys.buanderie.service.management.PersonnelStatistiqueService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/statistiques/personnel")
@RequiredArgsConstructor
public class PersonnelStatistiqueController {

    private final PersonnelStatistiqueService personnelStatistiqueService;

    @GetMapping
    public List<PersonnelStatDTO> getStatistiques(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin
    ) {
        return personnelStatistiqueService.getStatistiquesPersonnel(debut, fin);
    }

    @GetMapping("/{personnelId}")
    public PersonnelStatDTO getStatistiquesById(
            @PathVariable Long personnelId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin
    ) {
        return personnelStatistiqueService.getStatistiquesPersonnelById(personnelId, debut, fin);
    }
}