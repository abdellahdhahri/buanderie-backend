
package tn.softsys.buanderie.Sqlserver.Cubix;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import tn.softsys.buanderie.Sqlserver.dtos.ArsenalDto;
import tn.softsys.buanderie.Sqlserver.dtos.CubixImportResponse;
import tn.softsys.buanderie.Sqlserver.dtos.EmplacementDto;


import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArsenalCubixService {

    private final RestTemplate restTemplate;

    @Value("${cubix.base-url}")
    private String cubixBaseUrl;

    private static final String ARSENAUX_ENDPOINT =
            "/Importer/ArsenalImport/GetArsenals";

    private static final String EMPLACEMENTS_ENDPOINT =
            "/Importer/ArsenalImport/GetEmplacements";

    /**
     * Récupère la liste des arsenaux depuis Cubix.
     */
    @Cacheable(value = "arsenaux", unless = "#result == null")
    public List<ArsenalDto> getArsenaux() {

        String url = cubixBaseUrl + ARSENAUX_ENDPOINT;

        try {
            ResponseEntity<CubixImportResponse<ArsenalDto>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<
                                    CubixImportResponse<ArsenalDto>>() {}
                    );

            CubixImportResponse<ArsenalDto> body = response.getBody();

            if (body == null) {
                throw new IllegalStateException(
                        "Réponse vide de Cubix lors de la récupération des arsenaux"
                );
            }

            if (!body.isSuccess()) {
                throw new IllegalStateException(
                        "Erreur Cubix : " + body.getMessage()
                );
            }

            return body.getData() != null
                    ? body.getData()
                    : Collections.emptyList();

        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "Impossible de récupérer les arsenaux depuis Cubix",
                    e
            );
        }
    }


    @Cacheable(
            value = "emplacements-arsenal",
            key = "#arsenalId == null ? 'TOUS' : #arsenalId",
            unless = "#result == null"
    )
    public List<EmplacementDto> getEmplacements(Long arsenalId) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(cubixBaseUrl + EMPLACEMENTS_ENDPOINT);

        if (arsenalId != null) {
            builder.queryParam("arsenalId", arsenalId);
        }

        String url = builder.build()
                .encode()
                .toUriString();

        try {
            ResponseEntity<CubixImportResponse<EmplacementDto>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<
                                    CubixImportResponse<EmplacementDto>>() {}
                    );

            CubixImportResponse<EmplacementDto> body = response.getBody();

            if (body == null) {
                throw new IllegalStateException(
                        "Réponse vide de Cubix lors de la récupération des emplacements"
                );
            }

            if (!body.isSuccess()) {
                throw new IllegalStateException(
                        "Erreur Cubix : " + body.getMessage()
                );
            }

            return body.getData() != null
                    ? body.getData()
                    : Collections.emptyList();

        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "Impossible de récupérer les emplacements depuis Cubix",
                    e
            );
        }
    }
    public Optional<EmplacementDto> getEmplacementById(Long emplacementId) {

        if (emplacementId == null) {
            throw new IllegalArgumentException(
                    "L'identifiant de l'emplacement est obligatoire"
            );
        }

        return getEmplacements(null)
                .stream()
                .filter(emplacement ->
                        Objects.equals(
                                emplacement.getEmplacementID(),
                                emplacementId
                        )
                )
                .findFirst();
    }



    public Optional<EmplacementDto> getEmplacementByCodeBarre(
            String codeBarre) {

        if (codeBarre == null || codeBarre.isBlank()) {
            throw new IllegalArgumentException(
                    "Le code-barres de l'emplacement est obligatoire"
            );
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(cubixBaseUrl + "/Importer/ArsenalImport/GetEmplacement")
                .queryParam("codeBarre", codeBarre.trim())
                .build()
                .encode()
                .toUriString();

        try {
            ResponseEntity<CubixImportResponse<EmplacementDto>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<
                                    CubixImportResponse<EmplacementDto>>() {}
                    );

            CubixImportResponse<EmplacementDto> body = response.getBody();

            if (body == null || !body.isSuccess()) {
                throw new IllegalStateException(
                        body == null
                                ? "Réponse vide de Cubix"
                                : body.getMessage()
                );
            }

            if (body.getData() == null || body.getData().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(body.getData().get(0));

        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "Impossible de récupérer l'emplacement par code-barres depuis Cubix : "
                            + e.getMessage(),
                    e
            );
        }
    }

}
