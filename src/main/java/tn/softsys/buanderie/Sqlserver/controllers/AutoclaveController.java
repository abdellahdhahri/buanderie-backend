package tn.softsys.buanderie.Sqlserver.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Autoclave;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.AutoclaveService;

import java.util.List;

@RestController
@RequestMapping("/api/sqlserver/autoclaves")
@RequiredArgsConstructor
public class AutoclaveController {

    private final AutoclaveService autoclaveService;

    @GetMapping
    public List<Autoclave> getTous() {
        return autoclaveService.getTous();
    }

    @GetMapping("/laveurs")
    public List<Autoclave> getLaveurs() {
        return autoclaveService.getLaveurs();
    }
}