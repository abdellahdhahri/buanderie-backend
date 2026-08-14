package tn.softsys.buanderie.Sqlserver.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.dtos.LitDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.SqlServerLitService;


import java.util.List;

@RestController("sqlServerLitController")
@RequestMapping("/api/sqlserver/lits")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Lit SQL Server", description = "Consultation des lits depuis SQL Server")
public class LitController {

    private final SqlServerLitService litService;

    @GetMapping
    public Page<LitDTO> getAllLits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return litService.getAllLits(page, size);
    }
    @GetMapping("/lits-infectes")
    public List<LitDTO> getLitsInfectes() {

        return litService.getLitsInfectes();
    }
//    @GetMapping("/{id}")
//    public LitDTO getLitById(@PathVariable Long id) {
//        return litService.getLitById(id);
//    }
//    @GetMapping("/chambre/{chambreId}")
//    public Page<LitDTO> getLitsByChambreId(
//            @PathVariable Long chambreId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "200") int size
//    ) {
//        return litService.getLitsByChambreId(chambreId, page, size);
//    }


    @GetMapping("/chambre/{chambreId}")
    public Page<LitDTO> getLitsByChambre(
            @PathVariable Long chambreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {

        return litService.getLitsByChambreId(
                chambreId,
                page,
                size
        );
    }




    @GetMapping("/etat/{etat}")
    public Page<LitDTO> getLitsByEtat(
            @PathVariable Integer etat,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        return litService.getLitsByEtat(etat, page, size);
    }
    @GetMapping("/libres")
    public List<LitDTO> getLitsLibres() {
        return litService.getLitsLibres();
    }

    @GetMapping("/occupes")
    public List<LitDTO> getLitsOccupes() {
        return litService.getLitsOccupes();
    }

}
