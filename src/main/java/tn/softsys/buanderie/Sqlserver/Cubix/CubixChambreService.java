package tn.softsys.buanderie.Sqlserver.Cubix;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import tn.softsys.buanderie.dto.ChambreDto;
import tn.softsys.buanderie.Sqlserver.dtos.ChambresImportResponse;
import tn.softsys.buanderie.entity.Chambre;
import tn.softsys.buanderie.repository.ChambreRepository;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CubixChambreService {

    private final ChambreRepository chambreRepository;
    private final RestTemplate restTemplate;
    private final CubixChambreCacheService cacheService;

    @Value("${cubix.base-url}")
    private String cubixBaseUrl;




    public List<ChambreDto> getAll() {

        return cacheService.getAllFromCubix();
    }

    public Page<ChambreDto> getAll(Pageable pageable) {

        List<ChambreDto> chambres =
                cacheService.getAllFromCubix();

        int total = chambres.size();

        int start = (int) pageable.getOffset();

        if (start >= total) {

            return new PageImpl<>(
                    Collections.emptyList(),
                    pageable,
                    total
            );
        }

        int end = Math.min(
                start + pageable.getPageSize(),
                total
        );

        List<ChambreDto> contenu =
                chambres.subList(start, end);

        return new PageImpl<>(
                contenu,
                pageable,
                total
        );
    }

    @Cacheable(value = "chambres-code", key = "#codeBarre")
    public ChambreDto getByCodeBarre(String codeBarre) {

        if (codeBarre == null || codeBarre.isBlank()) {

            throw new IllegalArgumentException(
                    "Le code-barres de la chambre est obligatoire."
            );
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(
                        cubixBaseUrl
                                + "/Importer/BuanderieImport/GetChambreByCodeBarre"
                )
                .queryParam("codeBarre", codeBarre)
                .toUriString();

        try {

            System.out.println("=================================");
            System.out.println("APPEL CUBIX : GET CHAMBRE");
            System.out.println("CODE BARRE = " + codeBarre);
            System.out.println("URL = " + url);
            System.out.println("=================================");

            ResponseEntity<ChambreDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            ChambreDto.class
                    );

            if (response.getBody() == null) {

                throw new RuntimeException(
                        "Chambre introuvable : " + codeBarre
                );
            }

            System.out.println(
                    "Chambre récupérée depuis Cubix = "
                            + response.getBody()
            );

            return response.getBody();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Erreur lors de la récupération de la chambre ["
                            + codeBarre
                            + "] depuis Cubix : "
                            + e.getMessage(),
                    e
            );
        }
    }
}