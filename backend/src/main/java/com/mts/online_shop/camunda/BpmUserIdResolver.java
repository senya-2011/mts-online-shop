package com.mts.online_shop.camunda;

import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.security.XmlUserDetailsService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Единая точка: {@code userId} в BPMN — это ID из таблицы {@code users}, не из users.xml.
 */
@Service
public class BpmUserIdResolver {

    private static final Logger log = LoggerFactory.getLogger(BpmUserIdResolver.class);

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final IdentityService identityService;
    private final XmlUserDetailsService xmlUserDetailsService;
    private final UserRepository userRepository;
    private final BpmDelegateSupport bpmDelegateSupport;

    public BpmUserIdResolver(
            IdentityService identityService,
            XmlUserDetailsService xmlUserDetailsService,
            UserRepository userRepository,
            BpmDelegateSupport bpmDelegateSupport) {
        this.identityService = identityService;
        this.xmlUserDetailsService = xmlUserDetailsService;
        this.userRepository = userRepository;
        this.bpmDelegateSupport = bpmDelegateSupport;
    }

    /** Выставляет {@code userId} и {@code initiatorLogin} в execution. */
    public void reconcileExecutionUser(DelegateExecution execution) {
        Long userId = resolveUserId(execution);
        if (userId == null) {
            throw new BpmnError(
                    VALIDATION_ERROR,
                    "Не удалось определить пользователя магазина. Войдите в Tasklist под user/admin "
                            + "или запустите процесс через API с JWT.");
        }
        String login = resolveLogin(execution);
        if (login != null) {
            execution.setVariable("initiatorLogin", login.toLowerCase(Locale.ROOT));
        }
        Object existing = execution.getVariable("userId");
        if (existing != null && !userId.equals(toLong(existing))) {
            log.warn(
                    "Reconciled BPMN userId {} -> {} for process {}",
                    existing,
                    userId,
                    execution.getProcessInstanceId());
        }
        execution.setVariable("userId", userId);
    }

    public Long requiredUserId(DelegateExecution execution) {
        Long userId = resolveUserId(execution);
        if (userId == null) {
            throw new BpmnError(
                    VALIDATION_ERROR,
                    "Пользователь не найден в базе магазина. Перезайдите в API/Tasklist и перезапустите процесс.");
        }
        execution.setVariable("userId", userId);
        return userId;
    }

    public Long resolveUserId(DelegateExecution execution) {
        String login = resolveLogin(execution);
        if (login != null && !login.isBlank()) {
            String normalized = login.trim().toLowerCase(Locale.ROOT);
            return bpmDelegateSupport.callInNewTransaction(
                    () -> xmlUserDetailsService.ensureDatabaseUserIdByLogin(normalized).orElse(null));
        }
        Long fromVariable = toLong(execution.getVariable("userId"));
        if (fromVariable != null && userRepository.existsById(fromVariable)) {
            return fromVariable;
        }
        return null;
    }

    private String resolveLogin(DelegateExecution execution) {
        var authentication = identityService.getCurrentAuthentication();
        if (authentication != null
                && authentication.getUserId() != null
                && !authentication.getUserId().isBlank()) {
            return authentication.getUserId();
        }
        Object initiatorLogin = execution.getVariable("initiatorLogin");
        if (initiatorLogin != null && !initiatorLogin.toString().isBlank()) {
            return initiatorLogin.toString();
        }
        Object initiator = execution.getVariable("initiator");
        if (initiator != null && !initiator.toString().isBlank()) {
            return initiator.toString();
        }
        Long userId = toLong(execution.getVariable("userId"));
        if (userId != null) {
            return userRepository.findById(userId)
                    .map(com.mts.online_shop.model.User::getLogin)
                    .orElse(null);
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        if (value instanceof Double doubleValue) {
            return doubleValue.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
