package com.mts.online_shop.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import javax.security.auth.Subject;
import java.util.Collection;

public class JaasAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private Object credentials;
    private final Subject subject;

    public JaasAuthenticationToken(Object principal, Object credentials, 
                                  Collection<? extends GrantedAuthority> authorities, Subject subject) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.subject = subject;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }
}
