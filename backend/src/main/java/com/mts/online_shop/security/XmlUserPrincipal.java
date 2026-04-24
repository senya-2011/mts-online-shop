package com.mts.online_shop.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class XmlUserPrincipal extends User {
    private final Long userId;

    public XmlUserPrincipal(String username, String password, boolean enabled, 
                         Collection<? extends GrantedAuthority> authorities, Long userId) {
        super(username, password, enabled, true, true, !enabled, authorities);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "XmlUserPrincipal{" +
                "userId=" + userId +
                ", username='" + getUsername() + '\'' +
                '}';
    }
}
