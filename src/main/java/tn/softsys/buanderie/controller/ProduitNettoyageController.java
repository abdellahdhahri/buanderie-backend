package tn.softsys.buanderie.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.ProduitNettoyage;
import tn.softsys.buanderie.service.management.ProduitNettoyageService;

@RestController
@RequestMapping("/api/produits-nettoyage")
@RequiredArgsConstructor
public class ProduitNettoyageController {

    private final ProduitNettoyageService service;

    @GetMapping
    public List<ProduitNettoyage> lister(@RequestParam(defaultValue = "false") boolean tous) {
        return service.lister(tous);
    }

    @GetMapping("/code/{codeBarre}")
    public ProduitNettoyage getParCode(@PathVariable String codeBarre) {
        return service.getParCode(codeBarre);
    }

    @GetMapping("/{id}")
    public ProduitNettoyage get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ProduitNettoyage creer(@RequestBody ProduitNettoyage produit) {
        return service.creer(produit);
    }

    @PutMapping("/{id}")
    public ProduitNettoyage modifier(@PathVariable Long id, @RequestBody ProduitNettoyage produit) {
        return service.modifier(id, produit);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }
}
