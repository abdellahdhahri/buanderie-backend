package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.softsys.buanderie.entity.Machine;
import tn.softsys.buanderie.entity.enums.EtatTechniqueMachine;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.entity.enums.TypeMachine;

import java.util.List;
import java.util.Optional;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    List<Machine> findByType(TypeMachine type);

    List<Machine> findByEtatTechnique(EtatTechniqueMachine etatTechnique);

    List<Machine> findByTypeAndEtatTechnique(
            TypeMachine type,
            EtatTechniqueMachine etatTechnique
    );

    Machine findByCode(String code);

    List<Machine> findBySource(SourceDonnee source);

}

