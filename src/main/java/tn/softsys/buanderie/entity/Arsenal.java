package tn.softsys.buanderie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "Arsenals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Arsenal {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String code;


    @Column(name = "service_id")
    private Long serviceId;


    @PrePersist
    public void prePersist() {
        if (this.code == null) {
            this.code = "ARS" + UUID.randomUUID().toString().substring(0, 9).toUpperCase().replace("-", "X");
        }


    }
}
