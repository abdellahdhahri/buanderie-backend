package tn.softsys.buanderie.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;


    public String generateToken(SecurityUser user) {

        Date now = new Date();

        Date expirationDate =
                new Date(now.getTime() + expiration);

        SecretKey key = getSigningKey();

        return Jwts.builder()
                .subject(user.getLogin())
                .claim("admin", user.isAdmin())
                .claim("privileges", user.getPrivileges())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(key)
                .compact();
    }



    public String extractLogin(String token) {

        return getClaims(token)
                .getSubject();
    }


    /**
     * Extrait les privilèges du JWT.
     */
    public List<String> extractPrivileges(String token) {

        return getClaims(token)
                .get("privileges", List.class);
    }


    /**
     * Extrait le statut administrateur.
     */
    public boolean extractAdmin(String token) {

        Boolean admin =
                getClaims(token)
                        .get("admin", Boolean.class);

        return Boolean.TRUE.equals(admin);
    }


    /**
     * Récupère la clé utilisée pour signer/vérifier le JWT.
     */
    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }


    /**
     * Vérifie et récupère les Claims du JWT.
     */
    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * Vérifie si le JWT est valide.
     */
    public boolean isTokenValid(
            String token,
            String login
    ) {

        try {

            String tokenLogin =
                    extractLogin(token);

            Date expirationDate =
                    getClaims(token)
                            .getExpiration();

            return tokenLogin.equals(login)
                    && expirationDate.after(new Date());

        } catch (Exception e) {

            return false;
        }
    }
}
