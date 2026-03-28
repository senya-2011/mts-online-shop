#import "title.typ": title_page

#set page(margin: 2.5cm)
#set figure(numbering: "1", supplement: [Рисунок])

// Титульная страница
#title_page(
  worktype: [По лабораторной работе 1],
  theme: [Вариант 2600],
  teacher: [Кривоносов Егор Дмитриевич],
  author: [Алхимовици Арсений],
  group: [P3310],
  date: "2026",
  discipline: [Бизнес-логика программных систем],
)

#pagebreak()


#v(1cm)

= Текст задания

МТС — связь и экосистема цифровых сервисов — https://mts.ru. Бизнес-процесс: интернет-магазин — выбор товара, корзина покупателя, оплата и доставка.
Описать бизнес-процесс в соответствии с нотацией BPMN 2.0, после чего реализовать его в виде приложения на базе Spring Boot.

Порядок выполнения работы:

Выбрать один из бизнес-процессов, реализуемых сайтом из варианта задания.
Утвердить выбранный бизнес-процесс у преподавателя.
Специфицировать модель реализуемого бизнес-процесса в соответствии с требованиями BPMN 2.0.
Разработать приложение на базе Spring Boot, реализующее описанный на предыдущем шаге бизнес-процесс. Приложение должно использовать СУБД PostgreSQL для хранения данных, для всех публичных интерфейсов должны быть разработаны REST API.
Разработать набор curl-скриптов, либо набор запросов для REST клиента Insomnia для тестирования публичных интерфейсов разработанного программного модуля. Запросы Insomnia оформить в виде файла экспорта.
Развернуть разработанное приложение на сервере helios.


#v(1cm)

= Исходный код

#link("https://github.com/senya-2011/mts-online-shop")[https://github.com/senya-2011/mts-online-shop]

#v(1cm)

= Модель потока управления для автоматизируемого бизнес-процесса

#figure(
  image("mts-shop-bpmn.svg", width: 100%),
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

Спецификация REST API задана в формате OpenAPI 3.1 (файл #raw("api/openapi.yml")). Контроллеры реализуют сгенерированные интерфейсы. Авторизация: Bearer JWT (кроме Auth и Products).

#v(0.5cm)

*Auth* (без авторизации)

#table(
  columns: (auto, 1.2fr, 2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [POST], [/auth/login], [Вход. Тело: LoginRequest (login, password). Ответ: 200 LoginResponse (accessToken); 401 неверный логин/пароль; 500.],
  [POST], [/auth/register], [Регистрация. Тело: RegisterRequest (login, email, password, name). Ответ: 201 MessageResponse; 400/409/500.],
)

#v(0.3cm)

*Products* (без авторизации)

#table(
  columns: (auto, 1.2fr, 2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/products], [Список товаров. Ответ: 200 ProductListResponse (items); 500.],
  [GET], [/products/\{productId\}], [Товар по id. Ответ: 200 Product; 404/500.],
)

#v(0.3cm)

*Cart* (требуется Bearer JWT)

#table(
  columns: (auto, 1.2fr, 2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/cart], [Содержимое корзины текущего пользователя. Ответ: 200 CartResponse (items); 401/500.],
  [DELETE], [/cart], [Очистить корзину. Ответ: 200 MessageResponse; 401/500.],
  [POST], [/cart/items], [Добавить товар. Тело: AddCartItemRequest (productId). Ответ: 201 MessageResponse; 400/401/404/500.],
  [DELETE], [/cart/items/\{itemId\}], [Удалить позицию из корзины. Ответ: 200 MessageResponse; 401/404/500.],
)

#v(0.3cm)

*Orders* (требуется Bearer JWT)

#table(
  columns: (auto, 1.2fr, 2fr),
  align: (center, left, left),
  stroke: 0.5pt,
  [*Метод*], [*Путь*], [*Описание*],
  [GET], [/orders], [Список заказов текущего пользователя. Ответ: 200 OrderListResponse (items); 401/500.],
  [POST], [/orders], [Создать заказ из корзины. Ответ: 201 MessageResponse; 400/401/409/500.],
  [GET], [/orders/\{orderId\}], [Заказ по id. Ответ: 200 OrderResponse; 401/403/404/500.],
  [POST], [/orders/\{orderId\}/payment], [Оплатить заказ. Тело: PaymentRequest (cardNumber, expiresAt, cvv). Ответ: 200 MessageResponse; 400/401/403/404/409/422/500.],
)


#v(1cm)

= Выводы по работе

Описан бизнес-процесс интернет-магазина МТС (каталог товаров, корзина, оформление заказа, оплата) в нотации BPMN 2.0 и реализовано приложение на Spring Boot с хранением данных в PostgreSQL. Все публичные интерфейсы заданы в OpenAPI 3.1 и реализованы в виде REST API: каталог и корзина (GoodsController), заказы и оплата (OrderController).

Реализована полная цепочка: выбор товаров и корзина (UserItem), создание заказа из корзины в одной транзакции с переносом позиций в OrderItem и очисткой корзины, валидация платёжных данных (BankSimulator) и отправка письма пользователю (MailSimulator, SMTP). Использованы MapStruct для маппинга сущностей и DTO, централизованная обработка исключений и логирование. Настроены unit-тесты (Kotest) и CI (GitHub Actions).