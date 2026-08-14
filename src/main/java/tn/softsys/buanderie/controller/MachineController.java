package tn.softsys.buanderie.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.service.management.MachineService;
import java.util.List;
@RestController @RequestMapping("/api/machines") @RequiredArgsConstructor
public class MachineController {
    private final MachineService machineService;
    @GetMapping public List<Machine> liste() { return machineService.findAll(); }
    @GetMapping("/{id}") public Machine getById(@PathVariable Long id) {
        return machineService.findById(id);
    }
    @GetMapping("/disponibles") public List<Machine> disponibles(@RequestParam String type) {
        return machineService.findDisponibles(type);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Machine creer(@Valid @RequestBody Machine machine) {
        return machineService.creer(machine);
    }
    @PutMapping("/{id}") public Machine modifier(@PathVariable Long id, @RequestBody Machine machine) {
        return machineService.modifier(id, machine);
    }
    @GetMapping("/code/{code}")
    public Machine getBycode(@PathVariable String code)
    {
        return machineService.findByCode(code);
    }
}
