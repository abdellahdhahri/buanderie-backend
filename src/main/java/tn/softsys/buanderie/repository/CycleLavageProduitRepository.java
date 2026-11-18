package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.softsys.buanderie.entity.CycleLavageProduit;

public interface CycleLavageProduitRepository extends JpaRepository<CycleLavageProduit, Long> {

    List<CycleLavageProduit> findByCycleLavage_Id(Long cycleId);

    boolean existsByProduit_Id(Long produitId);
}
