package tn.softsys.buanderie.service.management;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.entity.CycleLavage;
import tn.softsys.buanderie.entity.CycleLavageProduit;
import tn.softsys.buanderie.entity.ProduitNettoyage;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.exception.ResourceNotFoundException;
import tn.softsys.buanderie.repository.CycleLavageProduitRepository;
import tn.softsys.buanderie.repository.ProduitNettoyageRepository;

@Service
@RequiredArgsConstructor
public class ProduitNettoyageService {

    private final ProduitNettoyageRepository repository;
    private final CycleLavageProduitRepository cycleProduitRepository;

    public List<ProduitNettoyage> lister(boolean tous) {
        return tous ? repository.findAll() : repository.findByActifTrueOrderByDesignationAsc();
    }

    public ProduitNettoyage get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Produit de nettoyage", id));
    }

    /** Produit scanné : retrouvé par son code-barres, il doit être actif. */
    public ProduitNettoyage getParCode(String codeBarre) {
        String code = codeBarre == null ? "" : codeBarre.trim();
        ProduitNettoyage p = repository.findByCodeBarre(code)
                .orElseThrow(() -> new ResourceNotFoundException("Produit de nettoyage", "codeBarre", code));
        if (!p.isActif()) {
            throw new BusinessException("Le produit « " + p.getDesignation() + " » est désactivé.");
        }
        return p;
    }

    @Transactional
    public ProduitNettoyage creer(ProduitNettoyage p) {
        valider(p);
        if (p.getCodeBarre() != null && repository.existsByCodeBarre(p.getCodeBarre())) {
            throw new BusinessException("Le code-barres « " + p.getCodeBarre() + " » est déjà utilisé.");
        }
        p.setId(null);
        return repository.save(p);
    }

    @Transactional
    public ProduitNettoyage modifier(Long id, ProduitNettoyage donnees) {
        ProduitNettoyage p = get(id);
        valider(donnees);
        if (donnees.getCodeBarre() != null && !donnees.getCodeBarre().equals(p.getCodeBarre())
                && repository.existsByCodeBarre(donnees.getCodeBarre())) {
            throw new BusinessException("Le code-barres « " + donnees.getCodeBarre() + " » est déjà utilisé.");
        }
        p.setDesignation(donnees.getDesignation());
        p.setCodeBarre(donnees.getCodeBarre());
        p.setDosage(donnees.getDosage());
        p.setDilution(donnees.getDilution());
        p.setVolumeTotal(donnees.getVolumeTotal());
        p.setConcentrationActive(donnees.getConcentrationActive());
        p.setDureeUtilisationApresOuverture(donnees.getDureeUtilisationApresOuverture());
        p.setTemperatureStockage(donnees.getTemperatureStockage());
        p.setTypeProduit(donnees.getTypeProduit());
        p.setActif(donnees.isActif());
        return repository.save(p);
    }

    /** Supprime le produit ; s'il a déjà servi dans un lavage, il est seulement désactivé (historique conservé). */
    @Transactional
    public void supprimer(Long id) {
        ProduitNettoyage p = get(id);
        if (cycleProduitRepository.existsByProduit_Id(id)) {
            p.setActif(false);
            repository.save(p);
        } else {
            repository.delete(p);
        }
    }

    /** Vérifie que les produits existent et sont actifs (avant de démarrer le cycle). */
    public List<ProduitNettoyage> verifier(List<Long> ids) {
        List<ProduitNettoyage> produits = new ArrayList<>();
        if (ids == null) {
            return produits;
        }
        for (Long id : new LinkedHashSet<>(ids)) {
            if (id == null) {
                continue;
            }
            ProduitNettoyage p = get(id);
            if (!p.isActif()) {
                throw new BusinessException("Le produit « " + p.getDesignation() + " » est désactivé.");
            }
            produits.add(p);
        }
        return produits;
    }

    @Transactional
    public void associer(CycleLavage cycle, List<ProduitNettoyage> produits) {
        for (ProduitNettoyage p : produits) {
            cycleProduitRepository.save(CycleLavageProduit.builder().cycleLavage(cycle).produit(p).build());
        }
    }

    @Transactional(readOnly = true)
    public List<ProduitNettoyage> produitsDuCycle(Long cycleId) {
        return cycleProduitRepository.findByCycleLavage_Id(cycleId).stream()
                .map(CycleLavageProduit::getProduit)
                .toList();
    }

    private void valider(ProduitNettoyage p) {
        if (p.getDesignation() == null || p.getDesignation().isBlank()) {
            throw new BusinessException("La désignation du produit est obligatoire.");
        }
        p.setDesignation(p.getDesignation().trim());
        if (p.getCodeBarre() != null && p.getCodeBarre().isBlank()) {
            p.setCodeBarre(null);
        }
    }
}
