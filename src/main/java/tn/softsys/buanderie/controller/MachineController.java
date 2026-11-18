package tn.softsys.buanderie.controller;

import tn.softsys.buanderie.service.referentiel.CubixMachineImportService;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final CubixMachineImportService cubixMachineImportService;


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

    /** Mode CUBIX : immobilisations importables (laveurs / séchoirs à classer dans CUBIX). */
    @GetMapping("/machinesAimporter")
    public JsonNode machinesAImporter(@RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "50") int pageSize) {
        return cubixMachineImportService.importables(q, page, pageSize);
    }

    /** Mode CUBIX : importe la sélection [{id, type: LAVEUR | SECHEUR}] puis relit les machines. */
    @PostMapping("/import")
    public Map<String, Object> importerMachines(@RequestBody List<MachineImportRequest> demandes) {
        return cubixMachineImportService.importer(demandes);
    }

    @GetMapping("/code/{code}")
    public Machine getBycode(@PathVariable String code) {
        return machineService.findByCode(code);
    }


//    @PostMapping("/laveurs")
//    public ResponseEntity<?> importerLaveurs() {
//
//        int nombre =
//                machineImportService.importerLaveurs();
//
//        return ResponseEntity.ok(
//                Map.of(
//                        "message", "Import des laveurs terminé",
//                        "nombreImporte", nombre
//                )
//        );
//    }

    //    @GetMapping("/laveurs-importables")
//    public List<LaveurImportableProjection> getLaveursImportables() {
//        return machineImportService.getLaveursImportables();
//    }
//    @GetMapping("/machinesAimporter")
//    public List<MachineImportableProjectionDto> machinesAimporter() {
//        return cubixImportClient.getMachines();
//    }
//
//    @PostMapping("/import")
//    public ResponseEntity<?> importerMachines(
//            @RequestBody List<MachineImportRequest> requests) {
//
//        try {
//
//            List<Machine> machines =
//                    machineImportService.importerMachines(requests);
//
//            return ResponseEntity.ok(machines);
//
//        } catch (RuntimeException e) {
//
//            return ResponseEntity.badRequest().body(
//                    "Erreur import machines : " + e.getMessage()
//            );
//
//        } catch (Exception e) {
//
//            return ResponseEntity.internalServerError().body(
//                    "Erreur interne : " + e.getMessage()
//            );
//        }
//    }
}
