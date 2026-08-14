package tn.softsys.buanderie.Sqlserver.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineDTO {
    private Long id;
    private String designation;
    private Long articleFamilleId;
    private  String codeBarre;
    private String marque ;
    private  String Code;
}
