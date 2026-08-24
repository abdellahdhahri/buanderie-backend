package tn.softsys.buanderie.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    private Long lingeId;

    @Column(length = 100)
    private String codeBarre;

    @Column(length = 50)
    private String categorie;

    @Builder.Default
    @Column(nullable = false)
    private boolean lue = false;
}