package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import tn.softsys.buanderie.Sqlserver.dtos.CubixImportResponse;
import tn.softsys.buanderie.Sqlserver.dtos.LingeImportableProjectionDto;
import tn.softsys.buanderie.Sqlserver.dtos.MachineImportableProjectionDto;

@Service
@RequiredArgsConstructor
public class CubixImportClient {

    private final RestTemplate restTemplate;

    @Value("${cubix.base-url}")
    private String cubixBaseUrl;


    /**
     * Récupération paginée des linges depuis Cubix.
     *
     * Utilisée pour afficher la liste des linges
     * dans le frontend.
     */
    public Page<LingeImportableProjectionDto> getLinges(
            int page,
            int size) {

        String url = UriComponentsBuilder
                .fromHttpUrl(
                        cubixBaseUrl
                                + "/Importer/BuanderieImport/Linges"
                )
                .queryParam("page", page + 1)
                .queryParam("pageSize", size)
                .toUriString();

        System.out.println("URL Cubix : " + url);

        ResponseEntity<
                CubixImportResponse<LingeImportableProjectionDto>
                > response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<
                                CubixImportResponse<LingeImportableProjectionDto>
                                >() {}
                );

        CubixImportResponse<LingeImportableProjectionDto> body =
                response.getBody();

        if (body == null) {
            throw new RuntimeException(
                    "Cubix a retourné une réponse vide"
            );
        }

        if (!body.isSuccess()) {
            throw new RuntimeException(
                    "Cubix a retourné une erreur : "
                            + body.getMessage()
            );
        }

        Pageable pageable =
                PageRequest.of(page, size);

        return new PageImpl<>(
                body.getData(),
                pageable,
                body.getTotal()
        );
    }

    /**
     * Récupération des linges sélectionnés
     * par leurs IDs Legacy.
     *
     * Utilisée lors de l'import.
     */
    public List<LingeImportableProjectionDto> getLingesByIds(
            List<Long> ids) {


        if (ids == null || ids.isEmpty()) {

            throw new RuntimeException(
                    "Aucun ID de linge fourni"
            );
        }


        String idsParam = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));


        String url = UriComponentsBuilder
                .fromHttpUrl(
                        cubixBaseUrl
                                + "/Importer/BuanderieImport/LingesByIds"
                )
                .queryParam("ids", idsParam)
                .toUriString();


        ResponseEntity<
                CubixImportResponse<LingeImportableProjectionDto>
                > response = restTemplate.exchange(

                url,

                HttpMethod.GET,

                null,

                new ParameterizedTypeReference<
                        CubixImportResponse<LingeImportableProjectionDto>
                        >() {}
        );


        CubixImportResponse<LingeImportableProjectionDto> body =
                response.getBody();


        if (body == null || !body.isSuccess()) {

            throw new RuntimeException(
                    "Impossible de récupérer les linges sélectionnés depuis Cubix"
            );
        }


        if (body.getData() == null) {

            return List.of();
        }


        return body.getData();
    }


    public List<MachineImportableProjectionDto> getMachines() {

        String url = cubixBaseUrl
                + "/Importer/BuanderieImport/Machines";

        ResponseEntity<
                CubixImportResponse<MachineImportableProjectionDto>
                > response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<
                                CubixImportResponse<MachineImportableProjectionDto>
                                >() {}
                );

        CubixImportResponse<MachineImportableProjectionDto> body =
                response.getBody();

        if (body == null) {
            throw new RuntimeException(
                    "Cubix a retourné une réponse vide"
            );
        }

        if (!body.isSuccess()) {
            throw new RuntimeException(
                    "Cubix a retourné une erreur : "
                            + body.getMessage()
            );
        }

        return body.getData() != null
                ? body.getData()
                : List.of();
    }
}
