# Команда для запуска Compose
DOCKER_COMPOSE = docker-compose

# Адрес Nexus для health-проверки
NEXUS_URL = http://localhost:8081

# Список "фантомных" целей (не файлы)
.PHONY: all up start stop clean logs rebuild build-artifacts

# -------------------------
# Windows PowerShell shell
# -------------------------
ifeq ($(OS),Windows_NT)
SHELL := powershell.exe
.SHELLFLAGS := -NoProfile -ExecutionPolicy Bypass -Command

# PowerShell ожидание Nexus
WAIT_CMD = while ($$true) { \
    try { \
        Invoke-WebRequest -UseBasicParsing -Uri '$(NEXUS_URL)/service/rest/v1/status' -TimeoutSec 5 -ErrorAction Stop | Out-Null; \
        break; \
    } catch { \
        Write-Host 'Nexus not ready, sleeping...'; \
        Start-Sleep -Seconds 5; \
    } \
}

# Удаление build-папки безопасно (если нет — не падаем)
RM_BUILD = if (Test-Path 'person-service_\build') { Remove-Item -Recurse -Force 'person-service_\build' }

else
# Linux/macOS (если когда-нибудь понадобится)
WAIT_CMD = until curl -sf $(NEXUS_URL)/service/rest/v1/status; do \
	echo 'Nexus not ready, sleeping...'; sleep 5; \
done
RM_BUILD = rm -rf ./person-service_/build
endif

all: up build-artifacts start

up:
	$(DOCKER_COMPOSE) up -d nexus
	@Write-Host "Waiting for Nexus to be healthy..."
	@$(WAIT_CMD)
	@Write-Host "Nexus is healthy!"

build-artifacts:
	@$(DOCKER_COMPOSE) build persons-api --no-cache

start:
	$(DOCKER_COMPOSE) up -d

stop:
	$(DOCKER_COMPOSE) down

clean: stop
	$(DOCKER_COMPOSE) rm -f
	@docker volume prune -f | Out-Null
	@$(RM_BUILD)

logs:
	$(DOCKER_COMPOSE) logs -f --tail=200

rebuild: clean all