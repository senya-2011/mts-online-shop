package com.mts.online_shop.security;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "users")
public class XmlUsers {
    
    @XmlElement(name = "user")
    private List<XmlUser> users = new ArrayList<>();
    
    public List<XmlUser> getUsers() {
        return users;
    }
    
    public void setUsers(List<XmlUser> users) {
        this.users = users;
    }
}
