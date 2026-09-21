package tn.softsys.buanderie.Sqlserver.dtos;

import lombok.Data;

@Data
public class ChambreImportDto {

    private Long id;

    private String code;

    private Long serviceId;

    private String codeBarre;
}