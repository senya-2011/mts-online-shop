#!/usr/bin/env bash
# Поднять весь стек MTS Online Shop на сервере (Docker Compose).
# Использование:
#   ./start.sh          — сборка и запуск в фоне
#   ./start.sh logs     — логи всех сервисов
#   ./start.sh down     — остановить и удалить контейнеры
#   ./start.sh status   — статус контейнеров
#   ./start.sh restart  — пересобрать и перезапустить
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

ensure_swap() {
  if swapon --show 2>/dev/null | grep -q swapfile; then
    return 0
  fi
  if [[ ! -f /swapfile ]]; then
    echo "==> Creating 2G swap (low RAM on VPS)..."
    fallocate -l 2G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=2048 status=none
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null
  fi
  swapon /swapfile 2>/dev/null || true
}

ensure_docker() {
  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    return 0
  fi
  echo "==> Installing Docker..."
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y -qq docker.io docker-compose-v2
  systemctl enable --now docker
}

load_env() {
  if [[ -f "$ROOT/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$ROOT/.env"
    set +a
  fi
  SERVER_HOST="${SERVER_HOST:-$(curl -sf --max-time 3 ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')}"
}

print_urls() {
  local host="${SERVER_HOST:-localhost}"
  echo ""
  echo "=== MTS Online Shop is up ==="
  echo "  Backend API:    http://${host}:${MTS_PORT:-8080}${MTS_CONTEXT_PATH:-/api}"
  echo "  Swagger UI:     http://${host}:${MTS_PORT:-8080}${MTS_CONTEXT_PATH:-/api}/swagger-ui.html"
  echo "  Bank API:       http://${host}:${BANK_PORT:-8081}${BANK_CONTEXT_PATH:-/api}"
  echo "  RabbitMQ UI:    http://${host}:${RABBITMQ_UI_PORT:-15672}  (guest/guest)"
  echo "  Prometheus:     http://${host}:${PROMETHEUS_PORT:-9090}"
  echo "  Grafana:        http://${host}:${GRAFANA_PORT:-3000}  (${GRAFANA_ADMIN_USER:-admin}/${GRAFANA_ADMIN_PASSWORD:-admin})"
  echo "  Notification:   http://${host}:${NOTIFY1_PORT:-18081}"
  echo ""
  echo "  ./start.sh logs    — смотреть логи"
  echo "  ./start.sh down    — остановить"
}

cmd="${1:-up}"

case "$cmd" in
  up|start|"")
    ensure_swap
    ensure_docker
    chmod +x "$ROOT/docker-up.sh"
    # На VPS <2G RAM параллельная сборка bank+backend+notify гоняет swap; notify (Kotlin) «висит» дольше всех.
    mem_mb="$(awk '/MemTotal/ {print int($2/1024)}' /proc/meminfo)"
    if [[ "${SEQUENTIAL_BUILD:-1}" == "1" && "$mem_mb" -lt 2048 ]]; then
      echo "==> Low RAM (${mem_mb}MB): building images one by one (notify/Telegram ~4–5 min)..."
      for svc in rabbitmq bank backend notification-service-1; do
        echo "    -> build $svc"
        "$ROOT/docker-up.sh" build "$svc"
      done
      echo "==> Starting containers..."
      "$ROOT/docker-up.sh" up -d
    else
      echo "==> Building and starting containers (may take 10–20 min on first run)..."
      "$ROOT/docker-up.sh" up --build -d
    fi
    load_env
    "$ROOT/docker-up.sh" ps
    print_urls
    ;;
  restart)
    ensure_swap
    ensure_docker
    chmod +x "$ROOT/docker-up.sh"
    "$ROOT/docker-up.sh" down --remove-orphans 2>/dev/null || true
    "$ROOT/docker-up.sh" up --build -d
    load_env
    print_urls
    ;;
  down|stop)
    chmod +x "$ROOT/docker-up.sh" 2>/dev/null || true
    "$ROOT/docker-up.sh" down --remove-orphans
    echo "Stopped."
    ;;
  logs)
    chmod +x "$ROOT/docker-up.sh" 2>/dev/null || true
    "$ROOT/docker-up.sh" logs -f --tail=100 "${@:2}"
    ;;
  status|ps)
    chmod +x "$ROOT/docker-up.sh" 2>/dev/null || true
    "$ROOT/docker-up.sh" ps
    load_env
    print_urls
    ;;
  *)
    echo "Unknown command: $cmd"
    echo "Usage: $0 [up|restart|down|logs|status]"
    exit 1
    ;;
esac
