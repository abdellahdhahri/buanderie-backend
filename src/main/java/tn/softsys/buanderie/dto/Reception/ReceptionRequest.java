package tn.softsys.buanderie.dto.Reception;

import lombok.Data;

import java.util.List;


@Data
public class ReceptionRequest {

    private List<String> codesBac;
    private String matriculeAgent;
    private List<String> numerosSacsInfectes;
    private List<String> rfidTags;
}
