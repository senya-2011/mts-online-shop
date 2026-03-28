package com.mts.online_shop.security;

import java.util.Set;

public enum Role {
    ADMIN(Set.of(
        Privilege.READ_USERS, Privilege.WRITE_USERS, Privilege.DELETE_USERS,
        Privilege.READ_PRODUCTS, Privilege.WRITE_PRODUCTS, Privilege.DELETE_PRODUCTS,
        Privilege.READ_ORDERS, Privilege.WRITE_ORDERS, Privilege.PROCESS_ORDERS, Privilege.CANCEL_ORDERS,
        Privilege.READ_CART, Privilege.WRITE_CART, Privilege.CLEAR_CART,
        Privilege.READ_SYSTEM_CONFIG, Privilege.WRITE_SYSTEM_CONFIG,
        Privilege.PROCESS_PAYMENTS, Privilege.VIEW_REPORTS
    )),
    
    MANAGER(Set.of(
        Privilege.READ_PRODUCTS,
        Privilege.READ_ORDERS, Privilege.WRITE_ORDERS, Privilege.PROCESS_ORDERS, Privilege.CANCEL_ORDERS,
        Privilege.READ_CART, Privilege.WRITE_CART, Privilege.CLEAR_CART,
        Privilege.VIEW_REPORTS
    )),
    
    OPERATOR(Set.of(
        Privilege.READ_PRODUCTS,
        Privilege.READ_ORDERS, Privilege.PROCESS_ORDERS,
        Privilege.READ_CART, Privilege.CLEAR_CART,
        Privilege.VIEW_REPORTS
    )),
    
    CUSTOMER(Set.of(
        Privilege.READ_PRODUCTS,
        Privilege.READ_ORDERS, Privilege.WRITE_ORDERS, Privilege.CANCEL_ORDERS,
        Privilege.READ_CART, Privilege.WRITE_CART
    ));
    
    private final Set<Privilege> privileges;
    
    Role(Set<Privilege> privileges) {
        this.privileges = privileges;
    }
    
    public Set<Privilege> getPrivileges() {
        return privileges;
    }
}
