package tn.softsys.buanderie.Sqlserver.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.Cubix.ArsenalCubixService;
import tn.softsys.buanderie.Sqlserver.dtos.ArsenalDto;
import tn.softsys.buanderie.Sqlserver.dtos.EmplacementDto;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.SqlServerArsenalService;


import java.util.List;

@RestController("sqlServerArsenalController")
@RequestMapping("/api/sqlserver/arsenal")
@RequiredArgsConstructor
@Tag(name = "Arsenal SQL Server", description = "Consultation des Arsenals depuis SQL Server")

public class ArsenalController {
    private final ArsenalCubixService arsenalCubixService;

    @GetMapping("/arsenaux")
    public ResponseEntity<List<ArsenalDto>> getArsenaux() {
        return ResponseEntity.ok(
                arsenalCubixService.getArsenaux()
        );
    }

    @GetMapping("/emplacements/{id}")
    public ResponseEntity<EmplacementDto> getEmplacementById(
            @PathVariable Long id) {

        return arsenalCubixService.getEmplacementById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/emplacements")
    public ResponseEntity<List<EmplacementDto>> getEmplacements(
            @RequestParam(required = false) Long arsenalId) {

        return ResponseEntity.ok(
                arsenalCubixService.getEmplacements(arsenalId)
        );
    }

    @GetMapping("/emplacements/code/{codeBarre}")
    public ResponseEntity<EmplacementDto> getEmplacementByCodeBarre(
            @PathVariable String codeBarre) {

        return arsenalCubixService
                .getEmplacementByCodeBarre(codeBarre)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
