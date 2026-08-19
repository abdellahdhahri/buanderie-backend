package tn.softsys.buanderie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.StockageRequest;
import tn.softsys.buanderie.entity.Arsenal;
import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.service.Process.StockageService;
import tn.softsys.buanderie.service.management.ArsenalService;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/Arsenals")
@RequiredArgsConstructor
public class ArsenalController {
    private final StockageService stockageService;
    private final ArsenalService arsenalService;


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
