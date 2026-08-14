package ru.truwlf.trueauth.proxy;

import java.nio.charset.StandardCharsets;

public final class ProxyPasswordPolicy {
    private ProxyPasswordPolicy() { }

    public static boolean valid(String password) {
        return password != null && password.length() >= 6 && password.length() <= 32
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
