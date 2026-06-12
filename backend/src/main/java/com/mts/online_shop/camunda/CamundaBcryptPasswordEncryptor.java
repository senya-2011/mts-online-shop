package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.impl.digest.PasswordEncryptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.regex.Pattern;

/**
 * BCrypt passwords for Camunda Identity.
 * {@link org.camunda.bpm.engine.impl.digest.PasswordManager} adds the {@code {bcrypt}} DB prefix itself;
 * {@link #encrypt(String)} must return a bare {@code $2a$...} hash only.
 */
public class CamundaBcryptPasswordEncryptor implements PasswordEncryptor {

    public static final String ALGORITHM_NAME = "bcrypt";

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String encrypt(String password) {
        if (password == null) {
            return null;
        }
        if (isBcryptHash(password)) {
            return unwrapBcryptHash(password);
        }
        return encoder.encode(password);
    }

    @Override
    public boolean check(String password, String encrypted) {
        if (password == null || encrypted == null) {
            return false;
        }
        String bare = unwrapBcryptHash(encrypted);
        if (!isBcryptHash(bare)) {
            return password.equals(encrypted);
        }
        return encoder.matches(password, bare);
    }

    @Override
    public String hashAlgorithmName() {
        return ALGORITHM_NAME;
    }

    /** Format of act_id_user.pwd_: {@code {bcrypt}$2b$10$...} */
    public static String wrapForCamundaDatabase(String bcryptHash) {
        String bare = unwrapBcryptHash(bcryptHash);
        if (!isBcryptHash(bare)) {
            throw new IllegalArgumentException("Expected BCrypt hash, got: " + bcryptHash);
        }
        return "{" + ALGORITHM_NAME + "}" + bare;
    }

    public static String unwrapBcryptHash(String value) {
        if (value == null) {
            return null;
        }
        String prefix = "{" + ALGORITHM_NAME + "}";
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }

    public static boolean isBcryptHash(String value) {
        String bare = unwrapBcryptHash(value);
        if (bare == null || bare.isBlank()) {
            return false;
        }
        if (BCRYPT_PATTERN.matcher(bare).matches()) {
            return true;
        }
        // Spring BCryptPasswordEncoder: $2a$10$ + 53 символа (иногда 59/60 в зависимости от версии)
        return bare.startsWith("$2") && bare.length() >= 59 && bare.length() <= 60;
    }
}
