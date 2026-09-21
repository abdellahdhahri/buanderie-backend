package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerServiceRepository;
import tn.softsys.buanderie.Sqlserver.dtos.CubixServiceDto;
import tn.softsys.buanderie.Sqlserver.dtos.CubixServiceResponseDto;
import tn.softsys.buanderie.Sqlserver.dtos.CubixServicesResponseDto;
import tn.softsys.buanderie.Sqlserver.dtos.ServiceDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Service;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;


@org.springframework.stereotype.Service("sqlServerServiceService")
@RequiredArgsConstructor
public class SqlServerServiceService {





        @Value("${cubix.base-url}")
        private String cubixBaseUrl;

        private final RestTemplate restTemplate;


        /**
         * Récupérer tous les services depuis Cubix.
         */
        public List<ServiceDTO> getAllServices() {

            String url = cubixBaseUrl
                    + "/Importer/BuanderieImport/GetServices";

            try {

                ResponseEntity<CubixServicesResponseDto> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                null,
                                CubixServicesResponseDto.class
                        );

                CubixServicesResponseDto body = response.getBody();

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
                    return List.of();
                }

                return body.getData()
                        .stream()
                        .map(this::mapToDTO)
                        .collect(Collectors.toList());

            } catch (Exception e) {

                throw new RuntimeException(
                        "Erreur lors de la récupération des services depuis Cubix : "
                                + e.getMessage(),
                        e
                );
            }
        }



        public ServiceDTO getServiceById(Long id) {

            if (id == null) {
                throw new IllegalArgumentException(
                        "L'identifiant du service est obligatoire."
                );
            }

            String url = cubixBaseUrl
                    + "/Importer/BuanderieImport/GetServiceById"
                    + "?serviceId="
                    + id;

            try {

                ResponseEntity<CubixServiceResponseDto> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                null,
                                CubixServiceResponseDto.class
                        );

                CubixServiceResponseDto body = response.getBody();

                if (body == null) {
                    throw new RuntimeException(
                            "Réponse vide de Cubix."
                    );
                }

                if (Boolean.FALSE.equals(body.getSuccess())) {
                    throw new RuntimeException(
                            body.getMessage() != null
                                    ? body.getMessage()
                                    : "Service introuvable dans Cubix."
                    );
                }

                if (body.getData() == null) {
                    throw new RuntimeException(
                            "Aucun service trouvé avec l'ID : " + id
                    );
                }

                return mapToDTO(body.getData());

            } catch (Exception e) {

                throw new RuntimeException(
                        "Erreur lors de la récupération du service ["
                                + id
                                + "] depuis Cubix : "
                                + e.getMessage(),
                        e
                );
            }
        }


        /**
         * Récupérer un service par sa référence.
         *
         * Exemple :
         * #17#
         */
        public ServiceDTO getByReference(String reference) {

            if (reference == null || reference.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "La référence du service est obligatoire."
                );
            }

            reference = reference.trim();

            URI uri = UriComponentsBuilder
                    .fromHttpUrl(cubixBaseUrl)
                    .path("/Importer/BuanderieImport/GetServiceByReference")
                    .queryParam("reference", reference)
                    .build()
                    .encode()
                    .toUri();

            try {

                ResponseEntity<CubixServiceResponseDto> response =
                        restTemplate.exchange(
                                uri,
                                HttpMethod.GET,
                                null,
                                CubixServiceResponseDto.class
                        );

                CubixServiceResponseDto body = response.getBody();

                if (body == null) {
                    throw new RuntimeException(
                            "Réponse vide de Cubix."
                    );
                }

                if (Boolean.FALSE.equals(body.getSuccess())) {
                    throw new RuntimeException(
                            body.getMessage() != null
                                    ? body.getMessage()
                                    : "Service introuvable dans Cubix."
                    );
                }

                if (body.getData() == null) {
                    throw new RuntimeException(
                            "Aucun service trouvé avec la référence : "
                                    + reference
                    );
                }

                return mapToDTO(body.getData());

            } catch (Exception e) {

                throw new RuntimeException(
                        "Erreur lors de la récupération du service ["
                                + reference
                                + "] depuis Cubix : "
                                + e.getMessage(),
                        e
                );
            }
        }


        /**
         * Conversion DTO Cubix -> DTO utilisé par Buanderie.
         */
        private ServiceDTO mapToDTO(CubixServiceDto service) {

            return ServiceDTO.builder()
                    .id(service.getId())
                    .code(service.getCode())
                    .description(service.getDescription())
                    .reference(service.getReference())
                    .build();
        }
    }
