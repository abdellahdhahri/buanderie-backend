package tn.softsys.buanderie.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.ZoneStock;
import tn.softsys.buanderie.service.Reporting.SuiviStockService;

@RestController
@RequestMapping("/api/suivi-stock")
@RequiredArgsConstructor
public class SuiviStockController {

    private final SuiviStockService suiviStockService;

    @GetMapping
    public Map<String, Object> getSuiviGlobal() {
        return suiviStockService.getSuiviGlobal();
    }

    @GetMapping("/service/{serviceId}")
    public Map<String, Object> getSuiviService(@PathVariable Long serviceId) {
        return suiviStockService.getSuiviService(serviceId);
    }

    @GetMapping("/linges")
    public Map<String, Object> getLingesParZone(
            @RequestParam ZoneStock zone,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) CategLinge categorie,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return suiviStockService.getLingesParZone(zone, serviceId, categorie, page, size);
    }
}
