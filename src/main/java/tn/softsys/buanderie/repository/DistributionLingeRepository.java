package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Collecte;
import tn.softsys.buanderie.entity.Distribution;
import tn.softsys.buanderie.entity.DistributionLinge;
import tn.softsys.buanderie.entity.enums.CategLinge;

import java.util.List;

public interface DistributionLingeRepository
        extends JpaRepository<DistributionLinge, Long> {
    long countByDistributionAndLinge_Categorie(
            Distribution distribution,
            CategLinge categorie);

    Long countByDistribution_CollecteAndLinge_Categorie(
            Collecte collecte,
            CategLinge categorie);
    Long countByDistribution_Collecte(Collecte collecte);


    // Compte le total des linges livrés dans une distribution (toutes catégories)
    long countByDistribution(Distribution distribution);
    List<DistributionLinge> findByDistribution_Id(Long distributionId);


}