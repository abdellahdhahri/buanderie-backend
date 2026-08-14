package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.Arsenal;

import java.util.Optional;

public interface ArsenalRepository extends JpaRepository<Arsenal,Long> {
    Arsenal findByServiceId(Long serviceId);
    boolean existsByCode(String code);
    Optional<Arsenal> findByCode(String code);


}
