package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.enums.CategLinge;
import tn.softsys.buanderie.entity.enums.EtatLot;
import tn.softsys.buanderie.entity.Lot;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotRepository
        extends JpaRepository<Lot, Long> {

    Optional<Lot> findByTypeCollecteAndCategorieAndEtat(
            TypeCollecte typeCollecte,
            CategLinge categorie,
            EtatLot etat);
    Optional<Lot> findByCodeLot(String codeLot);
    List<Lot> findAllByTypeCollecteAndCategorieAndEtat(
            TypeCollecte typeCollecte,
            CategLinge categorie,
            EtatLot etat);
    List<Lot> findByEtat(EtatLot etatLot);
}