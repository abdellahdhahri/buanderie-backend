package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "receptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bac_id", nullable = false)
    private Bac bac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chambre_id")
    private Chambre chambre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;



    @Column(name = "nombre_articles")
    private Integer nombreArticles;

    @Column(name = "poids_kg")
    private Double poidsKg;

    @Column(length = 500)
    private String observations;


}
