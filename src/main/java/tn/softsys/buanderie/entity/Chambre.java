package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chambres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chambre {

    @Id
    private Long id;

    @Column
    private String code;

    @Column
    private Long serviceId;

    @Column
    private String codeBarre;
}