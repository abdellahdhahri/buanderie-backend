package tn.softsys.buanderie.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.service.Reporting.AnalyseLingesIncompletsService;

@RestController
@RequestMapping("/api/analyse/linges-incomplets")
@RequiredArgsConstructor
public class AnalyseLingesIncompletsController {

    private final AnalyseLingesIncompletsService analyseService;

    @GetMapping
    public Map<String, Object> getRapport(
            @RequestParam(required = false) String attribut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return analyseService.getRapport(attribut, page, size);
    }

    @PostMapping("/analyser")
    public Map<String, Object> analyserMaintenant() {
        return analyseService.analyserEtNotifier();
    }
}
