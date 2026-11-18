package tn.softsys.buanderie.security;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Jetons CUBIX des utilisateurs connectés, gardés côté serveur (mémoire) : le jeton contient les identifiants
 * chiffrés, il ne part jamais vers le navigateur. Perdus au redémarrage : l'utilisateur se reconnecte.
 */
@Component
public class CubixTokenStore {

    private final Map<String, String> jetons = new ConcurrentHashMap<>();

    public void enregistrer(String login, String jeton) {
        if (login != null && jeton != null) {
            jetons.put(cle(login), jeton);
        }
    }

    public String jeton(String login) {
        return login == null ? null : jetons.get(cle(login));
    }

    public void retirer(String login) {
        if (login != null) {
            jetons.remove(cle(login));
        }
    }

    /** Login de l'utilisateur authentifié sur la requête en cours, ou null. */
    public String loginCourant() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a.getName() == null || "anonymousUser".equals(a.getName())) {
            return null;
        }
        return a.getName();
    }

    /** Jeton CUBIX de l'utilisateur connecté, ou null. */
    public String jetonCourant() {
        return jeton(loginCourant());
    }

    private String cle(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }
}
