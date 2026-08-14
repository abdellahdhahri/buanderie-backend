package tn.softsys.buanderie.Sqlserver.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDTO {
    private Long id;
    private String code;
    private String description;
    private String reference;
}
