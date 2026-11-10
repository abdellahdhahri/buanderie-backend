package tn.softsys.buanderie.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.SeuilEtatLinge;
import tn.softsys.buanderie.entity.enums.EtatLinge;
import tn.softsys.buanderie.service.Reporting.AnalyseLingesBloquesService;

@RestController
@RequestMapping("/api/analyse/linges-bloques")
@RequiredArgsConstructor
public class AnalyseLingesBloquesController {

    private final AnalyseLingesBloquesService analyseService;

    @GetMapping
    public Map<String, Object> getRapport(
            @RequestParam(required = false) EtatLinge etat,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return analyseService.getRapport(etat, serviceId, page, size);
    }

    @PostMapping("/analyser")
    public Map<String, Object> analyserMaintenant() {
        return analyseService.analyserEtNotifier();
    }

    @GetMapping("/seuils")
    public List<SeuilEtatLinge> getSeuils() {
        return analyseService.getSeuils();
    }

    @PutMapping("/seuils/{etat}")
    public SeuilEtatLinge modifierSeuil(
            @PathVariable EtatLinge etat,
            @RequestBody Map<String, Object> corps) {

        Integer heures = corps.get("dureeMaxHeures") instanceof Number n ? n.intValue() : null;
        Boolean actif = corps.get("actif") instanceof Boolean b ? b : null;
        return analyseService.modifierSeuil(etat, heures, actif);
    }
}