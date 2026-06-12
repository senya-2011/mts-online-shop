# ЛР4: Camunda BPM (embedded) + WildFly

## Обзорная диаграмма

Файл [`bpmn/lr4.bpmn`](bpmn/lr4.bpmn) — единая **документационная** BPMN (не деплоится в Camunda): все 5 процессов, дорожки USER / ADMIN / SERVICE, пул BANK и message flow оплаты.

Пересобрать после правок скрипта:

```bash
node scripts/build-lr4-overview.mjs
```

Открыть в Camunda Modeler 4.12+.

## Процессы (5 шт.)

| Название | Ключ | Описание |
|----------|------|----------|
| **USER: заказ в ЛК** | `user-lk-order` | ID товара → корзина → оформление → оплата → сервер |
| **USER: отмена заказа** | `user-order-cancel` | Ввод номера заказа → отмена |
| **ADMIN: добавление товара** | `admin-product-create` | Форма name/price → создание |
| **ADMIN: изменение товара** | `admin-product-update` | Форма ID/name/price → обновление |
| **SERVER: обработка заказа** | `server-order-processing` | Списание, email, Telegram, Bitrix, COMPLETED |

## Триггеры API

| Endpoint | Процесс |
|----------|---------|
| `POST /api/cart/items` | USER: заказ в ЛК (шаг «добавить товар») |
| `POST /api/orders/create` | USER: заказ в ЛК (оплата) |
| `POST /api/orders/{id}/cancel` | USER: отмена заказа |
| `POST /api/admin/products` | ADMIN: добавление товара |
| `PUT /api/admin/products/{id}` | ADMIN: изменение товара |
| `POST /api/internal/bank/payment-callback` | callback оплаты в `user-lk-order` |

Вне BPMN: login/register, GET каталога, DELETE товаров/пользователей, админ-отмена заказа.

## Формы (`backend/src/main/resources/forms/`)

- `user-add-product-form` — ID товара
- `user-add-more-form` — добавить ещё товар
- `user-payment-form` — карта, CVV, срок
- `user-cancel-order-form` — номер заказа
- `admin-product-create-form` — название, цена
- `admin-product-update-form` — ID, название, цена

## Сборка

```bash
./gradlew :backend:bootWar -x test
```

Артефакт: `backend/build/libs/online-shop.war`

## Camunda

- Tasklist: `/camunda/app/tasklist/`
- Cockpit: `/camunda/app/cockpit/`
- При старте удаляются старые deployment'ы (см. `CamundaDeploymentCleaner`)
