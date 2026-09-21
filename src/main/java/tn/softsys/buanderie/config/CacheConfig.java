package tn.softsys.buanderie.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {

        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(1000);
    }

    @Bean
    public CacheManager cacheManager(
            Caffeine<Object, Object> caffeine
    ) {

        CaffeineCacheManager cacheManager =
                new CaffeineCacheManager(
                        "chambres-all",
                        "chambres-code",
                        "lits-id",
                        "lits-code",
                        "lits-chambre",
                        "personnels-all",
                        "personnels-id",
                        "personnels-code"
                );

        cacheManager.setCaffeine(caffeine);

        return cacheManager;
    }
}