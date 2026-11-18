package tn.softsys.buanderie.repository;

import tn.softsys.buanderie.entity.enums.UsageBac;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeBac;

import java.util.List;
import java.util.Optional;

public interface BacRepository extends JpaRepository<Bac, Long> {
    Optional<Bac> findByReference(String reference);

    List<Bac> findBySourceDonnee(tn.softsys.buanderie.entity.enums.SourceDonnee sourceDonnee);
    List<Bac> findByEtat(EtatBac etat);
    List<Bac> findByUsage(UsageBac usage);
    Optional<Bac> findByTypebac(TypeBac typeBac);
    Optional<Bac> findByCode(String codeBac);
    //  List<Bac> findByService_Id(Long serviceId);
    boolean existsByReference(String reference);

    // Optional<Bac> findFirstByServiceAndTypebac(ServiceEntity service, TypeBac typebac);
    Optional<Bac> findByRfidTag(String rfidTag);
}
