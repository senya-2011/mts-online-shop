package com.mts.online_shop.security;

import com.mts.online_shop.security.jaas.XmlUserLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PrivilegeService {

    private static final Logger log = LoggerFactory.getLogger(PrivilegeService.class);
    
    private final Map<String, Set<String>> rolePrivileges = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> operationPrivileges = new ConcurrentHashMap<>();
    private final Map<String, String> privilegeDescriptions = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        loadSecurityModel();
        log.info("Security model loaded with {} roles and {} privileges", 
                rolePrivileges.size(), privilegeDescriptions.size());
    }
    
    public boolean hasPrivilege(Set<String> userRoles, String privilege) {
        for (String role : userRoles) {
            if (rolePrivileges.getOrDefault(role, Collections.emptySet()).contains(privilege)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasPrivilege(String username, String privilege) {
        // Получить роли пользователя из XmlUserDetailsService или из контекста
        // Для простоты, пока оставим true, но нужно реализовать
        return true;
    }
    
    public boolean hasAnyPrivilege(String username, String... privileges) {
        return Arrays.stream(privileges).anyMatch(privilege -> hasPrivilege(username, privilege));
    }
    
    public boolean hasAllPrivileges(String username, String... privileges) {
        return Arrays.stream(privileges).allMatch(privilege -> hasPrivilege(username, privilege));
    }
    
    public Set<String> getPrivilegesForRole(String role) {
        return rolePrivileges.getOrDefault(role, Collections.emptySet());
    }
    
    public Set<String> getRequiredPrivilegesForOperation(String path, String method) {
        String operationKey = method + ":" + path;
        return operationPrivileges.getOrDefault(operationKey, Collections.emptySet());
    }
    
    public String getPrivilegeDescription(String privilege) {
        return privilegeDescriptions.get(privilege);
    }
    
    public Set<String> getAllPrivileges() {
        return new HashSet<>(privilegeDescriptions.keySet());
    }
    
    public Set<String> getAllRoles() {
        return new HashSet<>(rolePrivileges.keySet());
    }
    
    private void loadSecurityModel() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("security-model.xml");
            if (inputStream == null) {
                throw new RuntimeException("Security model file not found: security-model.xml");
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            
            loadPrivileges(document);
            loadRoles(document);
            loadOperations(document);
            
            inputStream.close();
            
        } catch (Exception e) {
            log.error("Failed to load security model: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load security model", e);
        }
    }
    
    private void loadPrivileges(Document document) {
        NodeList privilegeNodes = document.getElementsByTagName("privilege");
        for (int i = 0; i < privilegeNodes.getLength(); i++) {
            Element privilegeElement = (Element) privilegeNodes.item(i);
            String id = privilegeElement.getAttribute("id");
            String description = privilegeElement.getAttribute("description");
            privilegeDescriptions.put(id, description);
        }
        log.debug("Loaded {} privileges", privilegeDescriptions.size());
    }
    
    private void loadRoles(Document document) {
        NodeList roleNodes = document.getElementsByTagName("role");
        for (int i = 0; i < roleNodes.getLength(); i++) {
            Element roleElement = (Element) roleNodes.item(i);
            String roleId = roleElement.getAttribute("id");
            
            Set<String> privileges = new HashSet<>();
            NodeList privilegeNodes = roleElement.getElementsByTagName("privilege");
            for (int j = 0; j < privilegeNodes.getLength(); j++) {
                String privilege = privilegeNodes.item(j).getTextContent();
                privileges.add(privilege);
            }
            
            rolePrivileges.put(roleId, privileges);
        }
        log.debug("Loaded {} roles with privileges", rolePrivileges.size());
    }
    
    private void loadOperations(Document document) {
        NodeList operationNodes = document.getElementsByTagName("operation");
        for (int i = 0; i < operationNodes.getLength(); i++) {
            Element operationElement = (Element) operationNodes.item(i);
            String path = operationElement.getAttribute("path");
            String method = operationElement.getAttribute("method");
            String requiredPrivileges = operationElement.getAttribute("required-privileges");
            
            String operationKey = method + ":" + path;
            Set<String> privileges = new HashSet<>();
            if (!requiredPrivileges.isEmpty()) {
                privileges.addAll(Arrays.asList(requiredPrivileges.split("\\s*,\\s*")));
            }
            
            operationPrivileges.put(operationKey, privileges);
        }
        log.debug("Loaded {} operations with privilege requirements", operationPrivileges.size());
    }
}
