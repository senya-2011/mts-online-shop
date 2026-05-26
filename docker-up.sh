#!/usr/bin/env bash
# Запуск compose из корня репозитория так, чтобы:
# 1) подхватился .env в корне (без --env-file .env — у Docker из Snap он ломается в /var/lib/snapd/void/);
# 2) ${VAR} в docker-compose.yml подставлялись из этого .env (--project-directory = корень);
# 3) путь к compose — только АБСОЛЮТНЫЙ: у Snap относительный -f docker/... превращается в /var/lib/snapd/void/docker/...
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE="$ROOT/docker/docker-compose.yml"
cd "$ROOT"
exec docker compose --project-directory "$ROOT" -f "$COMPOSE" "$@"
