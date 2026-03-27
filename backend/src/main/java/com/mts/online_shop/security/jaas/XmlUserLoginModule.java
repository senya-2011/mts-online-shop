package com.mts.online_shop.security.jaas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class XmlUserLoginModule implements LoginModule {

    private static final Logger log = LoggerFactory.getLogger(XmlUserLoginModule.class);
    
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;
    
    private String username;
    private boolean succeeded = false;
    private boolean commitSucceeded = false;
    
    private XmlUserPrincipal userPrincipal;
    private Set<XmlRolePrincipal> rolePrincipals;
    
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private Map<String, XmlUser> users = new HashMap<>();
    
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, 
                          Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        
        loadUsersFromXml();
    }
    
    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("No CallbackHandler available");
        }
        
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);
        
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException("Callback handling failed: " + e.getMessage());
        }
        
        username = ((NameCallback) callbacks[0]).getName();
        char[] passwordChars = ((PasswordCallback) callbacks[1]).getPassword();
        String password = passwordChars != null ? new String(passwordChars) : "";
        
        if (passwordChars != null) {
            Arrays.fill(passwordChars, ' ');
        }
        
        if (username == null || username.isEmpty()) {
            throw new FailedLoginException("Username is required");
        }
        
        XmlUser user = users.get(username);
        if (user == null) {
            log.warn("User not found: {}", username);
            throw new FailedLoginException("User not found: " + username);
        }
        
        if (!user.isEnabled()) {
            log.warn("User account disabled: {}", username);
            throw new FailedLoginException("Account disabled: " + username);
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Invalid password for user: {}", username);
            throw new FailedLoginException("Invalid password");
        }
        
        this.userPrincipal = new XmlUserPrincipal(user);
        this.rolePrincipals = new HashSet<>();
        
        for (String roleName : user.getRoles()) {
            rolePrincipals.add(new XmlRolePrincipal(roleName));
        }
        
        log.info("User authenticated successfully: {}", username);
        succeeded = true;
        return true;
    }
    
    @Override
    public boolean commit() throws LoginException {
        if (!succeeded) {
            return false;
        }
        
        if (subject.isReadOnly()) {
            throw new LoginException("Subject is read-only");
        }
        
        subject.getPrincipals().add(userPrincipal);
        subject.getPrincipals().addAll(rolePrincipals);
        
        commitSucceeded = true;
        log.debug("Login committed for user: {}", username);
        return true;
    }
    
    @Override
    public boolean abort() throws LoginException {
        if (!succeeded) {
            return false;
        }
        
        if (commitSucceeded) {
            logout();
        } else {
            succeeded = false;
            username = null;
            userPrincipal = null;
            rolePrincipals = null;
        }
        
        return true;
    }
    
    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        subject.getPrincipals().removeAll(rolePrincipals);
        
        succeeded = false;
        commitSucceeded = false;
        username = null;
        userPrincipal = null;
        rolePrincipals = null;
        
        log.debug("User logged out: {}", username);
        return true;
    }
    
    private void loadUsersFromXml() {
        String usersFilePath = (String) options.get("usersFile");
        if (usersFilePath == null) {
            usersFilePath = "classpath:users.xml";
        }
        
        try {
            File usersFile = getUsersFile(usersFilePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(usersFile);
            
            NodeList userNodes = document.getElementsByTagName("user");
            
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userElement = (Element) userNodes.item(i);
                XmlUser user = parseUserElement(userElement);
                users.put(user.getUsername(), user);
            }
            
            log.info("Loaded {} users from XML", users.size());
            
        } catch (Exception e) {
            log.error("Failed to load users from XML: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load users configuration", e);
        }
    }
    
    private File getUsersFile(String filePath) {
        if (filePath.startsWith("classpath:")) {
            String resourcePath = filePath.substring("classpath:".length());
            var resource = getClass().getClassLoader().getResource(resourcePath);
            if (resource == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            return new File(resource.getFile());
        } else {
            return new File(filePath);
        }
    }
    
    private XmlUser parseUserElement(Element userElement) {
        String username = userElement.getAttribute("username");
        String password = userElement.getAttribute("password");
        boolean enabled = Boolean.parseBoolean(userElement.getAttribute("enabled"));
        
        XmlUser user = new XmlUser(username, password, enabled);
        
        // Parse roles
        NodeList roleNodes = userElement.getElementsByTagName("role");
        for (int i = 0; i < roleNodes.getLength(); i++) {
            String role = roleNodes.item(i).getTextContent();
            user.addRole(role);
        }
        
        // Parse attributes
        NodeList attributeNodes = userElement.getElementsByTagName("attribute");
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Element attrElement = (Element) attributeNodes.item(i);
            String name = attrElement.getAttribute("name");
            String value = attrElement.getAttribute("value");
            user.addAttribute(name, value);
        }
        
        return user;
    }
    
    public static class XmlUser {
        private final String username;
        private final String password;
        private final boolean enabled;
        private final Set<String> roles = new HashSet<>();
        private final Map<String, String> attributes = new HashMap<>();
        
        public XmlUser(String username, String password, boolean enabled) {
            this.username = username;
            this.password = password;
            this.enabled = enabled;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public boolean isEnabled() { return enabled; }
        public Set<String> getRoles() { return Collections.unmodifiableSet(roles); }
        public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }
        
        public void addRole(String role) { roles.add(role); }
        public void addAttribute(String name, String value) { attributes.put(name, value); }
    }
}
