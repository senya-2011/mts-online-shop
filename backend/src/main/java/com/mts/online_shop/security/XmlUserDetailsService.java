package com.mts.online_shop.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class XmlUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(XmlUserDetailsService.class);
    private static final String XML_FILE_NAME = "users.xml";
    private final Map<String, UserDetails> users = new HashMap<>();
    private final Map<String, Long> usernameToId = new HashMap<>();
    private final PrivilegeService privilegeService;
    private Path xmlFilePath;

    public XmlUserDetailsService(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    @PostConstruct
    public void init() {
        try {
            // Копируем XML из ресурсов в рабочую директорию если нужно
            xmlFilePath = Paths.get(System.getProperty("user.dir"), "data", XML_FILE_NAME);
            File dataDir = xmlFilePath.getParent().toFile();
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            if (!xmlFilePath.toFile().exists()) {
                log.info("Copying users.xml from resources to {}", xmlFilePath);
                ClassPathResource resource = new ClassPathResource(XML_FILE_NAME);
                try (InputStream is = resource.getInputStream()) {
                    Files.copy(is, xmlFilePath);
                }
            }
            
            loadUsersFromXml();
        } catch (Exception e) {
            log.error("Failed to initialize XML user storage", e);
            throw new RuntimeException("Failed to initialize XML user storage", e);
        }
    }

    private void loadUsersFromXml() {
        try {
            users.clear();
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            Document doc = builder.parse(xmlFilePath.toFile());
            doc.getDocumentElement().normalize();
            
            NodeList userNodes = doc.getElementsByTagName("user");
            log.info("Loading {} users from XML", userNodes.getLength());
            
            for (int i = 0; i < userNodes.getLength(); i++) {
                Element userElement = (Element) userNodes.item(i);
                
                String username = userElement.getAttribute("username");
                String userIdStr = userElement.getAttribute("id");
                Long userId = (userIdStr != null && !userIdStr.trim().isEmpty()) ? Long.parseLong(userIdStr.trim()) : null;
                String password = userElement.getAttribute("password");
                boolean enabled = Boolean.parseBoolean(userElement.getAttribute("enabled"));
                
                // Parse roles
                List<String> roles = new ArrayList<>();
                NodeList rolesList = userElement.getElementsByTagName("role");
                for (int j = 0; j < rolesList.getLength(); j++) {
                    roles.add(rolesList.item(j).getTextContent().trim().toUpperCase());
                }
                
                // Convert roles to authorities
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                
                // Use userId as username for principal
                String principal = userId != null ? userId.toString() : username;
                
                UserDetails userDetails = User.builder()
                        .username(principal)
                        .password(password)
                        .disabled(!enabled)
                        .authorities(authorities)
                        .build();
                
                users.put(username.toLowerCase(), userDetails);
                if (userId != null) {
                    usernameToId.put(username.toLowerCase(), userId);
                }
            }
            
            log.info("Successfully loaded {} users from XML", users.size());
            
        } catch (Exception e) {
            log.error("Failed to load users from XML", e);
            throw new RuntimeException("Failed to load users from XML", e);
        }
    }

    public void saveUser(String username, String password, List<String> roles) {
        try {
            if (users.containsKey(username.toLowerCase())) {
                throw new RuntimeException("User already exists: " + username);
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFilePath.toFile());
            
            Element root = doc.getDocumentElement();
            
            // Generate new ID
            Long newUserId = generateNewUserId();
            
            // Create new user element
            Element newUser = doc.createElement("user");
            newUser.setAttribute("id", newUserId.toString());
            newUser.setAttribute("username", username);
            newUser.setAttribute("password", password);
            newUser.setAttribute("enabled", "true");
            
            // Create roles element
            Element rolesElement = doc.createElement("roles");
            for (String role : roles) {
                Element roleEl = doc.createElement("role");
                roleEl.setTextContent(role);
                rolesElement.appendChild(roleEl);
            }
            newUser.appendChild(rolesElement);
            
            // Add user to document
            root.appendChild(newUser);
            
            // Save document
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xmlFilePath.toFile());
            transformer.transform(source, result);
            
            // Reload users
            loadUsersFromXml();
            
            log.info("Saved new user: {} with ID {} and roles {}", username, newUserId, roles);
            
        } catch (Exception e) {
            log.error("Failed to save user to XML", e);
            throw new RuntimeException("Failed to save user to XML", e);
        }
    }
    
    private Long generateNewUserId() {
        // Find max existing ID and add 1
        Long maxId = usernameToId.values().stream()
                .max(Long::compareTo)
                .orElse(0L);
        return maxId + 1;
    }

    public boolean userExists(String username) {
        return users.containsKey(username.toLowerCase());
    }

    public Long getUserIdByUsername(String username) {
        return usernameToId.get(username.toLowerCase());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Attempting to load user: {}. Available users: {}", username, users.keySet());
        UserDetails user = users.get(username.toLowerCase());
        if (user == null) {
            log.warn("User not found: {}. Available users: {}", username, users.keySet());
            throw new UsernameNotFoundException("User not found: " + username);
        }
        log.info("User found: {}", username);
        return user;
    }
}
