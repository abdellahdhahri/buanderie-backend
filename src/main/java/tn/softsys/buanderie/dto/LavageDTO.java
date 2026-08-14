package tn.softsys.buanderie.dto;
import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.Lavage;
import java.time.LocalDateTime;
@Data @Builder
public class LavageDTO {
    private Long id;
    private Long bacId;
    private String bacReference;
    private Long machineId;
    private String machineReference;
    private Long programmeLavageId;
    private String programmeLavageNom;
    private Integer temperatureCelsius;
    private Integer dureeMinutes;
    private String agentMatricule;
    private String agentNom;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String statut;
    private String observations;

    public static LavageDTO from(Lavage l) {
        return LavageDTO.builder()
            .id(l.getId())
            .bacId(l.getBac().getId())
            .bacReference(l.getBac().getReference())
            .machineId(l.getMachine().getId())
            .machineReference(l.getMachine().getReference())
            .programmeLavageId(l.getProgrammeLavage().getId())
            .programmeLavageNom(l.getProgrammeLavage().getNom())
            .temperatureCelsius(l.getProgrammeLavage().getTemperatureCelsius())
            .dureeMinutes(l.getProgrammeLavage().getDureeMinutes())
            .agentMatricule(l.getAgent().getMatricule())
            .agentNom(l.getAgent().getPrenom() + " " + l.getAgent().getNom())
            .dateDebut(l.getDateDebut())
            .dateFin(l.getDateFin())
            .statut(l.getStatut())
            .observations(l.getObservations())
            .build();
    }
}
