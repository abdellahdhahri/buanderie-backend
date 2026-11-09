package tn.softsys.buanderie.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.dto.Referentiel.ConfigurationIntegrationRequest;
import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.JournalSynchronisation;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.TypeConnecteur;
import tn.softsys.buanderie.repository.JournalSynchronisationRepository;
import tn.softsys.buanderie.service.referentiel.IntegrationService;
import tn.softsys.buanderie.service.referentiel.ModeleConnecteur;
import tn.softsys.buanderie.service.referentiel.SynchronisationService;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;
    private final SynchronisationService synchronisationService;
    private final JournalSynchronisationRepository journalRepository;

    @GetMapping
    public Map<String, Object> get() {
        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("configuration", integrationService.getConfiguration());
        reponse.put("endpoints", integrationService.getEndpoints());
        return reponse;
    }

    @PutMapping
    public Map<String, Object> modifier(@Valid @RequestBody ConfigurationIntegrationRequest request) {
        integrationService.modifier(request);
        return get();
    }

    @PostMapping("/modele/{type}")
    public List<EndpointIntegration> appliquerModele(@PathVariable TypeConnecteur type) {
        return integrationService.appliquerModele(type);
    }

    @GetMapping("/champs/{referentiel}")
    public List<String> getChamps(@PathVariable Referentiel referentiel) {
        return ModeleConnecteur.CHAMPS.getOrDefault(referentiel, List.of());
    }

    @PostMapping("/tester/{referentiel}")
    public Map<String, Object> tester(@PathVariable Referentiel referentiel) {
        return synchronisationService.tester(referentiel);
    }

    @PostMapping("/synchroniser")
    public List<JournalSynchronisation> synchroniserTout() {
        return synchronisationService.synchroniserTout("MANUEL");
    }

    @PostMapping("/synchroniser/{referentiel}")
    public JournalSynchronisation synchroniser(@PathVariable Referentiel referentiel) {
        return synchronisationService.synchroniser(referentiel, "MANUEL");
    }

    @GetMapping("/journal")
    public List<JournalSynchronisation> getJournal(@RequestParam(required = false) Referentiel referentiel) {
        return referentiel == null
                ? journalRepository.findTop50ByOrderByDateDebutDesc()
                : journalRepository.findTop50ByReferentielOrderByDateDebutDesc(referentiel);
    }
}