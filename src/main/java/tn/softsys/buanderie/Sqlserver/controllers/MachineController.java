package tn.softsys.buanderie.Sqlserver.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.Sqlserver.dtos.MachineDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.SqlServerMachineService;


@RestController("sqlServerMachineController")
@RequestMapping("/api/sqlserver/machines")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Machine SQL Server", description = "Consultation des machines / immobilisations depuis SQL Server")
public class MachineController {

    private final SqlServerMachineService machineService;

    @GetMapping
    public Page<MachineDTO> getAllMachines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return machineService.getAllMachines(page, size,22);
    }

    @GetMapping("/{id}")
    public MachineDTO getMachineById(@PathVariable Long id) {
        return machineService.getMachineById(id);
    }
}
