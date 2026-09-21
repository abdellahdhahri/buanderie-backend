package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerLitRepository;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.SqlServerSejourRepository;
import tn.softsys.buanderie.Sqlserver.dtos.CubixLitDto;
//import tn.softsys.buanderie.Sqlserver.dtos.LitDTO;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Lit;


import java.util.List;

@Service("sqlServerLitService")
@RequiredArgsConstructor
public class SqlServerLitService {

    private final SqlServerLitRepository litRepository;
    private final SqlServerSejourRepository sejourRepository;
    public Page<CubixLitDto> getAllLits(int page, int size) {
        return litRepository
                .findAll(PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    public CubixLitDto getLitById(Long id) {
        Lit lit = litRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lit SQL Server introuvable"));
        return mapToDTO(lit);
    }

    public Page<CubixLitDto> getLitsByChambreId(Long chambreId, int page, int size) {
        return litRepository
                .findByChambreId(chambreId, PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    public Page<CubixLitDto> getLitsByEtat(Integer etat, int page, int size) {
        return litRepository
                .findByEtat(etat, PageRequest.of(page, size))
                .map(this::mapToDTO);
    }
    public List<CubixLitDto> getLitsInfectes() {

        return sejourRepository.findLitsInfectes()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private CubixLitDto mapToDTO(Lit lit) {
        return CubixLitDto.builder()
                .id(lit.getId())
                .code(lit.getCode())
                .description(lit.getDescription())
                .chambreId(lit.getChambre() != null ? lit.getChambre().getId() : null)
                .etat(lit.getEtat())
                //.infecte(sejourRepository.isLitInfecte(lit.getId()))

                .build();
    }
    private boolean verifierInfection(Long litId){

        return sejourRepository.isLitInfecte(litId);
    }
    public List<CubixLitDto> getLitsLibres() {

        return litRepository.findByEtat(
                        0,
                        PageRequest.of(0, 50)
                )
                .stream()
                .map(l -> CubixLitDto.builder()
                        .id(l.getId())
                        .code(l.getCode())
                        .chambreId(l.getChambre().getId())
                        .etat(l.getEtat())
                       // .infecte(sejourRepository.isLitInfecte(l.getId()))

                        .build())
                .toList();
    }
        public List<CubixLitDto> getLitsOccupes () {

            return litRepository.findByEtat(
                            0,
                            PageRequest.of(2, 50)
                    )
                    .stream()
                    .map(l -> CubixLitDto.builder()
                            .id(l.getId())
                            .code(l.getCode())
                            .chambreId(l.getChambre().getId())
                            .etat(l.getEtat())
                            //.infecte(sejourRepository.isLitInfecte(l.getId()))
                            .build())
                    .toList();
        }

    }
