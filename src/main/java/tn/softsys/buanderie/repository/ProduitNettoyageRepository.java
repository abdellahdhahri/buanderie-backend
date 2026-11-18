package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.softsys.buanderie.entity.ProduitNettoyage;

public interface ProduitNettoyageRepository extends JpaRepository<ProduitNettoyage, Long> {

    List<ProduitNettoyage> findByActifTrueOrderByDesignationAsc();

    boolean existsByCodeBarre(String codeBarre);

    java.util.Optional<ProduitNettoyage> findByCodeBarre(String codeBarre);
}
