package tn.softsys.buanderie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.softsys.buanderie.entity.ServiceEntity;
import java.util.List;
@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long>{
    boolean existsByNom(String nom);
    ServiceEntity findByCodeService(String code);

}
