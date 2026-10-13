package tn.softsys.buanderie.Sqlserver.Cubix;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tn.softsys.buanderie.Sqlserver.dtos.PersonnelDto;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CubixPersonnelService {

    @Value("${cubix.base-url}")
    private String cubixBaseUrl;

    private final RestTemplate restTemplate;


    // =========================================================
    // GET TOUS LES PERSONNELS
    // =========================================================

    @Cacheable(value = "personnels-all", key = "'all'")
    public List<PersonnelDto> getAll() {

        System.out.println(
                "🔥 APPEL CUBIX — GET TOUS LES PERSONNELS"
        );

        String url = cubixBaseUrl
                + "/Importer/BuanderieImport/GetPersonnels";

        try {

            ResponseEntity<List<PersonnelDto>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<List<PersonnelDto>>() {}
                    );

            if (response.getBody() == null) {
                return Collections.emptyList();
            }

            System.out.println(
                    "Nombre de personnels récupérés = "
                            + response.getBody().size()
            );

            return response.getBody();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de la récupération des personnels "
                            + "depuis Cubix : "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // GET PERSONNEL PAR ID
    // =========================================================

    @Cacheable(value = "personnels-id", key = "#id")
    public PersonnelDto getById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "L'identifiant du personnel est obligatoire."
            );
        }

        System.out.println(
                "🔥 APPEL CUBIX — GET PERSONNEL PAR ID : " + id
        );

        String url = cubixBaseUrl
                + "/Importer/BuanderieImport/GetPersonnelById/"
                + id;

        try {

            ResponseEntity<PersonnelDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            PersonnelDto.class
                    );

            if (response.getBody() == null) {
                throw new RuntimeException(
                        "Personnel introuvable avec l'ID : " + id
                );
            }

            return response.getBody();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de la récupération du personnel "
                            + "avec l'ID [" + id + "] : "
                            + e.getMessage(),
                    e
            );
        }
    }





    // =========================================================
    // GET PERSONNEL PAR CODE-BARRES
    // =========================================================

    @Cacheable(value = "personnels-code", key = "#codeBarre")
    public PersonnelDto getByCode(String codeBarre) {

        if (codeBarre == null || codeBarre.isBlank()) {
            throw new IllegalArgumentException(
                    "Le code-barres du personnel est obligatoire."
            );
        }

        codeBarre = codeBarre.trim();

        System.out.println(
                "🔥 APPEL CUBIX — GET PERSONNEL PAR CODE : "
                        + codeBarre
        );

        String url = UriComponentsBuilder
                .fromHttpUrl(
                        cubixBaseUrl
                                + "/Importer/BuanderieImport/GetPersonnelByCodeBarre"
                )
                .queryParam("codeBarre", codeBarre)
                .toUriString();

        try {

            ResponseEntity<PersonnelDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            PersonnelDto.class
                    );

            if (response.getBody() == null) {
                throw new RuntimeException(
                        "Personnel introuvable : " + codeBarre
                );
            }

            return response.getBody();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de la récupération du personnel "
                            + "avec le code-barres ["
                            + codeBarre
                            + "] : "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // RESPONSE DTO
    // =========================================================

    @Data
    public static class CubixPersonnelSingleResponse {

        private Boolean success;
        private String message;
        private PersonnelDto data;
    }
}