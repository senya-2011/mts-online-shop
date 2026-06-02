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
        for (User u : allUsers) {
            String role = u.getRole();
            if (role == null) continue;
            // ensure group exists
            if (identityService.createGroupQuery().groupId(role).count() == 0) {
                Group g = identityService.newGroup(role);
                g.setName(role);
                identityService.saveGroup(g);
            }
            // add user to group if not present
            long membershipCount = identityService.createGroupQuery()
                    .groupMember(u.getLogin())
                    .groupId(role)
                    .count();
            if (membershipCount == 0) {
                if (identityService.createUserQuery().userId(u.getLogin()).count() > 0) {
                    identityService.createMembership(u.getLogin(), role);
                }
            }
        }
    }
}
