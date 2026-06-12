package com.mts.online_shop.camunda.listener;

import com.mts.online_shop.repository.UserRepository;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Назначает user task на инициатора процесса, чтобы задача оставалась за ним в Tasklist.
 * Админские задачи ({@code candidateGroups=admin}) пропускаются.
 */
@Component("assignTaskToStarterListener")
public class AssignTaskToStarterListener implements TaskListener {

    private static final String TASK_ADMIN_CONFIRM_CANCEL = "Task_AdminConfirmCancel";
    private static final Set<String> ADMIN_STARTER_TASKS = Set.of(
            "Task_CreateProductForm",
            "Task_UpdateProductForm"
    );
    private static final Set<String> ADMIN_GROUPS = Set.of("admin", "camunda-admin");

    private final IdentityService identityService;
    private final UserRepository userRepository;

    public AssignTaskToStarterListener(IdentityService identityService, UserRepository userRepository) {
        this.identityService = identityService;
        this.userRepository = userRepository;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        if (!EVENTNAME_CREATE.equals(delegateTask.getEventName())
                && !EVENTNAME_ASSIGNMENT.equals(delegateTask.getEventName())) {
            return;
        }
        if (TASK_ADMIN_CONFIRM_CANCEL.equals(delegateTask.getTaskDefinitionKey())) {
            return;
        }
        if (delegateTask.getAssignee() != null && !delegateTask.getAssignee().isBlank()) {
            return;
        }
        String assignee = resolveAssignee(delegateTask);
        if (assignee != null && !assignee.isBlank()) {
            delegateTask.setAssignee(assignee.toLowerCase(Locale.ROOT));
        }
    }

    private String resolveAssignee(DelegateTask task) {
        if (ADMIN_STARTER_TASKS.contains(task.getTaskDefinitionKey())) {
            var authentication = identityService.getCurrentAuthentication();
            if (authentication != null
                    && authentication.getUserId() != null
                    && isAdminUser(authentication.getUserId())) {
                return authentication.getUserId();
            }
            return null;
        }
        return resolveStarter(task);
    }

    private boolean isAdminUser(String userId) {
        String normalized = userId.toLowerCase(Locale.ROOT);
        return identityService.createGroupQuery()
                .groupMember(normalized)
                .list()
                .stream()
                .anyMatch(group -> ADMIN_GROUPS.contains(group.getId()));
    }

    private String resolveStarter(DelegateTask task) {
        Object initiatorLogin = task.getVariable("initiatorLogin");
        if (initiatorLogin != null && !initiatorLogin.toString().isBlank()) {
            return initiatorLogin.toString();
        }
        Object userId = task.getVariable("userId");
        if (userId != null) {
            String login = resolveLoginByUserId(userId);
            if (login != null) {
                return login;
            }
        }
        Object initiator = task.getVariable("initiator");
        if (initiator != null && !initiator.toString().isBlank()) {
            return initiator.toString();
        }
        var authentication = identityService.getCurrentAuthentication();
        if (authentication != null && authentication.getUserId() != null) {
            return authentication.getUserId();
        }
        return null;
    }

    private String resolveLoginByUserId(Object userIdValue) {
        Long userId = null;
        if (userIdValue instanceof Long longValue) {
            userId = longValue;
        } else if (userIdValue instanceof Integer intValue) {
            userId = intValue.longValue();
        } else {
            try {
                userId = Long.parseLong(userIdValue.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return userRepository.findById(userId)
                .map(com.mts.online_shop.model.User::getLogin)
                .orElse(null);
    }
}
