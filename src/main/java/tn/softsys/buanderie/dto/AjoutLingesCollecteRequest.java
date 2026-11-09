package tn.softsys.buanderie.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjoutLingesCollecteRequest {

    private List<String> codesLinges;

    private String codeContenant;

    private String matriculeAdmin;
}