package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.RemiseLinge;

import java.util.List;


@Repository
public interface RemiseLingeRepository extends JpaRepository<RemiseLinge,Long> {
    List<RemiseLinge> findByCollecte_IdOrderByDateLivraisonDesc(
            Long collecteId
    );

    List<RemiseLinge> findByServiceOrderByDateLivraisonDesc(
            Long serviceId
    );
    List<RemiseLinge> findByCollecte_IdOrderByDateLivraisonAsc(
            Long collecteId
    );
}
