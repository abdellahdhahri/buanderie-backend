package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.service.Reporting.AnalytiqueDashboardService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AnalytiqueDashboardService service;

    @GetMapping("/analytique")
    public ResponseEntity<Map<String, Object>> getDashboardComplet() {
        return ResponseEntity.ok(service.getDashboardComplet());
    }

    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getKpis() {
        return ResponseEntity.ok(service.getKpis());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<Map<String, Object>>> getStock() {
        return ResponseEntity.ok(service.getStockParService());
    }


    @GetMapping("/alertes")
    public ResponseEntity<Map<String, Object>> getAlertes() {
        return ResponseEntity.ok(service.getAlertes());
    }

    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Object>> getDistribution() {
        return ResponseEntity.ok(service.getDistribution());
    }
}
