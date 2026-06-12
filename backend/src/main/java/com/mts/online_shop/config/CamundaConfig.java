package com.mts.online_shop.config;

import com.mts.online_shop.camunda.CamundaBcryptPasswordEncryptor;
import com.mts.online_shop.camunda.CamundaEmptySaltGenerator;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class CamundaConfig {

    @Bean
    public CamundaBcryptPasswordEncryptor camundaBcryptPasswordEncryptor() {
        return new CamundaBcryptPasswordEncryptor();
    }

    @Bean
    public CamundaJobExecutorActivationConfiguration camundaJobExecutorActivationConfiguration() {
        return new CamundaJobExecutorActivationConfiguration();
    }

    @Bean
    public CamundaPasswordEncryptorConfiguration camundaPasswordEncryptorConfiguration(
            CamundaBcryptPasswordEncryptor camundaBcryptPasswordEncryptor) {
        return new CamundaPasswordEncryptorConfiguration(camundaBcryptPasswordEncryptor);
    }

    static class CamundaJobExecutorActivationConfiguration extends AbstractCamundaConfiguration {

        @Override
        public void preInit(SpringProcessEngineConfiguration configuration) {
            configuration.setJobExecutorActivate(true);
        }
    }

    static class CamundaPasswordEncryptorConfiguration extends AbstractCamundaConfiguration {

        private final CamundaBcryptPasswordEncryptor passwordEncryptor;

        CamundaPasswordEncryptorConfiguration(CamundaBcryptPasswordEncryptor passwordEncryptor) {
            this.passwordEncryptor = passwordEncryptor;
        }

        @Override
        public void preInit(SpringProcessEngineConfiguration configuration) {
            configuration.setPasswordEncryptor(passwordEncryptor);
            configuration.setSaltGenerator(new CamundaEmptySaltGenerator());
            configuration.setWebappsAuthenticationLoggingEnabled(true);
            configuration.setLoginMaxAttempts(Integer.MAX_VALUE);
            configuration.setLoginDelayBase(0);
            configuration.setLoginDelayMaxTime(0);
            configuration.setLoginDelayFactor(1);
        }
    }
}
