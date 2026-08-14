package tn.softsys.buanderie.dto;

import lombok.Data;

import java.util.List;

@Data
public class RFIDScanRequest {
    private Long collecteId;
    private List<String> rfidTags;
}