package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.softsys.buanderie.entity.EnvoiCubix;
import tn.softsys.buanderie.entity.enums.StatutEnvoi;

public interface EnvoiCubixRepository extends JpaRepository<EnvoiCubix, Long> {

    List<EnvoiCubix> findByStatutOrderByDateCreationAsc(StatutEnvoi statut);

    List<EnvoiCubix> findByStatutNotOrderByDateCreationDesc(StatutEnvoi statut);
}
