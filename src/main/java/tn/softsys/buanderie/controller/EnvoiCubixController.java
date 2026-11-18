package tn.softsys.buanderie.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.EnvoiCubix;
import tn.softsys.buanderie.service.referentiel.CubixEtapeSyncService;

/** Passages d'étape non reportés dans CUBIX (échecs) : consultation et renvoi. */
@RestController
@RequestMapping("/api/integration/cubix/envois")
@RequiredArgsConstructor
public class EnvoiCubixController {

    private final CubixEtapeSyncService service;

    @GetMapping
    public List<EnvoiCubix> lister(@RequestParam(defaultValue = "false") boolean tous) {
        return service.lister(tous);
    }

    @PostMapping("/renvoyer")
    public Map<String, Object> renvoyerTout() {
        return service.renvoyerTout();
    }

    @PostMapping("/{id}/renvoyer")
    public EnvoiCubix renvoyer(@PathVariable Long id) {
        return service.renvoyer(id);
    }

    @PostMapping("/{id}/abandonner")
    public EnvoiCubix abandonner(@PathVariable Long id) {
        return service.abandonner(id);
    }
}
