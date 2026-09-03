package tn.softsys.buanderie.dto;


import lombok.Data;
import tn.softsys.buanderie.entity.enums.TypeMachine;

@Data
public class MachineImportRequest {

    private Long id;

    private TypeMachine type;
}