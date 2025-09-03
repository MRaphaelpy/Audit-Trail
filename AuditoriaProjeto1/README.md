Sistema acadêmico de auditoria desenvolvido para disciplina da faculdade.  
O projeto tem como objetivo demonstrar boas práticas de **coleta, armazenamento e visualização de logs** em uma aplicação distribuída.  

## 📌 Arquitetura

- **Backend (Spring Boot)**
  - Responsável por gerenciar os logs e armazená-los no banco de dados (MySQL).
  - Implementa autenticação simples, tentativas de login, captcha simulado e suporte a 2FA.

- **Bot (Python)**
  - Faz brute force no endpoint para tentar descobrir a senha

- **Frontend (React)**
  - Interface web para consulta e criação dos dados.

## ⚙️ Configuração

### 1. Clone o repositório
```bash
git clone https://github.com/mraphaelpy/audit-trail.git
cd audit-trail
```

### 2. Backend (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
Ou, se usar Gradle:
```bash
./gradlew bootRun
```

Configurações no `.env`:
```env
#nome da aplicação
APP_NAME=Sistema de Auditoria
APP_VERSION=1.0.0
APP_ENV=development

#Configuração dos Casos de Uso
TWO_FACTOR_ENABLED=false
TWO_FACTOR_CODE_EXPIRY_MINUTES=10
INFINITE_ATTEMPTS_ENABLED=false
ACCOUNT_LOCK_ENABLED=true
ACCOUNT_LOCK_MAX_ATTEMPTS=3
ACCOUNT_LOCK_DURATION_MINUTES=30
#banco de dados
DB_HOST=localhost
DB_PORT=3307
DB_NAME=auditoria_db
DB_USERNAME=auditoria_user
DB_PASSWORD=auditoria123

#Configuração do Maillog
EMAIL_HOST=localhost
EMAIL_PORT=1026
EMAIL_USERNAME=
EMAIL_PASSWORD=
EMAIL_FROM=noreply@yasmim.com
JWT_SECRET=
JWT_EXPIRATION=86400000
LOG_LEVEL=INFO
LOG_SQL=false
SERVER_PORT=8080


### 3. Bot (Python)
```bash
cd bot
python3 main.py
```

### 4. Frontend (React)
```bash
cd frontend
npm install
npm start
```

## 📝 Funcionalidades

✅ Armazenamento de logs em banco MySQL  
✅ Tentativas de login configuráveis via `.env`  
✅ Captcha simples (provavelmente :) )  
✅ Suporte a autenticação de dois fatores (simulada)  
✅ Visualização de eventos no frontend  

## 🎯 Objetivo acadêmico

Este projeto **não é destinado para produção**, apenas para fins educacionais.  (se colocar me produçao é por sua conta e risco rs)
Ele exemplifica como diferentes camadas (backend, frontend e automação com bot) podem interagir em um sistema de auditoria.
