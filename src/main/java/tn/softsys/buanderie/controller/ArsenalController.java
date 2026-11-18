package tn.softsys.buanderie.controller;

import tn.softsys.buanderie.service.referentiel.CubixArsenalImportService;

import tn.softsys.buanderie.dto.ArsenalImportRequest;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.StockageRequest;
import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.service.Process.StockageService;
import tn.softsys.buanderie.service.management.ArsenalService;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/Arsenals")
@RequiredArgsConstructor
public class ArsenalController {
    private final StockageService stockageService;
    private final ArsenalService arsenalService;
    private final CubixArsenalImportService cubixArsenalImportService;


    /** Mode CUBIX : immobilisations importables (à classer comme arsenaux dans CUBIX). */
    @GetMapping("/arsenauxAimporter")
    public JsonNode arsenauxAImporter(@RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "50") int pageSize) {
        return cubixArsenalImportService.importables(q, page, pageSize);
    }

    /** Mode CUBIX : importe { ids, nbrCol, emplacements } puis relit arsenaux et emplacements. */
    @PostMapping("/import")
    public Map<String, Object> importerArsenaux(@RequestBody ArsenalImportRequest demande) {
        return cubixArsenalImportService.importer(demande);
    }

    /** Mode CUBIX : ajoute des emplacements (liste de noms) à un arsenal importé. */
    @PostMapping("/{id}/emplacements")
    public Map<String, Object> ajouterEmplacements(@PathVariable Long id, @RequestBody List<String> noms) {
        return cubixArsenalImportService.ajouterEmplacements(id, noms);
    }

    @GetMapping
    public List<Arsenal> getAll(){
        return  arsenalService.findAll();
    }


    @PostMapping("/stock")
    public Linge stocker(@RequestBody StockageRequest request){
        return stockageService.Stocker(request);
    }

    @GetMapping("/arsenals/{arsenalId}/linges")
    public List<Linge> getLingesByArsenal(
            @PathVariable Long arsenalId
    ) {
        return stockageService.getLingesByArsenal(arsenalId);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Arsenal create(@RequestBody Arsenal arsenal) {
        return arsenalService.save(arsenal);
    }
    @GetMapping("/{id}")
    public Arsenal getById(@PathVariable Long id) {
        return arsenalService.findById(id);
    }

    @PutMapping("/{id}")
    public Arsenal update(
            @PathVariable Long id,
            @RequestBody Arsenal arsenal) {

        return arsenalService.update(id, arsenal);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        arsenalService.delete(id);
    }
    @GetMapping("/code/{code}")
    public Optional<Arsenal> getByCode(String code){
        return  arsenalService.getBycode(code);
    }
}
