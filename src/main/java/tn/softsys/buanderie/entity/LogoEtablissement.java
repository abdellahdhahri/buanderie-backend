package tn.softsys.buanderie.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "logo_etablissement")
@Getter
@Setter
@NoArgsConstructor
public class LogoEtablissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_contenu", nullable = false, length = 50)
    private String typeContenu;

    @Column(nullable = false)
    private Long taille;

    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private byte[] donnees;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}
