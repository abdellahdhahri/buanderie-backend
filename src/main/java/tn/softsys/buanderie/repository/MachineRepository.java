package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Machine;

import java.util.List;

public interface MachineRepository extends JpaRepository<Machine, Long> {
    List<Machine> findByType(String type);
    List<Machine> findByEtat(Machine.EtatMachine etat);
    List<Machine> findByTypeAndEtat(String type, Machine.EtatMachine etat);
    Machine findByCode(String code);
}
