#import "title.typ": title_page

#set page(margin: 2.5cm)
#set figure(numbering: "1", supplement: [Рисунок])

// Титульная страница
#title_page(
  worktype: [По лабораторной работе 2],
  theme: [Вариант 111],
  teacher: [Кривоносов Егор Дмитриевич],
  author: [Алхимовици Арсений,	Юсупова Алиса],
  group: [P3310, 3313],
  date: "2026",
  discipline: [Бизнес-логика программных систем],
)

#pagebreak()


#v(1cm)

= Текст задания

Доработать приложение из лабораторной работы #1, реализовав в нём управление транзакциями и разграничение доступа к операциям бизнес-логики в соответствии с заданной политикой доступа.

*Управление транзакциями:*

+ Переработать согласованные с преподавателем прецеденты (или по согласованию с ним разработать новые), объединив взаимозависимые операции в рамках транзакций.
+ Управление транзакциями реализовать с помощью Spring JTA.
+ В реализованных (или модифицированных) прецедентах использовать декларативное управление транзакциями.
+ В качестве менеджера транзакций использовать Narayana.

*Разграничение доступа:*

+ Разработать, специфицировать и согласовать с преподавателем набор привилегий для разграничения доступа к операциям.
+ Специфицировать и согласовать с преподавателем набор ролей для доступа к операциям бизнес-логики.
+ Реализовать модель разграничений на базе Spring Security + JAAS. Учётные записи хранить в XML; для доступа к API использовать JWT.

*Правила выполнения работы:*

+ Учесть изменения бизнес-процесса в модели процесса, в REST API и в скриптах тестирования публичных интерфейсов.
+ Развернуть доработанное приложение на сервере helios.


#v(1cm)

= Исходный код

#link("https://github.com/senya-2011/mts-online-shop")[https://github.com/senya-2011/mts-online-shop]

#v(1cm)

= Модель потока управления для автоматизируемого бизнес-процесса

#figure(
  image("lr2.svg", width: 100%),
  caption: [BPMN диаграмма]
)

#v(1cm)

= UML-диаграммы

#figure(
  image("./umls/mts-shop-class-uml.png", width: 100%),
  caption: [UML-диаграмма классов]
)

#v(0.5cm)

#figure(
  image("./umls/mts-shop-packet-uml.png", width: 100%),
  caption: [UML-диаграмма пакетов]
)

#v(1cm)

= Спецификация REST API

Спецификация REST API задана в формате OpenAPI 3.0.3 (файл #raw("api/openapi.yml")).

#v(0.5cm)

*Auth* (без JWT)

#table(
  columns: (auto, 1.2fr, 2.2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [POST], [/auth/login], [Вход. Тело: LoginRequest (login, password). Ответ: 200 LoginResponse (accessToken).],
  [POST], [/auth/register], [Регистрация. Тело: RegisterRequest (login, email, password, name). Ответ: 201 MessageResponse.],
)

#v(0.3cm)

*Products* (без JWT)

#table(
  columns: (auto, 1.2fr, 2.2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/products], [Каталог товаров. Ответ: 200 ProductListResponse: items, total, page, size.],
  [GET], [/products/\{id\}], [Товар по идентификатору. Ответ: 200 Product (id, name, price).],
)

#v(0.3cm)

*Cart* (Bearer JWT)

#table(
  columns: (auto, 1.2fr, 2.2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/cart], [Корзина текущего пользователя. Ответ: 200 CartResponse (items с itemId и product, total, page, size).],
  [POST], [/cart/items], [Добавить товар в корзину. Тело: AddCartItemRequest (productId). Ответ: 201 MessageResponse.],
  [DELETE], [/cart/items/\{itemId\}], [Удалить позицию корзины. Ответ: 200 MessageResponse.],
  [DELETE], [/cart/clear], [Очистить корзину. Ответ: 200 MessageResponse.],
)

#v(0.3cm)

*Orders* (Bearer JWT)

#table(
  columns: (auto, 1.2fr, 2.2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/orders], [Список заказов пользователя. Ответ: 200 OrderListResponse (items — OrderResponse, total, page, size).],
  [POST], [/orders/create], [Создание заказа из корзины с оплатой в одной транзакции. Тело: PaymentRequest (cardNumber, expiresAt, cvv). Ответ: 201 OrderResponse; 400/500.],
  [POST], [/orders/\{orderId\}/cancel], [Отмена заказа с возвратом средств. Ответ: 200 MessageResponse.],
)

#v(0.3cm)

*Admin Users* (Bearer JWT; в спецификации ответ 403 при отсутствии прав администратора)

#table(
  columns: (auto, 1.2fr, 2.2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/admin/users], [Список пользователей. Query: page, size. Ответ: 200 UserListResponse; 401/403.],
  [GET], [/admin/users/\{userId\}], [Пользователь по id. Ответ: 200 UserResponse; 401/403/404.],
  [GET], [/admin/users/\{userId\}/cart], [Корзина пользователя. Ответ: 200 CartResponse; 401/403/404.],
  [DELETE], [/admin/users/\{userId\}/cart/clear], [Очистить корзину пользователя. Ответ: 200 MessageResponse; 401/403/404.],
)

#v(0.3cm)

*Admin Orders* (Bearer JWT; 403 при отсутствии прав администратора)

#table(
  columns: (auto, 1.2fr, 2.2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/admin/orders], [Все заказы. Query: page, size. Ответ: 200 OrderListResponse; 401/403.],
  [POST], [/admin/orders/\{orderId\}/cancel], [Отмена заказа с возвратом. Ответ: 200 MessageResponse; 401/403/404.],
)

#v(0.3cm)

*Admin Products* (Bearer JWT; 403 при отсутствии прав администратора)

#table(
  columns: (auto, 1.2fr, 2.2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/admin/products], [Список товаров (в т.ч. скрытые). Query: page, size. Ответ: 200 ProductListResponse; 401/403.],
  [POST], [/admin/products], [Создать товар. Тело: CreateProductRequest (name, price). Ответ: 201 Product; 400/401/403.],
  [PUT], [/admin/products/\{productId\}], [Обновить товар. Тело: UpdateProductRequest (name, price). Ответ: 200 Product; 400/401/403/404.],
  [DELETE], [/admin/products/\{productId\}], [Удалить товар. Ответ: 200 MessageResponse; 401/403/404.],
)


#v(1cm)

= Выводы по работе

В соответствии с текстом задания к лабораторной работе #2 доработано приложение из ЛР#1: добавлены *управление транзакциями* и *разграничение доступа*.

*Транзакции (Spring JTA, Narayana):* взаимозависимые операции оформлены в прецедентах с использованием *декларативного* управления (#raw("@Transactional")) под менеджером транзакций Narayana; для ключевых сценариев (создание и оплата заказа, отмена с возвратом и др.) обеспечена согласованность данных при обращении к БД и внешнему банковскому сервису.

*Доступ (Spring Security + JAAS):* согласованы и заданы *роли* (в т.ч. USER и ADMIN) и *привилегии* для операций; учётные данные и роли загружаются из *XML*; для REST используется *JWT* (Bearer). Пользовательские сценарии (каталог, корзина, заказы) и административные (#raw("/admin/...")) разделены на уровне API и политики безопасности.

*Артефакты:* обновлены модель процесса (BPMN), спецификация *OpenAPI 3.0.3* (#raw("api/openapi.yml")) с описанием публичных и админских эндпоинтов, исходный код и развёртывание на *helios* (GitHub Actions). Реализация REST: контроллеры #raw("ProductsController"), #raw("UserCartController"), #raw("OrderController"), админские #raw("AdminUsersController"), #raw("AdminCartController"), #raw("AdminProductsController"); маппинг сущностей — MapStruct; уведомления — почтовый симулятор при необходимости. Настроены тесты (Kotest) и CI.