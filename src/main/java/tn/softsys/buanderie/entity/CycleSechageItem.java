package tn.softsys.buanderie.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import tn.softsys.buanderie.entity.enums.TypeSource;

@Entity
@Table(name = "cycles_sechage_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleSechageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_sechage_id")
    @JsonBackReference
    private CycleSechage cycleSechage;

    @Enumerated(EnumType.STRING)
    private TypeSource typeSource;

    private Long sourceId;       // lotId

    private Double poidsCharge;

    private Integer quantiteChargee;
}
