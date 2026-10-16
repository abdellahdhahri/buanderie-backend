package tn.softsys.buanderie.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.repository.Repository;

import tn.softsys.buanderie.entity.Linge;
import tn.softsys.buanderie.entity.enums.EtatLinge;

public interface LingeArsenalRepository extends Repository<Linge, Long> {

    List<Linge> findByArsenalEmplacementIDInAndEtatOrderByCategorieAsc(
            Collection<Long> arsenalEmplacementIDs, EtatLinge etat);

    List<Linge> findByArsenalEmplacementIDAndEtatOrderByCategorieAsc(
            Long arsenalEmplacementID, EtatLinge etat);
}
