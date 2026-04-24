# MTS Online Shop — где выполнены требования ЛР №2 и ЛР №3

Краткая карта **ЛР №2**: **транзакции (Spring JTA + Narayana)**, **декларативные транзакции**, **Spring Security + JAAS**, **учётные записи в XML**, **JWT**, **роли и привилегии**, **OpenAPI**, **деплой на helios**.

Краткая карта **ЛР №3**: **очередь RabbitMQ**, **отправка MQTT**, **приём JMS**, **два узла notification-service**, **`@Scheduled`**, **интеграция с банком по JCA (CCI)** — детально в [разделе «Лабораторная работа №3»](#lab3).

---

## 1. Управление транзакциями: Spring JTA + Narayana

| Что сделано | Где в коде |
|-------------|------------|
| Менеджер транзакций JTA на Narayana | [`backend/src/main/java/com/mts/online_shop/config/NarayanaJtaConfig.java`](backend/src/main/java/com/mts/online_shop/config/NarayanaJtaConfig.java) — бин `transactionManager`: `JtaTransactionManager` + `com.arjuna.ats.jta.TransactionManager.transactionManager()` |
| Зависимости Narayana | [`backend/build.gradle.kts`](backend/build.gradle.kts) — `org.jboss.narayana.jta:narayana-jta`, `narayana-jta:jta` |
| Декларативные транзакции `@Transactional` | Например [`backend/src/main/java/com/mts/online_shop/service/OrderService.java`](backend/src/main/java/com/mts/online_shop/service/OrderService.java) (`createOrderWithPayment`, `cancelOrder`, `adminCancelOrder` и др.), [`AuthService`](backend/src/main/java/com/mts/online_shop/service/AuthService.java) |
| Логирование вокруг транзакций (аспект) | [`backend/src/main/java/com/mts/online_shop/aspect/NarayanaTransactionAspect.java`](backend/src/main/java/com/mts/online_shop/aspect/NarayanaTransactionAspect.java) — `@Around("@annotation(org.springframework.transaction.annotation.Transactional)")` |

Пример настройки JTA:

```15:20:backend/src/main/java/com/mts/online_shop/config/NarayanaJtaConfig.java
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        JtaTransactionManager transactionManager = new JtaTransactionManager();
        transactionManager.setTransactionManager(com.arjuna.ats.jta.TransactionManager.transactionManager());
        transactionManager.setUserTransaction(com.arjuna.ats.jta.UserTransaction.userTransaction());
        return transactionManager;
```

---

## 2. Spring Security + JAAS, пользователи в XML

| Что сделано | Где в коде |
|-------------|------------|
| Конфигурация JAAS (`LoginContext` имя приложения) | [`backend/src/main/resources/jaas.conf`](backend/src/main/resources/jaas.conf) — приложение `MTSOnlineShop`, модуль `XmlUserLoginModule` |
| JAAS `LoginModule`: чтение пользователей из XML, проверка пароля | [`backend/src/main/java/com/mts/online_shop/security/jaas/XmlUserLoginModule.java`](backend/src/main/java/com/mts/online_shop/security/jaas/XmlUserLoginModule.java) |
| Файл учётных записей (логин, хеш пароля BCrypt, роли) | [`backend/src/main/resources/users.xml`](backend/src/main/resources/users.xml) |
| Подключение JAAS к Spring Security | [`backend/src/main/java/com/mts/online_shop/config/SecurityConfig.java`](backend/src/main/java/com/mts/online_shop/config/SecurityConfig.java) — `JaasAuthenticationProvider`, `setLoginContextName("MTSOnlineShop")`, `setLoginConfig(ClassPathResource("jaas.conf"))`, `AuthorityGranter` из `XmlUserPrincipal` |
| Цепочка HTTP-безопасности: публичные пути, роли для `/api/cart`, `/api/orders`, админка | Тот же [`SecurityConfig.java`](backend/src/main/java/com/mts/online_shop/config/SecurityConfig.java) — метод `filterChain` (`requestMatchers`, `hasRole` / `hasAnyRole`) |

Фрагмент `jaas.conf`:

```1:5:backend/src/main/resources/jaas.conf
MTSOnlineShop {
    com.mts.online_shop.security.jaas.XmlUserLoginModule required
        usersFile="classpath:users.xml"
        debug=true;
};
```

Фрагмент регистрации JAAS-провайдера:

```50:76:backend/src/main/java/com/mts/online_shop/config/SecurityConfig.java
        JaasAuthenticationProvider jaasProvider = new JaasAuthenticationProvider();
        jaasProvider.setLoginContextName("MTSOnlineShop");
        Resource jaasConfigResource = new ClassPathResource("jaas.conf");
        jaasProvider.setLoginConfig(jaasConfigResource);
        jaasProvider.setAuthorityGranters(new org.springframework.security.authentication.jaas.AuthorityGranter[] {
            principal -> {
                if (principal instanceof com.mts.online_shop.security.jaas.XmlUserPrincipal) {
                    com.mts.online_shop.security.jaas.XmlUserPrincipal xmlPrincipal =
                        (com.mts.online_shop.security.jaas.XmlUserPrincipal) principal;
                    return xmlPrincipal.getUser().getRoles();
                }
                return java.util.Collections.emptySet();
            }
        });
        // ...
        return new ProviderManager(jaasProvider);
```

---

## 3. JWT для доступа к REST

| Что сделано | Где в коде |
|-------------|------------|
| Выдача и разбор JWT | [`backend/src/main/java/com/mts/online_shop/security/JwtService.java`](backend/src/main/java/com/mts/online_shop/security/JwtService.java) |
| Фильтр: заголовок `Authorization: Bearer …` → установка `SecurityContext` | [`backend/src/main/java/com/mts/online_shop/security/JwtAuthenticationFilter.java`](backend/src/main/java/com/mts/online_shop/security/JwtAuthenticationFilter.java), регистрация в [`SecurityConfig`](backend/src/main/java/com/mts/online_shop/config/SecurityConfig.java) (`addFilterBefore(jwtAuthenticationFilter, …)`) |
| Настройки секрета / времени жизни | [`backend/src/main/resources/application.yaml`](backend/src/main/resources/application.yaml) — блок `jwt:` |

---

## 4. Роли и привилегии (спецификация + часть проверок)

| Что сделано | Где в коде |
|-------------|------------|
| XML-модель: роли → набор привилегий, список привилегий, привязка операций к привилегиям | [`backend/src/main/resources/security-model.xml`](backend/src/main/resources/security-model.xml) |
| Загрузка модели в память, проверка `hasPrivilege(Set<String> roles, String privilege)` | [`backend/src/main/java/com/mts/online_shop/security/PrivilegeService.java`](backend/src/main/java/com/mts/online_shop/security/PrivilegeService.java) |
| Аннотации `@RequirePrivilege`, `@RequireAnyPrivilege`, `@RequireAllPrivileges` + аспект | [`backend/src/main/java/com/mts/online_shop/security/annotation/`](backend/src/main/java/com/mts/online_shop/security/annotation/), [`PrivilegeCheckAspect.java`](backend/src/main/java/com/mts/online_shop/security/aspect/PrivilegeCheckAspect.java) |
| Разграничение по **ролям** на уровне контроллеров | `@PreAuthorize("hasRole('USER')")` — [`UserCartController.java`](backend/src/main/java/com/mts/online_shop/controller/UserCartController.java); `@PreAuthorize("hasRole('ADMIN')")` — [`AdminProductsController`](backend/src/main/java/com/mts/online_shop/controller/AdminProductsController.java), [`AdminUsersController`](backend/src/main/java/com/mts/online_shop/controller/AdminUsersController.java), [`AdminCartController`](backend/src/main/java/com/mts/online_shop/controller/AdminCartController.java) |

> **Замечание:** в `PrivilegeService.hasPrivilege(String username, String privilege)` сейчас заглушка `return true` — детальная проверка по привилегиям для имени пользователя не доведена до конца; основная защита REST идёт через **роли** в `SecurityConfig` и **`@PreAuthorize`**.

---

## 5. REST API и артефакты процесса

| Что сделано | Где |
|-------------|-----|
| Спецификация OpenAPI | [`api/openapi.yml`](api/openapi.yml) (версия указана в файле, сейчас 3.0.x) |
| Реализация эндпоинтов | [`backend/src/main/java/com/mts/online_shop/controller/`](backend/src/main/java/com/mts/online_shop/controller/) |
| Внешний банк (симулятор) | Модуль [`bank/`](bank/) |

---

## 6. Развёртывание на helios

| Что сделано | Где |
|-------------|-----|
| Сборка JAR и выкладка по SSH | [`.github/workflows/helios-deploy.yml`](.github/workflows/helios-deploy.yml) — `bootJar`, `scp` в `~/MTS/`, переменные `MTS_PORT`, `BANK_PORT`, `BANK_URL`, запуск `online-shop.jar` и `bank-simulator.jar` |

---

## Быстрая навигация по пакетам (backend)

- `com.mts.online_shop.config` — Security, JTA (Narayana), Swagger и др.
- `com.mts.online_shop.security` — JWT, фильтры, JAAS callback/principal, `XmlUserDetailsService`
- `com.mts.online_shop.security.jaas` — `XmlUserLoginModule` (JAAS)
- `com.mts.online_shop.service` — бизнес-логика и `@Transactional`
- `com.mts.online_shop.aspect` — обход `@Transactional` для логов Narayana

---

## Сборка и запуск (локально)

- **Backend:** из каталога `backend`: `./gradlew bootRun` (нужны JDK 21, PostgreSQL по `application.yaml`).
- **Bank:** из каталога `bank`: `./gradlew bootRun`.
- Порты и URL банка задаются переменными окружения / `application.yaml` (см. также шаг деплоя в `helios-deploy.yml`).

---

## Лабораторная работа №3: асинхронность, два узла, планировщик, JCA

<a id="lab3"></a>

Ниже — соответствие **вашему тексту задания** (очередь, MQTT, JMS, два узла, распределённые транзакции, `@Scheduled`, JCA/EIS). У разных вариантов формулировки могут отличаться; при сдаче сверьтесь с выданным вам вариантом.

### Согласованные прецеденты для асинхронной обработки

| Прецедент | Смысл | Где запускается асинхронная цепочка |
|-----------|--------|-------------------------------------|
| Уведомления в Telegram | После привязки Telegram к аккаунту и после успешной оплаты заказа backend **не ждёт** доставки в мессенджер: публикует событие в брокер; доставка и вызов Bot API — на отдельном сервисе | Публикация: [`backend/src/main/java/com/mts/online_shop/service/TelegramLinkService.java`](backend/src/main/java/com/mts/online_shop/service/TelegramLinkService.java), [`backend/src/main/java/com/mts/online_shop/service/OrderService.java`](backend/src/main/java/com/mts/online_shop/service/OrderService.java) → [`backend/src/main/java/com/mts/online_shop/messaging/MqttNotificationPublisher.java`](backend/src/main/java/com/mts/online_shop/messaging/MqttNotificationPublisher.java) |

Контракт сообщения (JSON): [`messaging-contracts/src/main/java/com/mts/messaging/contracts/TelegramNotificationEnvelope.java`](messaging-contracts/src/main/java/com/mts/messaging/contracts/TelegramNotificationEnvelope.java).

---

### Асинхронная обработка и модель «очередь сообщений»

| Требование | Реализация | Путь к коду / конфигу |
|------------|------------|------------------------|
| Очередь на базе **RabbitMQ** | Очередь `telegram.notifications`, привязка к `amq.topic` | [`docker/docker-compose.yml`](docker/docker-compose.yml) — сервис `rabbitmq-setup` (HTTP API создания очереди и binding с routing key `mts.shop.telegram.#`) |
| Отправка по **MQTT** | Eclipse Paho, публикация JSON в топик | [`backend/src/main/java/com/mts/online_shop/messaging/MqttNotificationPublisher.java`](backend/src/main/java/com/mts/online_shop/messaging/MqttNotificationPublisher.java) |
| Настройки MQTT (URI, топик, префикс clientId) | `@ConfigurationProperties` | [`backend/src/main/java/com/mts/online_shop/config/MqttProperties.java`](backend/src/main/java/com/mts/online_shop/config/MqttProperties.java), [`backend/src/main/resources/application.yaml`](backend/src/main/resources/application.yaml) (`app.mqtt`) |
| Регистрация свойств в Spring | `@EnableConfigurationProperties` | [`backend/src/main/java/com/mts/online_shop/OnlineShopApplication.java`](backend/src/main/java/com/mts/online_shop/OnlineShopApplication.java) |
| Получение по **JMS API** | `@JmsListener`, фабрика на RabbitMQ JMS Client | [`notification-service/src/main/kotlin/com/wish_notification/notification_service/messaging/TelegramNotificationsJmsListener.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/messaging/TelegramNotificationsJmsListener.kt) |
| Конфигурация JMS (RMQConnectionFactory, listener container) | Spring JMS | [`notification-service/src/main/kotlin/com/wish_notification/notification_service/config/JmsConfig.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/config/JmsConfig.kt) (`@EnableJms`, `DefaultJmsListenerContainerFactory`) |
| Имя очереди (переменная окружения) | `app.jms.telegram-queue` | [`notification-service/src/main/resources/application.yml`](notification-service/src/main/resources/application.yml) и переменные в [`docker/docker-compose.yml`](docker/docker-compose.yml) |

---

### Распределённая обработка на двух узлах

| Требование | Реализация | Путь к коду / конфигу |
|------------|------------|------------------------|
| Два **независимых** узла обработки сообщений | Два Spring Boot-приложения `notification-service` в Docker; оба подписаны на одну и ту же JMS-очередь (конкурирующие потребители) | [`docker/docker-compose.yml`](docker/docker-compose.yml) — `notification-service-1` (всегда) и `notification-service-2` (**только** с профилем `notify-scale`, см. комментарий в compose: два long polling с одним токеном бота конфликтуют) |
| Обработка на узле | Разбор JSON, верификация, plain text, регистрация чата | [`TelegramNotificationsJmsListener.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/messaging/TelegramNotificationsJmsListener.kt), [`notification-service/.../telegram/TelegramNotificationService.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/telegram/TelegramNotificationService.kt), [`notification-service/.../telegram/TelegramBot.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/telegram/TelegramBot.kt), [`TelegramChatRegistry.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/telegram/TelegramChatRegistry.kt) |
| Регистрация long polling после старта | Повторные попытки при недоступности `api.telegram.org` | [`notification-service/.../config/TelegramBotRegistrationRunner.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/config/TelegramBotRegistrationRunner.kt), [`BotConfig.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/config/BotConfig.kt) |

---

### Распределённые транзакции (требование задания)

| Что сделано | Где |
|-------------|-----|
| JTA / глобальный менеджер транзакций на **backend** (Narayana) | [`backend/src/main/java/com/mts/online_shop/config/NarayanaJtaConfig.java`](backend/src/main/java/com/mts/online_shop/config/NarayanaJtaConfig.java) |
| Пример сценария «БД заказа + вызов банка» в одной границе `@Transactional` | [`backend/src/main/java/com/mts/online_shop/service/DistributedTransactionService.java`](backend/src/main/java/com/mts/online_shop/service/DistributedTransactionService.java) |

**Важно для отчёта:** JCA-адаптер банка в [`bank-jca-adapter/.../BankManagedConnection.java`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/BankManagedConnection.java) возвращает `null` из `getXAResource()` и не поддерживает `LocalTransaction` — то есть **зачисление ресурса банка в двухфазную XA-транзакцию вместе с БД в этом репозитории не реализовано**; `DistributedTransactionService` демонстрирует **декларативную транзакцию на узле backend** вокруг JPA и вызова `BankClient`. Основной пользовательский поток оплаты — в [`OrderService.payOrder`](backend/src/main/java/com/mts/online_shop/service/OrderService.java). Уточните у преподавателя, достаточно ли такой трактовки «распределённости» для вашего варианта.

Транзакции на **notification-service** (локальные, JPA): например [`TelegramChatRegistry.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/telegram/TelegramChatRegistry.kt).

---

### Планировщик задач Spring (`@Scheduled`)

| Прецедент | Назначение | Класс и путь |
|-----------|------------|----------------|
| Мониторинг доступности банка (EIS по REST на стороне симулятора) | Периодический HTTP-ping | [`backend/src/main/java/com/mts/online_shop/service/BankReachabilityScheduler.java`](backend/src/main/java/com/mts/online_shop/service/BankReachabilityScheduler.java) — `@Scheduled(fixedRateString = "${app.bank.health-check-ms:300000}")` |
| Очистка просроченных кодов привязки Telegram в памяти | Раз в час удаление записей старше TTL | [`notification-service/.../telegram/TelegramVerificationStore.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/telegram/TelegramVerificationStore.kt) — `@Scheduled(fixedRate = 3600000)` |
| Включение планировщика | `@EnableScheduling` | [`backend/src/main/java/com/mts/online_shop/OnlineShopApplication.java`](backend/src/main/java/com/mts/online_shop/OnlineShopApplication.java), [`notification-service/.../NotificationServiceApplication.kt`](notification-service/src/main/kotlin/com/wish_notification/notification_service/NotificationServiceApplication.kt) |

Интервал для банка задаётся в [`backend/src/main/resources/application.yaml`](backend/src/main/resources/application.yaml) (`app.bank.health-check-ms`).

---

### Интеграция с внешней корпоративной системой (EIS) через **JCA**

| Роль | Описание | Путь |
|------|----------|------|
| **EIS** | Корпоративный **банковский** сервис (симулятор): REST API приёма платежей | Модуль [`bank/`](bank/) |
| **JCA (Jakarta Connectors)** | Отдельный артефакт адаптера: `ManagedConnectionFactory`, `ManagedConnection`, CCI `Connection` / `Interaction`, `MappedRecord` | Каталог [`bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/) — в частности [`BankManagedConnectionFactory.java`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/BankManagedConnectionFactory.java), [`BankManagedConnection.java`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/BankManagedConnection.java), [`BankCciConnection.java`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/BankCciConnection.java), [`BankCciInteraction.java`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/BankCciInteraction.java), [`LocalConnectionManager.java`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/LocalConnectionManager.java) |
| Подключение JCA к Spring (бин `ConnectionFactory`) | Конфигурация | [`backend/src/main/java/com/mts/online_shop/config/BankJcaConfig.java`](backend/src/main/java/com/mts/online_shop/config/BankJcaConfig.java) |
| Использование из бизнес-логики | Реализация интерфейса `BankClient` через CCI | [`backend/src/main/java/com/mts/online_shop/client/bank/BankClientJcaAdapter.java`](backend/src/main/java/com/mts/online_shop/client/bank/BankClientJcaAdapter.java) |
| Контракт операции (CCI) | `InteractionSpec` / поля записей | [`bank-jca-adapter/.../BankInteractionSpec.java`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/BankInteractionSpec.java), [`SimpleMappedRecord.java`](bank-jca-adapter/src/main/java/com/mts/online_shop/bank/jca/SimpleMappedRecord.java) |

Вызовы банка из домена: [`OrderService`](backend/src/main/java/com/mts/online_shop/service/OrderService.java), [`TransactionalOrderService`](backend/src/main/java/com/mts/online_shop/service/TransactionalOrderService.java) и др. через инжект `BankClient`.

---

### Модель процесса, REST API, тесты, деплой (по правилам работы)

| Требование | Статус / где |
|------------|----------------|
| Модель бизнес-процесса | При необходимости обновите диаграммы в [`report/`](report/) под ваш курс; асинхронный контур «оплата / привязка → MQTT → очередь → notification → Telegram» описан выше и в разделе «Архитектура» ниже по файлу. |
| REST API | [`api/openapi.yml`](api/openapi.yml); контроллеры в [`backend/src/main/java/com/mts/online_shop/controller/`](backend/src/main/java/com/mts/online_shop/controller/) (в т.ч. админка Telegram: [`AdminTelegramController.java`](backend/src/main/java/com/mts/online_shop/controller/AdminTelegramController.java)). |
| Скрипты тестирования публичных интерфейсов | Отдельного набора `.http`/shell-скриптов в репозитории **нет**; проверка через OpenAPI/Swagger UI, Postman/curl или тесты модулей (например Kotlin-тесты в `backend/src/test`, `notification-service/src/test`). При требовании методички — добавьте согласованные с преподавателем скрипты. |
| Деплой на helios или своя инфраструктура | GitHub Actions: [`.github/workflows/helios-deploy.yml`](.github/workflows/helios-deploy.yml); локально/Docker: [`docker/docker-compose.yml`](docker/docker-compose.yml). |

---

## Архитектура (Docker / сообщения / Telegram)

```mermaid
flowchart LR
  subgraph clients["Клиенты"]
    WEB["Web / REST"]
    TG["Telegram Bot API"]
  end

  WEB --> BACK["backend\n(Spring)"]
  BACK --> PG[("PostgreSQL\nbackend + bank DB")]
  BACK --> BANK["bank\n(REST)"]
  BACK -->|"MQTT publish\nmts/shop/telegram/…"| RMQ["RabbitMQ\nMQTT plugin + AMQP"]

  RMQ -->|"JMS queue\ntelegram.notifications"| NS["notification-service\n(JMS listener + бот)"]
  NS --> TG
  NS --> PG
```

**Зачем два «коннекта» к уведомлениям.** Backend шлёт события в **один** брокер RabbitMQ по **MQTT** (топик с точками в routing key); в Rabbit настроена очередь `telegram.notifications` и привязка к `amq.topic`, а **notification-service** читает ту же очередь по **AMQP/JMS**. То есть это не два разных брокера, а один канал брокера с двумя протоколами на входе (MQTT из backend) и на выходе (JMS в сервисе уведомлений).

**Два контейнера `notification-service` в compose.** По умолчанию поднимается один экземпляр. Второй включён только с профилем `notify-scale` (`docker compose --profile notify-scale up`), потому что у **long polling** один и тот же `TELEGRAM_BOT_TOKEN` нельзя безопасно использовать в двух процессах одновременно — обновления `/start` и сообщений начнут «теряться» или конфликтовать.
