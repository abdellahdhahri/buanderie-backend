package tn.softsys.buanderie.Sqlserver.sqlServerServices;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerPersonnelRepository;
import tn.softsys.buanderie.Sqlserver.dtos.PersonnelDto;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Personnel;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonnelService {

    private final SqlServerPersonnelRepository personnelRepository;

    public List<Personnel> getAll(){
        return personnelRepository.findAll().stream().toList();

    }


    public PersonnelDto getByCode(String code){
        Personnel personnel = personnelRepository.findByCodeBarre(code);
        return mapToDto(personnel);

    }
    public PersonnelDto mapToDto(Personnel personnel){
        return PersonnelDto.builder()
                .id(personnel.getId())
                .nom(personnel.getNom())
                .prenom(personnel.getPrenom())
                .codeBarre(personnel.getCodeBarre())
                .fonction(personnel.getFonction())
                .build();
    }




}
