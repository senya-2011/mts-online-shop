package com.mts.online_shop.controller;

import com.mts.online_shop.service.TelegramBroadcastService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo/telegram")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Demo-Telegram", description = "Демо-рассылка в RabbitMQ для нагрузочного теста")
public class DemoTelegramBroadcastController {

    private final TelegramBroadcastService telegramBroadcastService;

    public DemoTelegramBroadcastController(TelegramBroadcastService telegramBroadcastService) {
        this.telegramBroadcastService = telegramBroadcastService;
    }

    @GetMapping(value = "/broadcast-page", produces = MediaType.TEXT_HTML_VALUE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Простая страница для запуска рассылки")
    public String broadcastPage() {
        return """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                  <meta charset="UTF-8"/>
                  <title>MTS — рассылка в RabbitMQ</title>
                  <style>
                    body { font-family: sans-serif; max-width: 520px; margin: 2rem auto; padding: 0 1rem; }
                    button { padding: 0.75rem 1.25rem; font-size: 1rem; cursor: pointer; }
                    pre { background: #f4f4f4; padding: 1rem; border-radius: 8px; white-space: pre-wrap; }
                    label { display: block; margin: 0.5rem 0; }
                    input { width: 100%; padding: 0.4rem; box-sizing: border-box; }
                  </style>
                </head>
                <body>
                  <h1>Рассылка «Распродажа» → RabbitMQ → Telegram</h1>
                  <p>Отправляет сообщения всем привязанным Telegram-аккаунтам через MQTT.</p>
                  <label>Сообщений на каждого пользователя: <input id="count" type="number" value="10" min="1" max="100"/></label>
                  <label>Пауза между сообщениями (мс): <input id="delay" type="number" value="300" min="0" max="5000"/></label>
                  <p><button id="go">Запустить рассылку</button></p>
                  <pre id="out">Нажмите кнопку…</pre>
                  <script>
                    document.getElementById('go').onclick = async () => {
                      const count = document.getElementById('count').value;
                      const delay = document.getElementById('delay').value;
                      const out = document.getElementById('out');
                      out.textContent = 'Отправка…';
                      try {
                        const r = await fetch('broadcast?count=' + count + '&delayMs=' + delay, { method: 'POST' });
                        const j = await r.json();
                        out.textContent = r.ok ? JSON.stringify(j, null, 2) : 'Ошибка: ' + JSON.stringify(j);
                      } catch (e) {
                        out.textContent = 'Ошибка: ' + e;
                      }
                    };
                  </script>
                </body>
                </html>
                """;
    }

    @PostMapping("/broadcast")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Спам-рассылка в RabbitMQ",
            description = "Публикует N сообщений «Распродажа <случайное слово>» на каждого пользователя с привязанным Telegram"
    )
    public ResponseEntity<Map<String, Object>> broadcast(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "300") long delayMs
    ) {
        try {
            int published = telegramBroadcastService.broadcastSale(count, delayMs);
            return ResponseEntity.ok(Map.of(
                    "published", published,
                    "messagesPerUser", Math.max(1, count),
                    "delayMs", delayMs,
                    "messageTemplate", "Распродажа <random>"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
