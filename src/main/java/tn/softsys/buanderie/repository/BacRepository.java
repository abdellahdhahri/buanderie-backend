package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeBac;

import java.util.List;
import java.util.Optional;

public interface BacRepository extends JpaRepository<Bac, Long> {
    Optional<Bac> findByReference(String reference);
    List<Bac> findByEtat(EtatBac etat);
    Optional<Bac> findByTypebac(TypeBac typeBac);
    Optional<Bac> findByCode(String codeBac);
  //  List<Bac> findByService_Id(Long serviceId);
    boolean existsByReference(String reference);
  //  Optional<Bac> findByServiceAndTypebac(ServiceEntity service, TypeBac typebac);
   /* Optional<Bac> findByService_NomAndTypebac(
            String nomService,
            TypeBac typebac
    );*/
   /* Optional<Bac> findByService_IdAndTypebac(
            Long serviceId,
            TypeBac typebac
    );*/
   // Optional<Bac> findFirstByServiceAndTypebac(ServiceEntity service, TypeBac typebac);
    Optional<Bac> findByRfidTag(String rfidTag);
}
