package com.mts.online_shop.security;

public enum Privilege {
    // Привилегии для управления пользователями
    READ_USERS,
    WRITE_USERS,
    DELETE_USERS,
    
    // Привилегии для управления товарами
    READ_PRODUCTS,
    WRITE_PRODUCTS,
    DELETE_PRODUCTS,
    
    // Привилегии для управления заказами
    READ_ORDERS,
    WRITE_ORDERS,
    PROCESS_ORDERS,
    CANCEL_ORDERS,
    
    // Привилегии для управления корзиной
    READ_CART,
    WRITE_CART,
    CLEAR_CART,
    
    // Привилегии для управления системой
    READ_SYSTEM_CONFIG,
    WRITE_SYSTEM_CONFIG,
    
    // Финансовые привилегии
    PROCESS_PAYMENTS,
    VIEW_REPORTS
}
