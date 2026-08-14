package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Distribution;
import tn.softsys.buanderie.entity.DistributionItem;
import tn.softsys.buanderie.entity.enums.CategLinge;

import java.util.Optional;

public interface DistributionItemRepository extends JpaRepository<DistributionItem, Long> {

    Optional<DistributionItem> findByDistributionAndCategorie(
            Distribution distribution,
            CategLinge categorie);

}