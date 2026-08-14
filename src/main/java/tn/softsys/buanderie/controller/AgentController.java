package tn.softsys.buanderie.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.Agent;
import tn.softsys.buanderie.service.management.AgentService;
import java.util.List;
@RestController @RequestMapping("/api/agents") @RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AgentController {
    private final AgentService agentService;
    @GetMapping public List<Agent> listeActifs() { return agentService.findActifs(); }
    @GetMapping("/tous") public List<Agent> listeTous() { return agentService.findAll(); }
    @GetMapping("/{id}") public Agent getById(@PathVariable Long id) { return agentService.findById(id); }
    @GetMapping("/matricule/{matricule}") public Agent getByMatricule(@PathVariable String matricule) { return agentService.findByMatricule(matricule); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Agent creer(@Valid @RequestBody Agent agent) { return agentService.creer(agent); }
    @PutMapping("/{id}") public Agent modifier(@PathVariable Long id, @Valid @RequestBody Agent agent) { return agentService.modifier(id, agent); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void desactiver(@PathVariable Long id) { agentService.desactiver(id); }
}
