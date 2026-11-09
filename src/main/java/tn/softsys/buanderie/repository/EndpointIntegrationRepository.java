package tn.softsys.buanderie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.EndpointIntegration;
import tn.softsys.buanderie.entity.enums.Referentiel;

@Repository
public interface EndpointIntegrationRepository extends JpaRepository<EndpointIntegration, Referentiel> {

    List<EndpointIntegration> findAllByOrderByReferentielAsc();
}
