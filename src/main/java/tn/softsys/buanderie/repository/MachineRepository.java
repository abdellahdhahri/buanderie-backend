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
    Optional<Machine> findByLegacyLotArticleImmobilisationId(Long id);

    List<Machine> findBySource(SourceDonnee source);

    @Modifying
    @Query("UPDATE Machine m SET m.source = :source, m.idExterne = CAST(m.legacyLotArticleImmobilisationId AS String) "
            + "WHERE m.source IS NULL AND m.legacyLotArticleImmobilisationId IS NOT NULL")
    int migrerMachinesImportees(@Param("source") SourceDonnee source);

    @Modifying
    @Query("UPDATE Machine m SET m.source = :source WHERE m.source IS NULL")
    int definirSourceManquante(@Param("source") SourceDonnee source);

}

