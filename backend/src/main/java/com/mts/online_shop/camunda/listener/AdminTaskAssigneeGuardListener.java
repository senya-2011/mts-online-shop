package com.mts.online_shop.camunda.listener;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Админские user task не должны оказаться назначенными на обычного пользователя
 * (например, из-за переменной {@code initiator} процесса отмены заказа).
 */
@Component("adminTaskAssigneeGuardListener")
public class AdminTaskAssigneeGuardListener implements TaskListener {

    private static final Set<String> ADMIN_GROUPS = Set.of("admin", "camunda-admin");

    private final IdentityService identityService;

    public AdminTaskAssigneeGuardListener(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_CREATE.equals(delegateTask.getEventName())
                && !EVENTNAME_ASSIGNMENT.equals(delegateTask.getEventName())) {
            return;
        }
        String assignee = delegateTask.getAssignee();
        if (assignee == null || assignee.isBlank()) {
            return;
        }
        if (!isAdminUser(assignee)) {
            delegateTask.setAssignee(null);
        }
    }

    private boolean isAdminUser(String userId) {
        String normalized = userId.toLowerCase(Locale.ROOT);
        return identityService.createGroupQuery()
                .groupMember(normalized)
                .list()
                .stream()
                .anyMatch(group -> ADMIN_GROUPS.contains(group.getId()));
    }
}
