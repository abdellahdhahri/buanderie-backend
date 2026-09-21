package tn.softsys.buanderie.Sqlserver.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.softsys.buanderie.Sqlserver.sqlServerEntities.AgentSql;
import tn.softsys.buanderie.Sqlserver.sqlServerServices.AgentSqlService;

import java.util.List;

@RestController
@RequestMapping("/api/sqlserver/agents")
@RequiredArgsConstructor
public class AgentSqlController {

    private final AgentSqlService agentSqlService;

    @GetMapping
    public Page<AgentSql> findAll(Pageable pageable) {
        return agentSqlService.findAll(pageable);
    }
}