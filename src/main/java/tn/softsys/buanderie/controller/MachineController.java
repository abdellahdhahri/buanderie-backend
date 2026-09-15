package tn.softsys.buanderie.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.dtos.AutoclaveLegacyDTO;
import tn.softsys.buanderie.Sqlserver.dtos.MachineImportableProjectionDto;
import tn.softsys.buanderie.Sqlserver.projection.LaveurImportableProjection;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.CubixImportClient;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.MachineImportService;
import tn.softsys.buanderie.dto.MachineImportRequest;
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.service.management.MachineService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {
    private final MachineService machineService;
    private final CubixImportClient cubixImportClient;

    private final MachineImportService machineImportService;

    @GetMapping
    public List<Machine> liste() {
        return machineService.findAll();
    }

    @GetMapping("/{id}")
    public Machine getById(@PathVariable Long id) {
        return machineService.findById(id);
    }

    @GetMapping("/disponibles")
    public List<Machine> disponibles(@RequestParam String type) {
        return machineService.findDisponibles(type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Machine creer(@Valid @RequestBody Machine machine) {
        return machineService.creer(machine);
    }

    @PutMapping("/{id}")
    public Machine modifier(@PathVariable Long id, @RequestBody Machine machine) {
        return machineService.modifier(id, machine);
    }

    @GetMapping("/code/{code}")
    public Machine getBycode(@PathVariable String code) {
        return machineService.findByCode(code);
    }


    @PostMapping("/laveurs")
    public ResponseEntity<?> importerLaveurs() {

        int nombre =
                machineImportService.importerLaveurs();

        return ResponseEntity.ok(
                Map.of(
                        "message", "Import des laveurs terminé",
                        "nombreImporte", nombre
                )
        );
    }

    //    @GetMapping("/laveurs-importables")
//    public List<LaveurImportableProjection> getLaveursImportables() {
//        return machineImportService.getLaveursImportables();
//    }
    @GetMapping("/machinesAimporter")
    public List<MachineImportableProjectionDto> machinesAimporter() {
        return cubixImportClient.getMachines();
    }

    @PostMapping("/import")
    public ResponseEntity<?> importerMachines(
            @RequestBody List<MachineImportRequest> requests) {

        try {

            List<Machine> machines =
                    machineImportService.importerMachines(requests);

            return ResponseEntity.ok(machines);

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().body(
                    "Erreur import machines : " + e.getMessage()
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    "Erreur interne : " + e.getMessage()
            );
        }
    }
}
