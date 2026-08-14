package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.CycleLavage;
import tn.softsys.buanderie.entity.CycleLavageItem;

import java.util.List;

public interface CycleLavageItemRepository extends JpaRepository<CycleLavageItem,Long > {
    List<CycleLavageItem> findByCycleLavage(CycleLavage cycle);
    List<CycleLavageItem> findByCycleLavage_Id(Long cycleId);

}
