package com.mts.online_shop.security;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "user")
public class XmlUser {
    
    @XmlAttribute
    private String username;
    
    @XmlAttribute
    private String password;
    
    @XmlAttribute
    private boolean enabled = true;
    
    @XmlElementWrapper(name = "roles")
    @XmlElement(name = "role")
    private List<String> roles = new ArrayList<>();
    
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    private List<XmlAttributeElement> attributes = new ArrayList<>();
    
    // Getters and setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public List<String> getRoles() {
        return roles;
    }
    
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
    
    public List<XmlAttributeElement> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(List<XmlAttributeElement> attributes) {
        this.attributes = attributes;
    }
    
    public String getAttributeValue(String name) {
        return attributes.stream()
                .filter(attr -> name.equals(attr.getName()))
                .map(XmlAttributeElement::getValue)
                .findFirst()
                .orElse(null);
    }
    
    public Map<String, String> getAttributesAsMap() {
        Map<String, String> map = new HashMap<>();
        for (XmlAttributeElement attr : attributes) {
            map.put(attr.getName(), attr.getValue());
        }
        return map;
    }
}
