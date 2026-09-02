package tn.softsys.buanderie.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        String authorizationHeader =
                request.getHeader("Authorization");


        // Aucun token
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }




        String token =
                authorizationHeader.substring(7);


        if (token.isBlank()) {

            filterChain.doFilter(request, response);

            return;
        }


        try {


            String login =
                    jwtService.extractLogin(token);


            if (login != null &&
                    !login.isBlank() &&
                    jwtService.isTokenValid(token, login)) {


                if (SecurityContextHolder
                        .getContext()
                        .getAuthentication() == null) {

                    List<String> privileges =
                            jwtService.extractPrivileges(token);


                    if (privileges == null) {

                        privileges = new ArrayList<>();
                    }

                    List<SimpleGrantedAuthority> authorities =
                            privileges.stream()
                                    .filter(privilege ->
                                            privilege != null &&
                                                    !privilege.isBlank()
                                    )
                                    .map(String::trim)
                                    .distinct()
                                    .map(SimpleGrantedAuthority::new)
                                    .toList();

                    boolean admin =
                            jwtService.extractAdmin(token);


                    if (admin) {

                        List<SimpleGrantedAuthority> adminAuthorities =
                                new ArrayList<>(authorities);

                        adminAuthorities.add(
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMIN"
                                )
                        );

                        authorities = adminAuthorities;
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    login,
                                    null,
                                    authorities
                            );



                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );


                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }


        } catch (Exception e) {

            SecurityContextHolder
                    .clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
