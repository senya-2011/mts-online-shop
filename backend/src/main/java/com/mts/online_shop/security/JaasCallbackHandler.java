package com.mts.online_shop.security;

import javax.security.auth.callback.*;

public class JaasCallbackHandler implements CallbackHandler {

    private final String username;
    private final String password;

    public JaasCallbackHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                ((NameCallback) callback).setName(username);
            } else if (callback instanceof PasswordCallback) {
                ((PasswordCallback) callback).setPassword(password.toCharArray());
            } else {
                throw new UnsupportedCallbackException(callback, "Unrecognized callback");
            }
        }
    }
}
