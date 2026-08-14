package tn.softsys.buanderie.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.TypeSource;

@Entity
@Table(name = "cycle_lavage_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleLavageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference

    private CycleLavage cycleLavage;

    @Enumerated(EnumType.STRING)
    private TypeSource typeSource;


    private Long sourceId;

    private Double poidsCharge;

    private Integer quantiteChargee;
}
