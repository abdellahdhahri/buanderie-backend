package tn.softsys.buanderie.Sqlserver.sqlServerServices;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.softsys.buanderie.Sqlserver.SqlServerRepositories.AgentSqlRepository;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.AgentSql;


import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentSqlService {

    private final AgentSqlRepository agentSqlRepository;


    public Page<AgentSql> findAll(Pageable pageable) {
        return agentSqlRepository.findAllAgents(pageable);
    }
}