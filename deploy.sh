#!/bin/bash
# Coffee Shop System — Deploy Script
# Запускать на сервере под пользователем arman

set -e

echo "🚀 Coffee Shop System — Деплой на сервер"
echo "========================================"

# Цвета
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker не найден! Установи Docker:${NC}"
    echo "curl -fsSL https://get.docker.com | sh"
    exit 1
fi

echo -e "${GREEN}✓ Docker найден${NC}"

# Проверка Docker Compose
if ! command -v docker compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose не найден!${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker Compose найден${NC}"

# Переход в директорию проекта
cd /home/arman/coffee-shop-system || {
    echo -e "${RED}❌ Директория проекта не найдена!${NC}"
    exit 1
}

echo -e "${GREEN}✓ Директория проекта: $(pwd)${NC}"

# Проверка .env файла
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠ .env файл не найден. Создаю из шаблона...${NC}"
    cp .env.production .env
    echo -e "${RED}❗ Заполни .env файл перед запуском!${NC}"
    echo "nano .env"
    exit 1
fi

echo -e "${GREEN}✓ .env файл найден${NC}"

# Остановка старых контейнеров
echo -e "${YELLOW}⏹  Остановка старых контейнеров...${NC}"
docker compose -f docker-compose.prod.yml down || true

# Сборка образа
echo -e "${YELLOW}🔨 Сборка Docker образа...${NC}"
docker compose -f docker-compose.prod.yml build --no-cache

# Запуск
echo -e "${YELLOW}▶  Запуск контейнеров...${NC}"
docker compose -f docker-compose.prod.yml up -d

# Ожидание запуска
echo -e "${YELLOW}⏳ Ожидание запуска приложения...${NC}"
sleep 15

# Проверка статуса
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✅ Деплой завершён!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "📊 Статус контейнеров:"
docker compose -f docker-compose.prod.yml ps
echo ""
echo "📝 Логи приложения:"
echo "docker compose -f docker-compose.prod.yml logs -f app"
echo ""
echo "🌐 Swagger UI: http://$(hostname -I | awk '{print $1}'):8080/swagger-ui.html"
echo ""
echo "🔧 Полезные команды:"
echo "  docker compose -f docker-compose.prod.yml logs -f     # Логи"
echo "  docker compose -f docker-compose.prod.yml restart     # Перезапуск"
echo "  docker compose -f docker-compose.prod.yml down        # Остановить"
echo "  docker compose -f docker-compose.prod.yml ps          # Статус"
