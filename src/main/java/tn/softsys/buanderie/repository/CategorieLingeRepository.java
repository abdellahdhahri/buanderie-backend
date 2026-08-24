package tn.softsys.buanderie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.softsys.buanderie.entity.CategorieLinge;

public interface CategorieLingeRepository
        extends JpaRepository<CategorieLinge, Long> {
}