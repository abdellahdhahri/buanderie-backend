package tn.softsys.buanderie.dto;

import lombok.Data;

import java.util.List;

@Data
public class RFIDScanDTO {

    private String bacRfidTag;

    private List<String> rfidTags;

}
