package tn.softsys.buanderie.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.ChambreDto;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.Sqlserver.Cubix.CubixChambreService;
import tn.softsys.buanderie.service.management.ChambreService;
import java.util.List;
@RestController @RequestMapping("/api/chambres") @RequiredArgsConstructor
public class ChambreController {
    private final ChambreService chambreService;

    private final CubixChambreService chambreImportService;
    @GetMapping public List<Chambre> liste() { return chambreService.findAll(); }
    //@GetMapping("/{id}") public Chambre getById(@PathVariable Long id) { return chambreService.findById(id); }
    //@GetMapping("/numero/{numero}") public Chambre getByNumero(@PathVariable String numero) { return chambreService.findByNumero(numero); }
    //@PostMapping @ResponseStatus(HttpStatus.CREATED) public Chambre creer(@Valid @RequestBody Chambre chambre) { return chambreService.creer(chambre); }
   // @PutMapping("/{id}") public Chambre modifier(@PathVariable Long id, @RequestBody Chambre chambre) { return chambreService.modifier(id, chambre); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void supprimer(@PathVariable Long id) { chambreService.supprimer(id); }
//    @GetMapping("/a-importer")
//    public ResponseEntity<List<ChambreDto>> recupererChambres() {
//
//        return ResponseEntity.ok(
//                chambreImportService
//                        .recupererChambres()
//        );
//    }

//    @PostMapping("/importer")
//    public ResponseEntity<String> importerChambres() {
//
//        int nombre =
//                chambreImportService.importerChambres();
//
//        return ResponseEntity.ok(
//                nombre + " chambres importées avec succès."
//        );
//    }
}
