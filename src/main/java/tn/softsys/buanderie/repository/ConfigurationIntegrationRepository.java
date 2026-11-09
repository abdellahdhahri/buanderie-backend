package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tn.softsys.buanderie.entity.ConfigurationIntegration;

@Repository
public interface ConfigurationIntegrationRepository extends JpaRepository<ConfigurationIntegration, Long> {
}
