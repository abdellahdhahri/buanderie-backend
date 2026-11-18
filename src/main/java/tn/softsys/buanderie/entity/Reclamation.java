package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.TypeReclamation;

import java.time.LocalDateTime;

@Entity
@Table(name = "reclamations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Linge linge;

    @ManyToOne
    private Agent agent;

    /** Personnel qui a déclaré la réclamation (l'ancien champ « agent » n'est plus alimenté). */
    private Long personnelId;

    @jakarta.persistence.Column(length = 150)
    private String personnelNom;

    /** Renseignée pour une réclamation sur une machine (le linge est alors vide). */
    @ManyToOne
    private Machine machine;

    @jakarta.persistence.Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    private TypeReclamation type;

    private LocalDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    private StatutReclamation status;


    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }

    public enum StatutReclamation {
        OUVERTE, EN_COURS, RESOLUE, FERMEE
    }

    public enum PrioriteReclamation {
        BASSE, NORMALE, HAUTE, URGENTE
    }
}
