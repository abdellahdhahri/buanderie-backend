package tn.softsys.buanderie.Sqlserver.Cubix;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import tn.softsys.buanderie.Sqlserver.dtos.ChambresImportResponse;
import tn.softsys.buanderie.dto.ChambreDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CubixChambreCacheService {

    private final RestTemplate restTemplate;

    @Value("${cubix.base-url}")
    private String cubixBaseUrl;

    @Cacheable(value = "chambres-all", key = "'all'")
    public List<ChambreDto> getAllFromCubix() {

        System.out.println("🔥 APPEL CUBIX — CETTE MÉTHODE NE DOIT ÊTRE EXÉCUTÉE QU'UNE FOIS");

        String url = cubixBaseUrl
                + "/Importer/BuanderieImport/Chambres";

        try {

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            String.class
                    );

            if (response.getBody() == null ||
                    response.getBody().isBlank()) {

                throw new RuntimeException(
                        "Cubix retourne une réponse vide."
                );
            }

            ObjectMapper objectMapper = new ObjectMapper();

            ChambresImportResponse body =
                    objectMapper.readValue(
                            response.getBody(),
                            ChambresImportResponse.class
                    );

            if (!Boolean.TRUE.equals(body.getSuccess())) {

                throw new RuntimeException(
                        "Cubix success=false : "
                                + body.getMessage()
                );
            }

            if (body.getData() == null) {

                throw new RuntimeException(
                        "Cubix : data est null."
                );
            }

            System.out.println(
                    "Nombre de chambres récupérées = "
                            + body.getData().size()
            );

            return body.getData();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur Cubix chambres : "
                            + e.getMessage(),
                    e
            );
        }
    }
}