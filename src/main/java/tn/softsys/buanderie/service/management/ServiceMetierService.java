package tn.softsys.buanderie.service.management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.dto.ServiceDTO;
import tn.softsys.buanderie.entity.ServiceEntity;
import tn.softsys.buanderie.repository.ServiceRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceMetierService {

    private final ServiceRepository serviceRepository;

    public List<ServiceDTO> getAllServices() {
        return serviceRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


    public ServiceEntity getByCode(String code){
        ServiceEntity service = serviceRepository.findByCodeService(code);
        return service;
    }
    public ServiceDTO getServiceById(Long id) {
        ServiceEntity service = serviceRepository.findById(id).orElseThrow(() -> new RuntimeException("Service non trouvé"));

        return toDTO(service);
    }

    public ServiceDTO createService(ServiceDTO dto) {

        if (serviceRepository.existsByNom(dto.getNom())) {
            throw new RuntimeException("Service existe déjà");
        }

        ServiceEntity service = ServiceEntity.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .responsable(dto.getResponsable())
                .typeService(dto.getTypeService())
                .build();
        return toDTO(serviceRepository.save(service));
    }

    public ServiceDTO updateService(Long id, ServiceDTO dto) {

        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service non trouvé"));
        service.setNom(dto.getNom());
        service.setDescription(dto.getDescription());
        service.setResponsable(dto.getResponsable());
        service.setTypeService(dto.getTypeService());
        return toDTO(serviceRepository.save(service));
    }


    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    private ServiceDTO toDTO(ServiceEntity service) {
        return ServiceDTO.builder()
                .id(service.getId())
                .nom(service.getNom())
                .description(service.getDescription())
                .responsable(service.getResponsable())
                .typeService(service.getTypeService())
                .build();
    }

}
