package tn.softsys.buanderie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.softsys.buanderie.dto.ServiceDTO;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.entity.enums.Referentiel;
import tn.softsys.buanderie.entity.enums.SourceDonnee;
import tn.softsys.buanderie.exception.BusinessException;
import tn.softsys.buanderie.repository.ServiceRepository;
import tn.softsys.buanderie.service.referentiel.IntegrationService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Tag(name = "Service", description = "Gestion des services")
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final IntegrationService integrationService;

    public ServiceController(ServiceRepository serviceRepository, IntegrationService integrationService) {
        this.serviceRepository = serviceRepository;
        this.integrationService = integrationService;
    }

    @Operation(
            summary = "Récupérer tous les services",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Liste des services",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ServiceDTO.class))
            )
    )
    @GetMapping
    public List<ServiceDTO> getAllServices(@RequestParam(defaultValue = "false") boolean tous) {
        List<ServiceEntity> services = tous
                ? serviceRepository.findAllByOrderByNomAsc()
                : serviceRepository.findByActifTrueOrderByNomAsc();
        return services.stream()
                .map(ServiceDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDTO> getServiceById(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .map(service -> ResponseEntity.ok(new ServiceDTO(service)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ref/{ref}")
    public ResponseEntity<ServiceDTO> getServiceByReference(@PathVariable String ref) {
        return serviceRepository.findByReferenceIgnoreCaseOrCodeIgnoreCase(ref, ref)
                .map(service -> ResponseEntity.ok(new ServiceDTO(service)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ServiceEntity getServiceByCode(@PathVariable String code) {
        return serviceRepository.findByCodeService(code);
    }

    @Operation(summary = "Ajouter un service")
    @PostMapping
    public ServiceEntity createService(@RequestBody ServiceEntity service) {
        if (integrationService.estImporte(Referentiel.SERVICE)) {
            throw new BusinessException(
                    "Les services sont importés depuis le système externe : ajoutez-le dans le système source.");
        }
        service.setId(null);
        service.setSource(SourceDonnee.INTERNE);
        service.setIdExterne(null);
        if (service.getActif() == null) {
            service.setActif(true);
        }
        return serviceRepository.save(service);
    }

    @Operation(summary = "Modifier un service")
    @PutMapping("/{id}")
    public ServiceEntity updateService(@PathVariable Long id, @RequestBody ServiceEntity serviceDetails) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé avec id " + id));
        verifierModifiable(service);
        service.setNom(serviceDetails.getNom());
        service.setDescription(serviceDetails.getDescription());
        service.setResponsable(serviceDetails.getResponsable());
        service.setTypeService(serviceDetails.getTypeService());
        if (serviceDetails.getReference() != null) {
            service.setReference(serviceDetails.getReference());
        }
        if (serviceDetails.getActif() != null) {
            service.setActif(serviceDetails.getActif());
        }
        return serviceRepository.save(service);
    }

    @Operation(summary = "Désactiver un service")
    @DeleteMapping("/{id}")
    public void deleteService(@PathVariable Long id) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé avec id " + id));
        verifierModifiable(service);
        service.setActif(false);
        serviceRepository.save(service);
    }

    private void verifierModifiable(ServiceEntity service) {
        if (service.getSource() == SourceDonnee.EXTERNE) {
            throw new BusinessException(
                    "Ce service est synchronisé depuis le système externe : modifiez-le dans le système source.");
        }
    }
}