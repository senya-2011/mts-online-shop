package com.mts.online_shop.config;

import com.mts.online_shop.model.User;
import com.mts.online_shop.repository.UserRepository;
import org.camunda.bpm.engine.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CamundaUserSync {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private UserRepository userRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void syncUsers() {
        List<User> allUsers = userRepository.findAll();
        for (User appUser : allUsers) {
            if (identityService.createUserQuery().userId(appUser.getLogin()).count() == 0) {
                org.camunda.bpm.engine.identity.User camundaUser = identityService.newUser(appUser.getLogin());
                camundaUser.setEmail(appUser.getEmail());
                camundaUser.setFirstName(appUser.getName());
                camundaUser.setLastName("");
                identityService.saveUser(camundaUser);
            }
        }
    }
}