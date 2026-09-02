package tn.softsys.buanderie.Sqlserver.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.dtos.ChambreDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.SqlServerChambreService;

@RestController("sqlServerChambreController")
@RequestMapping("/api/sqlserver/chambres")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Chambre SQL Server", description = "Consultation des chambres depuis SQL Server")
public class ChambreController {

    private final SqlServerChambreService chambreService;
    @GetMapping
    public Page<ChambreDTO> getAllChambres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "300") int size
    ) {
        return chambreService.getAllChambres(page, size);
    }
    @GetMapping("/{id}")
    public ChambreDTO getChambreById(@PathVariable Long id) {
        return chambreService.getChambreById(id);
    }
    @GetMapping("/code/{code}")
    public ChambreDTO getChambreByCodeBarre(
            @PathVariable("code") String code) {

        return chambreService.getChambreByCodeBarre(code);
    }
    @GetMapping("/service/{serviceId}")
    public Page<ChambreDTO> getChambresByServiceId(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "300") int size
    ) {
        return chambreService.getChambresByServiceId(serviceId, page, size);
    }
}
