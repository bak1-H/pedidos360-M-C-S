package com.pedidos360.usuarios.model;

/**
 * Roles del sistema. Coinciden 1 a 1 con los App Roles definidos en el
 * App Registration de Azure AD, que MSAL expone en el claim "roles".
 */
public enum Rol {
    ADMIN,
    REPARTIDOR,
    CLIENTE
}
