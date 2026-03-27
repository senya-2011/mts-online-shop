package com.mts.online_shop.security.jaas;

import java.io.Serializable;
import java.security.Principal;

public class XmlRolePrincipal implements Principal, Serializable {

    private final String name;

    public XmlRolePrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XmlRolePrincipal that = (XmlRolePrincipal) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "XmlRolePrincipal{" +
                "name='" + name + '\'' +
                '}';
    }
}
