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
    public ServiceDTO(ServiceEntity s) {
        this.id = s.getId();
        this.nom = s.getNom();
        this.description = s.getDescription();
        this.responsable = s.getResponsable();
        this.typeService = s.getTypeService();
    }
}
