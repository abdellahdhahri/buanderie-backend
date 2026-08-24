package tn.softsys.buanderie.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories_linge")
public class CategorieLinge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    private Boolean actif = true;
}