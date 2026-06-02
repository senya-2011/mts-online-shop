package com.mts.online_shop.config;

import com.mts.online_shop.model.User;
import com.mts.online_shop.repository.UserRepository;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CamundaGroupSync {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void syncGroups() {
        List<User> allUsers = userRepository.findAll();
        // ensure default groups exist
        ensureGroupExists("CLIENT");
        ensureGroupExists("OPERATOR");
        ensureGroupExists("ADMIN");
        for (User u : allUsers) {
            String role = u.getRole();
            if (role == null) continue;
            // Map application role to Camunda group
            String groupId = mapAppRoleToGroup(role);
            if (groupId == null) continue;
            if (identityService.createGroupQuery().groupId(groupId).count() == 0) {
                Group g = identityService.newGroup(groupId);
                g.setName(groupId);
                identityService.saveGroup(g);
            }
            if (identityService.createGroupQuery().groupMember(u.getLogin()).groupId(groupId).count() == 0) {
                if (identityService.createUserQuery().userId(u.getLogin()).count() > 0) {
                    identityService.createMembership(u.getLogin(), groupId);
                }
            }
        }
    }

    private String mapAppRoleToGroup(String appRole) {
        if (appRole == null) return null;
        switch (appRole.toUpperCase()) {
            case "USER":
            case "CLIENT":
                return "CLIENT";
            case "OPERATOR":
            case "EMPLOYEE":
                return "OPERATOR";
            case "ADMIN":
                return "ADMIN";
            default:
                return appRole.toUpperCase();
        }
    }

    private void ensureGroupExists(String groupId) {
        if (identityService.createGroupQuery().groupId(groupId).count() == 0) {
            Group g = identityService.newGroup(groupId);
            g.setName(groupId);
            identityService.saveGroup(g);
        }
    }
}
