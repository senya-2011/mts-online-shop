package com.mts.online_shop.camunda;

import com.mts.online_shop.camunda.PaymentCardValidator;
import com.mts.online_shop.exception.BadRequestException;
import com.mts.online_shop.exception.InvalidPaymentDataException;
import com.mts.online_shop.exception.UserNotFoundException;
import com.mts.online_shop.model.OrderBpmStartResponse;
import com.mts.online_shop.model.OrderResponse;
import com.mts.online_shop.model.PaymentRequest;
import com.mts.online_shop.model.User;
import com.mts.online_shop.repository.UserRepository;
import com.mts.online_shop.service.GoodsService;
import com.mts.online_shop.service.OrderService;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class BpmUserService {

    public static final String PROCESS_USER_LK_ORDER = "user-lk-order";
    public static final String PROCESS_USER_ORDER_CANCEL = "user-order-cancel";
    public static final String TASK_ENTER_PRODUCT = "Task_EnterProductId";
    public static final String TASK_ADD_MORE = "Task_AddMoreProducts";
    public static final String TASK_ENTER_PAYMENT = "Task_EnterPayment";
    public static final String TASK_ENTER_ORDER_ID = "Task_EnterOrderId";
    public static final String TASK_ADMIN_CONFIRM_CANCEL = "Task_AdminConfirmCancel";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private static final String TASK_VALIDATE_CART = "Task_ValidateCart";

    private final OrderService orderService;
    private final GoodsService goodsService;
    private final UserRepository userRepository;
    private final BpmTaskCompleter bpmTaskCompleter;

    public BpmUserService(RuntimeService runtimeService,
                          TaskService taskService,
                          HistoryService historyService,
                          OrderService orderService,
                          GoodsService goodsService,
                          UserRepository userRepository,
                          BpmTaskCompleter bpmTaskCompleter) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.orderService = orderService;
        this.goodsService = goodsService;
        this.userRepository = userRepository;
        this.bpmTaskCompleter = bpmTaskCompleter;
    }

    public OrderBpmStartResponse startLkOrder(Long userId) {
        User user = requireUser(userId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_USER_LK_ORDER, lkOrderStartVariables(userId));
        assignActiveTasks(processInstance.getId(), user.getLogin());
        return buildStartResponse(processInstance, "Шаг 1: введите ID товара в Camunda Tasklist (или POST /api/cart/items)");
    }

    public void addToCart(Long userId, Long productId) {
        ProcessInstance processInstance = findOrStartLkOrder(userId);
        if (isTaskActive(processInstance.getId(), TASK_ADD_MORE)) {
            bpmTaskCompleter.completeUserTask(processInstance.getId(), TASK_ADD_MORE, Map.of("addMore", true));
        }
        bpmTaskCompleter.completeUserTask(processInstance.getId(), TASK_ENTER_PRODUCT, Map.of("productId", productId));
    }

    public OrderBpmStartResponse startOrderCreate(Long userId) {
        ProcessInstance processInstance = findOrStartLkOrder(userId);
        advanceLkOrderToPayment(processInstance.getId(), userId);
        User user = requireUser(userId);
        assignActiveTasks(processInstance.getId(), user.getLogin());
        return buildStartResponse(processInstance, "Шаг 3: введите данные карты в Camunda Tasklist (форма «Оплата корзины»)");
    }

    public OrderResponse createOrderWithPayment(Long userId, PaymentRequest paymentRequest) {
        ProcessInstance processInstance = findOrStartLkOrder(userId);
        advanceLkOrderToPayment(processInstance.getId(), userId);

        String validationError = PaymentCardValidator.validate(
                paymentRequest.getCardNumber(),
                paymentRequest.getCvv(),
                paymentRequest.getExpiresAt());
        if (validationError != null) {
            throw new InvalidPaymentDataException(validationError);
        }

        Map<String, Object> paymentVars = new HashMap<>();
        paymentVars.put("cardNumber", PaymentCardValidator.normalizeCardNumber(paymentRequest.getCardNumber()));
        paymentVars.put("cvv", paymentRequest.getCvv().trim());
        paymentVars.put("expiresAt", paymentRequest.getExpiresAt().trim());
        try {
            bpmTaskCompleter.completeUserTask(processInstance.getId(), TASK_ENTER_PAYMENT, paymentVars);
        } catch (ProcessEngineException e) {
            throw validationException(processInstance.getId(), e);
        }

        awaitLkOrderAfterPayment(processInstance.getId());

        Long orderId = getHistoricLong(processInstance.getId(), "orderId");
        if (orderId == null) {
            throw new IllegalStateException("Order was not created by BPM process");
        }
        return orderService.getOrder(orderId, userId);
    }

    public void cancelOrder(Long orderId, Long userId) {
        User user = requireUser(userId);
        Map<String, Object> startVars = new HashMap<>();
        startVars.put("orderId", orderId);
        startVars.put("userId", userId);
        startVars.put("initiatorLogin", user.getLogin().toLowerCase(Locale.ROOT));
        runtimeService.startProcessInstanceByKey(PROCESS_USER_ORDER_CANCEL, startVars);
    }

    public OrderBpmStartResponse startOrderCancel(Long userId) {
        User user = requireUser(userId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                PROCESS_USER_ORDER_CANCEL, orderCancelStartVariables(userId, user.getLogin()));
        assignActiveTasks(processInstance.getId(), user.getLogin());
        return buildStartResponse(processInstance,
                "Введите номер заказа для отмены. Заявка появится у администратора в Tasklist для подтверждения.");
    }

    /**
     * @deprecated Lab-поток больше не ждёт callback банка; оплата подтверждается по валидации карты.
     */
    @Deprecated
    public void correlatePaymentCallback(Long orderId, boolean success) {
        runtimeService.createMessageCorrelation("bank_payment_callback")
                .processInstanceVariableEquals("orderId", orderId)
                .setVariable("paymentSuccess", success)
                .setVariable("bankCallbackReceived", true)
                .correlate();
    }

    private void awaitLkOrderAfterPayment(String processInstanceId) {
        for (int attempt = 0; attempt < 150; attempt++) {
            if (isTaskActive(processInstanceId, TASK_ENTER_PAYMENT)) {
                throw validationException(processInstanceId, null);
            }
            if (runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .singleResult() == null) {
                return;
            }
            Boolean paymentSuccess = getHistoricBoolean(processInstanceId, "paymentSuccess");
            if (!Boolean.TRUE.equals(paymentSuccess)) {
                String error = getHistoricString(processInstanceId, "paymentError");
                if (error != null) {
                    throw new InvalidPaymentDataException(error);
                }
            }
            sleepBriefly();
        }
        throw new IllegalStateException("Timeout waiting for order payment BPM step");
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private ProcessInstance findOrStartLkOrder(Long userId) {
        ProcessInstance active = findActiveLkOrder(userId);
        if (active != null) {
            return active;
        }
        User user = requireUser(userId);
        ProcessInstance started = runtimeService.startProcessInstanceByKey(
                PROCESS_USER_LK_ORDER, lkOrderStartVariables(userId));
        assignActiveTasks(started.getId(), user.getLogin());
        return started;
    }

    private Map<String, Object> lkOrderStartVariables(Long userId) {
        User user = requireUser(userId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("userId", userId);
        vars.put("initiatorLogin", user.getLogin().toLowerCase(Locale.ROOT));
        return vars;
    }

    private Map<String, Object> orderCancelStartVariables(Long userId, String login) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("userId", userId);
        vars.put("initiatorLogin", login.toLowerCase(Locale.ROOT));
        return vars;
    }

    private ProcessInstance findActiveLkOrder(Long userId) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_USER_LK_ORDER)
                .variableValueEquals("userId", userId)
                .active()
                .singleResult();
    }

    private void advanceLkOrderToPayment(String processInstanceId, Long userId) {
        for (int attempt = 0; attempt < 10; attempt++) {
            if (isTaskActive(processInstanceId, TASK_ENTER_PAYMENT)) {
                return;
            }
            if (isTaskActive(processInstanceId, TASK_ADD_MORE)) {
                bpmTaskCompleter.completeUserTask(processInstanceId, TASK_ADD_MORE, Map.of("addMore", false));
                continue;
            }
            if (isTaskActive(processInstanceId, TASK_ENTER_PRODUCT)) {
                if (goodsService.getCartItems(userId).isEmpty()) {
                    throw new BadRequestException(
                            "Сначала добавьте товар в корзину (форма «Выбор ID товара» или POST /api/cart/items)");
                }
                jumpToCheckout(processInstanceId);
                continue;
            }
            throw new BadRequestException(
                    "Завершите добавление товаров в корзину (снимите галочку «Добавить ещё») и повторите POST /api/orders/create");
        }
        throw new IllegalStateException("Не удалось перейти к оплате заказа");
    }

    private void jumpToCheckout(String processInstanceId) {
        runtimeService.createProcessInstanceModification(processInstanceId)
                .cancelAllForActivity(TASK_ENTER_PRODUCT)
                .startBeforeActivity(TASK_VALIDATE_CART)
                .execute();
    }

    private boolean isTaskActive(String processInstanceId, String taskDefinitionKey) {
        return bpmTaskCompleter.findActiveUserTask(processInstanceId, taskDefinitionKey) != null;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private void assignActiveTasks(String processInstanceId, String login) {
        String normalizedLogin = login.toLowerCase(Locale.ROOT);
        taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list()
                .stream()
                .filter(this::isUserFacingTask)
                .forEach(task -> taskService.setAssignee(task.getId(), normalizedLogin));
    }

    private boolean isUserFacingTask(Task task) {
        String key = task.getTaskDefinitionKey();
        return TASK_ENTER_PRODUCT.equals(key)
                || TASK_ADD_MORE.equals(key)
                || TASK_ENTER_PAYMENT.equals(key)
                || TASK_ENTER_ORDER_ID.equals(key);
    }

    private OrderBpmStartResponse buildStartResponse(ProcessInstance processInstance, String message) {
        Task activeTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .active()
                .singleResult();
        OrderBpmStartResponse response = new OrderBpmStartResponse();
        response.setProcessInstanceId(processInstance.getId());
        if (activeTask != null) {
            response.setTaskId(activeTask.getId());
            response.setTaskName(activeTask.getName());
        }
        response.setMessage(message);
        return response;
    }

    private Boolean getHistoricBoolean(String processInstanceId, String variableName) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        return variable != null ? (Boolean) variable.getValue() : null;
    }

    private String getHistoricString(String processInstanceId, String variableName) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        return variable != null ? (String) variable.getValue() : null;
    }

    private BadRequestException validationException(String processInstanceId, ProcessEngineException cause) {
        String message = getValidationError(processInstanceId);
        if (message == null || message.isBlank()) {
            message = cause != null && cause.getMessage() != null ? cause.getMessage() : "Ошибка валидации формы";
        }
        return new BadRequestException(message);
    }

    private String getValidationError(String processInstanceId) {
        Object runtimeValue = runtimeService.getVariable(processInstanceId, "validationError");
        if (runtimeValue != null) {
            return runtimeValue.toString();
        }
        return getHistoricString(processInstanceId, "validationError");
    }

    private Long getHistoricLong(String processInstanceId, String variableName) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(variableName)
                .singleResult();
        if (variable == null) {
            return null;
        }
        Object value = variable.getValue();
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        return null;
    }
}
