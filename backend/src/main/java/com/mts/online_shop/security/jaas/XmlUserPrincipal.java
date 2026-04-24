package com.mts.online_shop.security.jaas;

import java.io.Serializable;
import java.security.Principal;

public class XmlUserPrincipal implements Principal, Serializable {

    private final String name;
    private final XmlUserLoginModule.XmlUser user;

    public XmlUserPrincipal(XmlUserLoginModule.XmlUser user) {
        this.user = user;
        this.name = user.getUsername();
    }

    @Override
    public String getName() {
        return name;
    }

    public XmlUserLoginModule.XmlUser getUser() {
        return user;
    }

    public String getEmail() {
        return user.getAttributes().get("email");
    }

    public String getDisplayName() {
        return user.getAttributes().get("name");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XmlUserPrincipal that = (XmlUserPrincipal) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "XmlUserPrincipal{" +
                "name='" + name + '\'' +
                ", email='" + getEmail() + '\'' +
                ", displayName='" + getDisplayName() + '\'' +
                '}';
    }
}
