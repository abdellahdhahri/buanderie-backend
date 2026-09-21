package tn.softsys.buanderie.Sqlserver.Cubix;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitDto;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitResponseDto;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitsResponseDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CubixLitService {

    @Value("${cubix.base-url}")
    private String cubixBaseUrl;

    private final RestTemplate restTemplate;


    // =========================================================
    // GET LIT PAR ID
    // =========================================================

    @Cacheable(value = "lits-id", key = "#litId")
    public CubixLitDto getById(Long litId) {

        if (litId == null) {
            throw new IllegalArgumentException(
                    "L'identifiant du lit est obligatoire."
            );
        }

        System.out.println(
                "🔥 APPEL CUBIX — GET LIT PAR ID : " + litId
        );

        String url = cubixBaseUrl
                + "/Importer/BuanderieImport/GetLitById"
                + "?litId="
                + litId;

        try {

            ResponseEntity<CubixLitResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            CubixLitResponseDto.class
                    );

            CubixLitResponseDto body = response.getBody();

            if (body == null) {
                throw new RuntimeException(
                        "Réponse vide de Cubix."
                );
            }

            if (Boolean.FALSE.equals(body.getSuccess())) {
                throw new RuntimeException(
                        body.getMessage() != null
                                ? body.getMessage()
                                : "Erreur retournée par Cubix."
                );
            }

            if (body.getData() == null) {
                throw new RuntimeException(
                        "Aucun lit trouvé avec l'ID : " + litId
                );
            }

            return body.getData();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de la récupération du lit ["
                            + litId
                            + "] depuis Cubix : "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // GET LIT PAR CODE
    // =========================================================

    @Cacheable(value = "lits-code", key = "#code")
    public CubixLitDto getByCode(String code) {

        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Le code du lit est obligatoire."
            );
        }

        code = code.trim();

        System.out.println(
                "🔥 APPEL CUBIX — GET LIT PAR CODE : " + code
        );

        String url = cubixBaseUrl
                + "/Importer/BuanderieImport/GetLitByCode"
                + "?code="
                + URLEncoder.encode(
                code,
                StandardCharsets.UTF_8
        );

        try {

            ResponseEntity<CubixLitResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            CubixLitResponseDto.class
                    );

            CubixLitResponseDto body = response.getBody();

            if (body == null) {
                throw new RuntimeException(
                        "Réponse vide retournée par Cubix."
                );
            }

            if (Boolean.FALSE.equals(body.getSuccess())) {
                throw new RuntimeException(
                        body.getMessage() != null
                                ? body.getMessage()
                                : "Erreur retournée par Cubix."
                );
            }

            if (body.getData() == null) {
                throw new RuntimeException(
                        "Aucun lit trouvé avec le code : " + code
                );
            }

            return body.getData();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de la récupération du lit "
                            + "avec le code ["
                            + code
                            + "] depuis Cubix : "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // GET TOUS LES LITS D'UNE CHAMBRE
    // =========================================================

    @Cacheable(value = "lits-chambre", key = "#chambreId")
    public List<CubixLitDto> getByChambreId(Long chambreId) {

        if (chambreId == null) {
            throw new IllegalArgumentException(
                    "L'identifiant de la chambre est obligatoire."
            );
        }

        System.out.println(
                "🔥 APPEL CUBIX — GET LITS CHAMBRE : "
                        + chambreId
        );

        String url = cubixBaseUrl
                + "/Importer/BuanderieImport/LitsParChambre"
                + "?chambreId="
                + chambreId;

        try {

            ResponseEntity<CubixLitsResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            CubixLitsResponseDto.class
                    );

            CubixLitsResponseDto body = response.getBody();

            if (body == null) {
                return Collections.emptyList();
            }

            if (Boolean.FALSE.equals(body.getSuccess())) {
                throw new RuntimeException(
                        body.getMessage() != null
                                ? body.getMessage()
                                : "Erreur retournée par Cubix."
                );
            }

            if (body.getData() == null) {
                return Collections.emptyList();
            }

            System.out.println(
                    "Nombre de lits récupérés = "
                            + body.getData().size()
            );

            return body.getData();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de la récupération des lits "
                            + "de la chambre ["
                            + chambreId
                            + "] depuis Cubix : "
                            + e.getMessage(),
                    e
            );
        }
    }
}