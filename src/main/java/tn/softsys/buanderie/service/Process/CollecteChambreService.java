package tn.softsys.buanderie.service.Process;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.*;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.*;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Chambre;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.Lit;
import tn.softsys.buanderie.entity.*;
import tn.softsys.buanderie.entity.enums.*;
import tn.softsys.buanderie.exception.*;
import tn.softsys.buanderie.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service @RequiredArgsConstructor @Transactional
public class CollecteChambreService {

    private final LingeRepository            lingeRepo;
    private final AgentRepository            agentRepo;
    private final BacRepository              bacRepo;
    private final CollecteRepository         collecteRepo;
    private final CollecteLingeRepository    collecteLingeRepo;
    private final HistoriqueLingeRepository  historiqueRepo;
    private final SqlServerChambreRepository sqlChambreRepo;
    private final SqlServerLitRepository     sqlLitRepo;
    private final SqlServerSejourRepository  sqlSejourRepo;

    public Map<String, Object> ouvrirChambre(String codeBarre, String matriculeAgent) {
        Agent agent = find(matriculeAgent);
        Chambre chambre = sqlChambreRepo.findByCodeBarre(codeBarre)
                .orElseThrow(() -> new BusinessException("Chambre introuvable : " + codeBarre));
        Set<Long> infectes = sqlSejourRepo.findLitsInfectes().stream().map(Lit::getId).collect(Collectors.toSet());
        List<Lit> lits = sqlLitRepo.findByChambreId(chambre.getId(), PageRequest.of(0, 50)).getContent();
        List<Map<String,Object>> litsDto = lits.stream().map(lit -> {
            List<Linge> lg = lingeRepo.findByLitIdSqlServerInAndEtat(List.of(lit.getId()), EtatLinge.LIVRE);
            lg.forEach(l -> { if (l.getStatutCollecte()==StatutCollecteLinge.PROPRE){l.setStatutCollecte(null);lingeRepo.save(l);}});
            return Map.<String,Object>of("litId",lit.getId(),"codeLit",s(lit.getCode()),
                    "estInfecte",infectes.contains(lit.getId()),"nbLinges",lg.size(),
                    "linges",lg.stream().map(l->Map.<String,Object>of("lingeId",l.getId(),
                            "codeBarre",l.getCodeBarre(),"rfid",s(l.getRfidTag()),
                            "categorie",l.getCategorie().name(),"statut","")).collect(Collectors.toList()));
        }).filter(m->(int)m.get("nbLinges")>0).collect(Collectors.toList());
        if (litsDto.isEmpty()) throw new BusinessException("Aucun linge LIVRE — chambre "+chambre.getCode());
        int nb = litsDto.stream().mapToInt(m->(int)m.get("nbLinges")).sum();
        Collecte c = collecteRepo.save(Collecte.builder().agent(agent)
                .chambreIdSqlServer(chambre.getId()).codeChambre(chambre.getCode())
                .typeCollecte(TypeCollecte.MIXTE).etatCollecte(EtatCollecte.EN_TRAITEMENT)
                        .service(chambre.getService().getId())
                .nbAttendus(nb).numeroCollecte("CH-"+System.currentTimeMillis()).build());
        return Map.of("collecteId",c.getId(),"codeChambre",chambre.getCode(),
                "nbLits",litsDto.size(),"nbLinges",nb,"lits",litsDto);
    }

    @Transactional
    public Map<String, Object> traiterLit(Long collecteId, Long litId, String codeBac) {
        Collecte c = findC(collecteId);
        Bac bac = bacRepo.findByCode(codeBac).or(()->bacRepo.findByRfidTag(codeBac))
                .orElseThrow(()->new BusinessException("Bac introuvable : "+codeBac));
        boolean inf = bac.getTypebac()==TypeBac.INFECTE;
        List<Linge> lg = lingeRepo.findByLitIdSqlServerInAndEtat(List.of(litId),EtatLinge.LIVRE)
                .stream().filter(l->l.getStatutCollecte()!=StatutCollecteLinge.PROPRE
                        &&l.getStatutCollecte()!=StatutCollecteLinge.MANQUANT).collect(Collectors.toList());
        if (lg.isEmpty()) throw new BusinessException("Aucun linge à traiter pour ce lit.");
        EtatLinge ne=inf?EtatLinge.INFECTE_COLLECTE:EtatLinge.COLLECTE;
        StatutCollecteLinge st=inf?StatutCollecteLinge.INFECTE_PRESUME:StatutCollecteLinge.BLANC;
        TypeCollecte tc=inf?TypeCollecte.INFECTE:TypeCollecte.BLANC;
        List<String> codes=new ArrayList<>();
        for(Linge l:lg){
            EtatLinge av=l.getEtat(); l.setBac(bac); l.setEtat(ne); l.setStatutCollecte(st);
            l.setChambreIdSqlServer(null);
            l.setTypeCollecte(tc); l.setLitIdSqlServer(null); lingeRepo.save(l);
            collecteLingeRepo.save(CollecteLinge.builder().collecte(c).linge(l).typeCollecte(tc)
                    .numeroSacInfecte(inf?bac.getCode():null).dateScan(LocalDateTime.now()).build());
            historiqueRepo.save(HistoriqueLinge.builder().linge(l).etatAvant(av).etatApres(ne)
                    .agent(c.getAgent()).typeOperation(inf?TypeOperation.DECLARATION_SAC_INFECTE:TypeOperation.COLLECTE)
                    .dateOperation(LocalDateTime.now()).collecte(c)
                    .observations((inf?"INFECTE":"BLANC")+" lit="+litId+" bac="+bac.getCode()).build());
            codes.add(l.getCodeBarre());
        }
        return Map.of("litId",litId,"bac",bac.getCode(),"typeBac",inf?"INFECTE":"BLANC","nbTraites",lg.size(),"codes",codes);
    }

    @Transactional
    public Map<String,Object> declarerPropre(Long collecteId, String codeOuTag) {
        Collecte c=findC(collecteId); Linge l=findL(codeOuTag);
        if(done(collecteId,l.getId())) throw new BusinessException("Déjà traité.");
        l.setStatutCollecte(StatutCollecteLinge.PROPRE); lingeRepo.save(l);
        collecteLingeRepo.save(CollecteLinge.builder().collecte(c).linge(l)
                .typeCollecte(TypeCollecte.PROPRE_EN_PLACE).dateScan(LocalDateTime.now()).build());
        return Map.of("lingeId",l.getId(),"codeBarre",l.getCodeBarre(),"statut","PROPRE");
    }

    @Transactional
    public Map<String,Object> declarerManquant(Long collecteId, Long lingeId) {
        Collecte c=findC(collecteId);
        Linge l=lingeRepo.findById(lingeId).orElseThrow(()->new ResourceNotFoundException("Linge","id",lingeId));
        EtatLinge av=l.getEtat(); l.setStatutCollecte(StatutCollecteLinge.MANQUANT); lingeRepo.save(l);
        collecteLingeRepo.save(CollecteLinge.builder().collecte(c).linge(l)
                .typeCollecte(TypeCollecte.MANQUANT).dateScan(LocalDateTime.now()).build());
        historiqueRepo.save(HistoriqueLinge.builder().linge(l).etatAvant(av).etatApres(EtatLinge.MANQUANT)
                .agent(c.getAgent()).typeOperation(TypeOperation.CHANGEMENT_ETAT_LINGE)
                .dateOperation(LocalDateTime.now()).collecte(c).observations("Manquant ch="+c.getCodeChambre()).build());
        return Map.of("lingeId",lingeId,"statut","MANQUANT");
    }

    @Transactional
    public Map<String,Object> cloturer(Long collecteId) {
        Collecte c=findC(collecteId);
        List<CollecteLinge> it=collecteLingeRepo.findByCollecte_Id(collecteId);
        if(it.isEmpty()) throw new BusinessException("Aucun linge traité.");
        long b=cnt(it,TypeCollecte.BLANC),i=cnt(it,TypeCollecte.INFECTE),
             p=cnt(it,TypeCollecte.PROPRE_EN_PLACE),m=cnt(it,TypeCollecte.MANQUANT);
        c.setNbScannésBlanc((int)b); c.setNbPropres((int)p);
        c.setNbInfectésPrésumés((int)i); c.setNbManquants((int)m);
        c.setEtatCollecte(EtatCollecte.EN_TRAITEMENT); c.setDateFin(LocalDateTime.now()); collecteRepo.save(c);
        return Map.of("collecteId",collecteId,"codeChambre",s(c.getCodeChambre()),
                "nbBlancs",b,"nbInfectes",i,"nbPropres",p,"nbManquants",m);
    }

    private Agent    find(String m)    {return agentRepo.findByMatricule(m).orElseThrow(()->new ResourceNotFoundException("Agent","matricule",m));}
    private Collecte findC(Long id)    {return collecteRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Collecte","id",id));}
    private Linge    findL(String c)   {return lingeRepo.findByCodeBarre(c).or(()->lingeRepo.findByBarcodeOrRfid(c)).orElseThrow(()->new ResourceNotFoundException("Linge","code",c));}
    private boolean  done(Long c,Long l){return collecteLingeRepo.existsByCollecte_IdAndLinge_Id(c,l);}
    private long     cnt(List<CollecteLinge> it,TypeCollecte t){return it.stream().filter(i->t==i.getTypeCollecte()).count();}
    private String   s(String v){return v!=null?v:"";}
}
