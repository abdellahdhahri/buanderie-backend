package tn.softsys.buanderie.Sqlserver.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CubixImportResponse<T> {

    @JsonProperty("Success")
    private boolean success;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("Page")
    private int page;

    @JsonProperty("PageSize")
    private int pageSize;

    @JsonProperty("Total")
    private int total;

    @JsonProperty("Nombre")
    private int nombre;

    @JsonProperty("HasNextPage")
    private boolean hasNextPage;

    @JsonProperty("Data")
    private List<T> data;
}