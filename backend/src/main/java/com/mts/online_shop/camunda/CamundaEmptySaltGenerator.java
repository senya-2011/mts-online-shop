package com.mts.online_shop.camunda;

import org.camunda.bpm.engine.impl.digest.SaltGenerator;

/**
 * Shop passwords are verified as BCrypt(plain). Camunda normally stores BCrypt(plain + salt);
 * empty salt keeps Camunda check compatible with Spring Security hashes.
 */
public class CamundaEmptySaltGenerator implements SaltGenerator {

    @Override
    public String generateSalt() {
        return "";
    }
}
