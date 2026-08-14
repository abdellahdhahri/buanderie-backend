package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 20)
    private String matricule;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nom;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(length = 50)
    private String poste;

    @Column(length = 20)
    private String telephone;

    @Builder.Default
    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;


    @PrePersist
    public void prePersist() {
        if (this.matricule == null) {
            this.matricule = "AG" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
        this.dateCreation = LocalDateTime.now();
    }

    }
