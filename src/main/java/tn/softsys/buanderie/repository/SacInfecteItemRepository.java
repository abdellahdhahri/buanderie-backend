package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.SacInfecteItem;

import java.util.List;

@Repository
public interface SacInfecteItemRepository extends JpaRepository<SacInfecteItem, Long> {
    List<SacInfecteItem> findBySacInfecte_Id(Long sacInfecteId);
}
