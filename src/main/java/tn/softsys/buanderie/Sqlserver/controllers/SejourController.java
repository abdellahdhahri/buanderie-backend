package tn.softsys.buanderie.Sqlserver.controllers;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.dtos.SejourDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.SqlServerSejourService;


@RestController("sqlServerSejourController")
@RequestMapping("/api/sejours")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Sejour", description = "Gestion des états des chambres")
public class SejourController {
    private final SqlServerSejourService sejourService;

    @GetMapping
    public Page<SejourDTO> getAllSejours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size
    ) {
        return sejourService.getAllSejours(page, size);
    }

//    @GetMapping("/lits-infectes")
//    public List<SejourDTO> getLitsInfectes() {
//
//        return sejourService.getLitsInfectes();
//    }

    @GetMapping("/{id}")
    public SejourDTO getSejourById(@PathVariable Long id) {
        return sejourService.getSejourById(id);
    }

    @GetMapping("/etat/{etat}")
    public Page<SejourDTO> getSejoursByEtat(
            @PathVariable Integer etat,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000  ") int size
    ) {

        return sejourService.getSejoursByEtat(
                etat,
                page,
                size
        );
    }
}
