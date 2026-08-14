package tn.softsys.buanderie.Sqlserver.dtos;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SejourDTO {
    private Long id;

    private Integer etatChambreActual;
    private Long litId;
    private Long serviceId;
    private Integer virus;
    private Date dateExit;
    private Long patientId;
}
