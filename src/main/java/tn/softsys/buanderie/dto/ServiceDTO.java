package tn.softsys.buanderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.softsys.buanderie.entity.ServiceEntity;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDTO {

    private Long id;
    private String nom;
    private String description;
    private String responsable;
    private String typeService;
    private String codeService;
    private String code;
    private String reference;
    private String source;
    private Boolean actif;

    public ServiceDTO(ServiceEntity s) {
        this.id = s.getId();
        this.nom = s.getNom();
        this.description = s.getDescription();
        this.responsable = s.getResponsable();
        this.typeService = s.getTypeService();
        this.codeService = s.getCodeService();
        this.code = s.getCodeService();
        this.reference = s.getReference();
        this.source = s.getSource() != null ? s.getSource().name() : null;
        this.actif = s.getActif();
    }
}