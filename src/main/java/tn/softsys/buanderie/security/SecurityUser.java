package tn.softsys.buanderie.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SecurityUser implements UserDetails {

    private final String login;
    private final boolean admin;
    private final List<String> privileges;

    public SecurityUser(
            String login,
            boolean admin,
            List<String> privileges
    ) {
        this.login = login;
        this.admin = admin;
        this.privileges = privileges;
    }



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        if (admin) {

            return List.of(
                    new SimpleGrantedAuthority("ADMIN")
            );
        }

        return privileges.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }




    @Override
    public String getPassword() {
        return null;
    }



    @Override
    public String getUsername() {
        return login;
    }



    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }




    public String getLogin() {
        return login;
    }

    public boolean isAdmin() {
        return admin;
    }

    public List<String> getPrivileges() {
        return privileges;
    }
}