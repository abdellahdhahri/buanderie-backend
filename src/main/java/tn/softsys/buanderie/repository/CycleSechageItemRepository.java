package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.CycleSechage;
import tn.softsys.buanderie.entity.CycleSechageItem;
import java.util.List;

public interface CycleSechageItemRepository extends JpaRepository<CycleSechageItem, Long> {
    List<CycleSechageItem> findByCycleSechage(CycleSechage cycle);
    List<CycleSechageItem> findByCycleSechage_Id(Long cycleId);

}
