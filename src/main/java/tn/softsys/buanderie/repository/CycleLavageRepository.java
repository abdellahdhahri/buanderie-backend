package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.CycleLavage;
import tn.softsys.buanderie.entity.enums.StatutCycle;

import java.util.List;

public interface CycleLavageRepository extends JpaRepository<CycleLavage, Long> {

    List<CycleLavage> findAllByOrderByDateDebutDesc();

    List<CycleLavage> findByStatut(StatutCycle statut);

    List<CycleLavage> findByMachine_Id(Long machineId);
}
