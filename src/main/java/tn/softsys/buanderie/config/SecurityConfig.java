    package tn.softsys.buanderie.config;

    import lombok.RequiredArgsConstructor;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.web.SecurityFilterChain;
    import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
    import org.springframework.web.cors.CorsConfiguration;
    import org.springframework.web.cors.CorsConfigurationSource;
    import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
    import tn.softsys.buanderie.security.JwtAuthenticationFilter;

    import java.util.Arrays;

    @Configuration
    @EnableMethodSecurity
    @RequiredArgsConstructor
    public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain filterChain(
                HttpSecurity http
        ) throws Exception {

            http
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(
                                    SessionCreationPolicy.STATELESS
                            )
                    )

                    .csrf(csrf -> csrf.disable())

                    // CORS
                    .cors(cors -> {})

                    // Autorisations
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/v3/api-docs/**",
                                    "/v3/api-docs.yaml",
                                    "/swagger-resources/**",
                                    "/webjars/**","/api/sqlserver/personnel/**"
                            ).permitAll()
                            // Login public
                            .requestMatchers(
                                    "/api/auth/login/**"
                            ).permitAll()
                            .requestMatchers(
                                    "/api/bacs/**"
                            ).permitAll()
                            .requestMatchers(
                                    "/api/notifications"
                            ).permitAll()
                            .requestMatchers(
                                    "/ws/**"
                            ).permitAll()
                            .requestMatchers(
                                    "/api/**"
                            ).permitAll()

                            // Tout le reste nécessite une authentification
                           // .anyRequest().authenticated()
                    )

                    .addFilterBefore(
                            jwtAuthenticationFilter,
                            UsernamePasswordAuthenticationFilter.class
                    );

            return http.build();
        }


        @Bean
        public CorsConfigurationSource corsConfigurationSource() {

            CorsConfiguration configuration =
                    new CorsConfiguration();

            configuration.setAllowedOriginPatterns(
                    Arrays.asList("*")
            );

            configuration.setAllowedMethods(
                    Arrays.asList(
                            "GET",
                            "POST",
                            "PUT",
                            "DELETE",
                            "PATCH",
                            "OPTIONS"
                    )
            );

            configuration.setAllowedHeaders(
                    Arrays.asList("*")
            );

            configuration.setAllowCredentials(false);

            UrlBasedCorsConfigurationSource source =
                    new UrlBasedCorsConfigurationSource();

            source.registerCorsConfiguration(
                    "/**",
                    configuration
            );

            return source;
        }
    }