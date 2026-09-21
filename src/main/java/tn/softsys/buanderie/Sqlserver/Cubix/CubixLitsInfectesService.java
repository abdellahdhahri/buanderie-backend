package tn.softsys.buanderie.Sqlserver.Cubix;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitInfecteDto;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitsInfectesResponseDto;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CubixLitsInfectesService {

    @Value("${cubix.base-url}")
    private String cubixBaseUrl;

    private final RestTemplate restTemplate;

    public List<CubixLitInfecteDto> getAll() {

        String url = cubixBaseUrl
                + "/Importer/BuanderieImport/GetLitsInfectes";

        try {

            System.out.println("======================================");
            System.out.println("URL CUBIX LITS INFECTES = " + url);
            System.out.println("======================================");

            ResponseEntity<CubixLitsInfectesResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            CubixLitsInfectesResponseDto.class
                    );

            System.out.println(
                    "STATUS CUBIX = " + response.getStatusCode()
            );

            CubixLitsInfectesResponseDto body =
                    response.getBody();

            if (body == null) {

                System.out.println(
                        "REPONSE CUBIX = NULL"
                );

                return Collections.emptyList();
            }

            System.out.println(
                    "SUCCESS = " + body.getSuccess()
            );

            System.out.println(
                    "MESSAGE = " + body.getMessage()
            );

            System.out.println(
                    "NOMBRE = " + body.getNombre()
            );

            if (Boolean.FALSE.equals(body.getSuccess())) {

                throw new RuntimeException(
                        body.getMessage() != null
                                ? body.getMessage()
                                : "Erreur retournée par Cubix."
                );
            }

            if (body.getData() == null) {

                System.out.println(
                        "DATA CUBIX = NULL"
                );

                return Collections.emptyList();
            }

            System.out.println(
                    "NOMBRE DATA = "
                            + body.getData().size()
            );

            for (CubixLitInfecteDto lit : body.getData()) {

                System.out.println(
                        "LIT INFECTE -> "
                                + "ID=" + lit.getId()
                                + " | CODE=" + lit.getCode()
                                + " | CHAMBRE_ID=" + lit.getChambreId()
                                + " | INFECTE=" + lit.getEstInfecte()
                );
            }

            return body.getData();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Erreur lors de la récupération des lits infectés "
                            + "depuis Cubix : "
                            + e.getMessage(),
                    e
            );
        }
    }
}