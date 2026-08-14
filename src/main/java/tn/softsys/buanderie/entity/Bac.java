package tn.softsys.buanderie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.EtatBac;
import tn.softsys.buanderie.entity.enums.TypeBac;
import tn.softsys.buanderie.entity.enums.TypeCollecte;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "bacs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"linges"})
@ToString(exclude = {"linges"})
public class Bac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String reference;
    private String code;
    @Enumerated(EnumType.STRING)
    private EtatBac etat;

    @Enumerated(EnumType.STRING)
    private TypeBac typebac;

    @Builder.Default
    @OneToMany(mappedBy = "bac", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Linge> linges = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private TypeCollecte typeCollecte;
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "observations", length = 500)
    private String observations;
    @Column(name = "poids", nullable = true)
    private Double poids;
    @Column(name = "capacite_max")
    private Double capacite_max;
//    @ManyToOne
//    @JoinColumn(name = "service_id")
//    @JsonIgnore
//
//    private ServiceEntity service;
    @Column(unique = true)
    private String rfidTag;
    @PrePersist
    public void prePersist() {
        if (this.code == null) {
            this.code = "BAC" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
        this.dateCreation = LocalDateTime.now();


    }
}
