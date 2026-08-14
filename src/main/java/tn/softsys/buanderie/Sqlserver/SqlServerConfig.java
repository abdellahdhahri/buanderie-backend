package tn.softsys.buanderie.Sqlserver;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = "tn.softsys.buanderie.Sqlserver",
        entityManagerFactoryRef = "sqlServerEntityManagerFactory",
        transactionManagerRef = "sqlServerTransactionManager"
)
public class SqlServerConfig {

    @Bean
    @ConfigurationProperties("sqlserver.datasource")
    public DataSourceProperties sqlServerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource sqlServerDataSource() {
        return sqlServerDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
    @Bean(name = "sqlServerEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean sqlServerEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {

        LocalContainerEntityManagerFactoryBean em = builder
                .dataSource(sqlServerDataSource())
                .packages("tn.softsys.buanderie.Sqlserver.sqlServerEntities")
                .persistenceUnit("sqlserver")
                .build();

        java.util.Map<String, Object> properties = new java.util.HashMap<>();

        properties.put(
                "hibernate.dialect",
                "org.hibernate.dialect.SQLServerDialect"
        );

        properties.put(
                "hibernate.hbm2ddl.auto",
                "none"
        );

        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "sqlServerTransactionManager")
    public PlatformTransactionManager sqlServerTransactionManager(
            @Qualifier("sqlServerEntityManagerFactory")
            LocalContainerEntityManagerFactoryBean sqlServerEntityManagerFactory) {

        return new JpaTransactionManager(
                sqlServerEntityManagerFactory.getObject()
        );
    }
}