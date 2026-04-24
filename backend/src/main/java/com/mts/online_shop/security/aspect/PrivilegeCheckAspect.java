package com.mts.online_shop.security.aspect;

import com.mts.online_shop.exception.AccessDeniedException;
import com.mts.online_shop.security.PrivilegeService;
import com.mts.online_shop.security.annotation.RequireAllPrivileges;
import com.mts.online_shop.security.annotation.RequireAnyPrivilege;
import com.mts.online_shop.security.annotation.RequirePrivilege;
import com.mts.online_shop.security.CurrentUserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class PrivilegeCheckAspect {

    private final PrivilegeService privilegeService;
    private final CurrentUserService currentUserService;

    public PrivilegeCheckAspect(PrivilegeService privilegeService, CurrentUserService currentUserService) {
        this.privilegeService = privilegeService;
        this.currentUserService = currentUserService;
    }

    @Before("@annotation(requirePrivilege)")
    public void checkPrivilege(JoinPoint joinPoint, RequirePrivilege requirePrivilege) {
        String username = getCurrentUsername();
        String privilege = requirePrivilege.value();
        
        if (!privilegeService.hasPrivilege(username, privilege)) {
            throw new AccessDeniedException("Required privilege: " + privilege);
        }
    }

    @Before("@annotation(requireAnyPrivilege)")
    public void checkAnyPrivilege(JoinPoint joinPoint, RequireAnyPrivilege requireAnyPrivilege) {
        String username = getCurrentUsername();
        String[] privileges = requireAnyPrivilege.value();
        
        if (!privilegeService.hasAnyPrivilege(username, privileges)) {
            throw new AccessDeniedException("Required one of privileges: " + String.join(", ", privileges));
        }
    }

    @Before("@annotation(requireAllPrivileges)")
    public void checkAllPrivileges(JoinPoint joinPoint, RequireAllPrivileges requireAllPrivileges) {
        String username = getCurrentUsername();
        String[] privileges = requireAllPrivileges.value();
        
        if (!privilegeService.hasAllPrivileges(username, privileges)) {
            throw new AccessDeniedException("Required all privileges: " + String.join(", ", privileges));
        }
    }

    private String getCurrentUsername() {
        return currentUserService.getCurrentUserId()
                .map(userId -> "user_" + userId)
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
    }
}
