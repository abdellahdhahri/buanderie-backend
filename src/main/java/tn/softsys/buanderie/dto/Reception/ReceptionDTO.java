package tn.softsys.buanderie.dto.Reception;
import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.entity.Reception;
import java.time.LocalDateTime;
@Data @Builder
public class ReceptionDTO {
    private Long id;
    private Long bacId;
    private String bacReference;
    private Long chambreId;
    private String chambreNumero;
    private String agentMatricule;
    private String agentNom;
    private LocalDateTime dateReception;
    private Integer nombreArticles;
    private Double poidsKg;
    private String observations;

    public static ReceptionDTO from(Reception r) {
        return ReceptionDTO.builder()
            .id(r.getId())
            .bacId(r.getBac().getId())
            .bacReference(r.getBac().getReference())
            .chambreId(r.getChambre() != null ? r.getChambre().getId() : null)
            .chambreNumero(r.getChambre() != null ? r.getChambre().getNumero() : null)
            .agentMatricule(r.getAgent().getMatricule())
            .agentNom(r.getAgent().getPrenom() + " " + r.getAgent().getNom())
           // .dateReception(r.getDateReception())
            .nombreArticles(r.getNombreArticles())
            .poidsKg(r.getPoidsKg())
            .observations(r.getObservations())
            .build();
    }
}
