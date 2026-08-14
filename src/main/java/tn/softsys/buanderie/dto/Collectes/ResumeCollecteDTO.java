package tn.softsys.buanderie.dto.Collectes;

import lombok.Builder;
import lombok.Data;
import tn.softsys.buanderie.dto.ResumeCategorieDTO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ResumeCollecteDTO {

    private Long collecteId;

    private LocalDateTime dateCollecte;

    private String agent;

    private Long serviceId;

    private String chambre;

    private List<ResumeCategorieDTO> categories;

    private Integer totalPieces;
}