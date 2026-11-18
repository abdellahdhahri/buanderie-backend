package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.softsys.buanderie.entity.BacQuantite;

public interface BacQuantiteRepository extends JpaRepository<BacQuantite, Long> {

    List<BacQuantite> findByCollecteId(Long collecteId);

    List<BacQuantite> findByBac_Id(Long bacId);
}
