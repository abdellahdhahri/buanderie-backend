package tn.softsys.buanderie.Sqlserver.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChambreDTO {
    private Long id;
    private String code;
    private Long serviceId;
}
