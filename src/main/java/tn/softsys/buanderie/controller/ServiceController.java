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
import tn.softsys.buanderie.repository.ServiceRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = {"http://localhost:5173","http://localhost:3000"})
@Tag(name = "Service", description = "Gestion des services")
public class ServiceController {

    private final ServiceRepository serviceRepository;

    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
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
    public List<ServiceDTO> getAllServices() {
        return serviceRepository.findAll()
                .stream()
                .map(ServiceDTO::new)
                .collect(Collectors.toList());
    }
    @GetMapping("/{id}")
    public ResponseEntity<ServiceDTO> getServiceById(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .map(service -> ResponseEntity.ok(new ServiceDTO(service)))
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/code/{code}")
    public ServiceEntity getServiceByCode(@PathVariable String code) {
        return serviceRepository.findByCodeService(code)
              ;
    }



    @Operation(summary = "Ajouter un service")
    @PostMapping
    public ServiceEntity createService(@RequestBody ServiceEntity service) {
        return serviceRepository.save(service);
    }

    @Operation(summary = "Modifier un service")
    @PutMapping("/{id}")
    public ServiceEntity updateService(@PathVariable Long id, @RequestBody ServiceEntity serviceDetails) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé avec id " + id));
        service.setNom(serviceDetails.getNom());
        service.setDescription(serviceDetails.getDescription());
        service.setResponsable(serviceDetails.getResponsable());
        service.setTypeService(serviceDetails.getTypeService());
        return serviceRepository.save(service);
    }

    @Operation(summary = "Supprimer un service")
    @DeleteMapping("/{id}")
    public void deleteService(@PathVariable Long id) {
        serviceRepository.deleteById(id);
    }
}
