package tn.softsys.buanderie.dto;

import lombok.Data;

@Data
public class TriRequest {

    private String codeBarre;

    private Long bacId;

    private String matriculeAgent;
}