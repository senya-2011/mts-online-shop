package com.mts.online_shop.security;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "attribute")
public class XmlAttributeElement {
    
    @XmlAttribute
    private String name;
    
    @XmlAttribute
    private String value;
    
    public XmlAttributeElement() {}
    
    public XmlAttributeElement(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
}
