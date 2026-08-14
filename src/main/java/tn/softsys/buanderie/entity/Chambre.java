package tn.softsys.buanderie.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "chambres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"lits", "sejours"})
@ToString(exclude = {"lits", "sejours"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class Chambre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String numero;

    @Column(nullable = false, length = 50)
    private String type;
    @Column(length = 5)
    private String etage;

    @Column(nullable = true)
    private String codeChambre;

    @Builder.Default
    @OneToMany(mappedBy = "chambre", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore

    private List<Lit> lits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "chambre", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore

    private List<Sejour> sejours = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.codeChambre == null) {
            this.codeChambre = "CH" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }
    }
}
