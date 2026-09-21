package tn.softsys.buanderie.Sqlserver.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixLitService;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixLitsInfectesService;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitDto;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitInfecteDto;

import java.util.List;

@RestController("sqlServerLitController")
@RequestMapping("/api/sqlserver/lits")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(
        name = "Lit SQL Server",
        description = "Consultation des lits depuis Cubix"
)
public class LitController {

    private final CubixLitService litService;
    private final CubixLitsInfectesService litsInfectesService;



    @GetMapping("/{code}")
    public CubixLitDto getLitByCode(
            @PathVariable String code
    ) {
        return litService.getByCode(code);
    }



    @GetMapping("/chambre/{chambreId}")
    public List<CubixLitDto> getLitsByChambre(
            @PathVariable Long chambreId
    ) {
        return litService.getByChambreId(chambreId);
    }


    @GetMapping("/lits-infectes")
    public List<CubixLitInfecteDto> getLitsInfectes() {
        return litsInfectesService.getAll();
    }
}