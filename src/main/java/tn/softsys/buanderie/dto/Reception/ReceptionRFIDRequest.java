package tn.softsys.buanderie.dto.Reception;

import lombok.Data;

import java.util.List;
@Data
public class ReceptionRFIDRequest {

    private String matriculeAgent;

    private List<String> rfidTags;

    private String codeBac;
}
