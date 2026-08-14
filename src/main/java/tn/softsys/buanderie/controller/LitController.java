package tn.softsys.buanderie.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.entity.Lit;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.ChambreRepository;
import tn.softsys.buanderie.repository.LitRepository;
import java.util.List;
@RestController @RequestMapping("/api/lits") @RequiredArgsConstructor
public class LitController {
    private final LitRepository litRepository;
    private final ChambreRepository chambreRepository;
    @GetMapping public List<Lit> liste() { return litRepository.findAll(); }
    @GetMapping("/chambre/{chambreId}") public List<Lit> getByChambre(@PathVariable Long chambreId) { return litRepository.findByChambreId(chambreId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Lit creer(@RequestBody Lit lit, @RequestParam Long chambreId) {
        Chambre c = chambreRepository.findById(chambreId).orElseThrow(() -> new ResourceNotFoundException("Chambre", chambreId));
        lit.setChambre(c); return litRepository.save(lit);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void supprimer(@PathVariable Long id) { litRepository.deleteById(id); }


}
