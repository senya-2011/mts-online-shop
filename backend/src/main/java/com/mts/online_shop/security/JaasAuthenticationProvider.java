package com.mts.online_shop.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.HashSet;
import java.util.Set;

public class JaasAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(JaasAuthenticationProvider.class);
    
    private String configFile = "classpath:jaas.conf";
    private String contextName = "MTSOnlineShop";

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        try {
            // Set JAAS system properties
            System.setProperty("java.security.auth.login.config", getConfigFilePath());
            
            // Create JAAS login context
            LoginContext loginContext = new LoginContext(contextName, new JaasCallbackHandler(username, password));
            loginContext.login();
            
            // Get authenticated subject
            Subject subject = loginContext.getSubject();
            
            // Extract principals and create Spring Security authentication
            Set<SimpleGrantedAuthority> authorities = new HashSet<>();
            
            subject.getPrincipals().forEach(principal -> {
                if (principal instanceof com.mts.online_shop.security.jaas.XmlRolePrincipal) {
                    String roleName = ((com.mts.online_shop.security.jaas.XmlRolePrincipal) principal).getName();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                }
            });
            
            // Create authenticated token
            JaasAuthenticationToken result = new JaasAuthenticationToken(
                    username, 
                    password, 
                    authorities, 
                    subject
            );
            
            result.setAuthenticated(true);
            
            log.info("User authenticated successfully via JAAS: {}", username);
            return result;
            
        } catch (LoginException e) {
            log.warn("JAAS authentication failed for user {}: {}", username, e.getMessage());
            throw new BadCredentialsException("Authentication failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during JAAS authentication for user {}: {}", username, e.getMessage(), e);
            throw new BadCredentialsException("Authentication error", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JaasAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String getConfigFilePath() {
        if (configFile.startsWith("classpath:")) {
            var resource = getClass().getClassLoader().getResource(configFile.substring("classpath:".length()));
            if (resource != null) {
                return resource.getFile();
            }
        }
        return configFile;
    }
}
