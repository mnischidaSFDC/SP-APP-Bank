# SP APP Bank

App Android de demonstração bancária com integração **Salesforce Einstein Personalization**.

## Visão Geral

Aplicativo fictício de banco digital utilizado para validar as três abordagens de personalização do Einstein Personalization SDK:

| Abordagem | Onde testar | Como funciona |
|---|---|---|
| **Pro-Code** | Tela Testes → aba Pro-Code | `fetchDecisions()` + Views XML manuais |
| **Low-Code** | Tela Testes → aba Low-Code | `ContentZone` + `Component<Model>` (Compose) |
| **Decisioning API** | Tela Testes → aba Decisioning API | `fetchDecisions()` com inspeção da resposta bruta |

## Stack

- **Android** — Kotlin, View-based + Jetpack Compose
- **Salesforce SDKs** — `sdk:3.+`, `lowcodemobile:1.+`, SFMCSdk, CDP
- **Backend** — Node.js / Express + PostgreSQL

## Configuração Salesforce

| Configuração | Valor |
|---|---|
| App ID | `e4fc0f7f-11f7-4b70-ac2e-0dd4d79c0b04` |
| Endpoint | `https://gqyt0ntgg5tg0mrsgztgcmjxmq.c360a.salesforce.com` |
| CDN Config URL | `https://cdn.c360a.salesforce.com/beacon/module_configuration/e4fc0f7f-.../app-config.json` |
| Content Zone | `mn_lowcode_test` |
| Personalization Points | `banner_home_pf`, `home_banner_pj`, `Low_Code` |
| Componente Low-Code | `custom_oferta_card` |

## Campos do Componente `custom_oferta_card`

```
title     (String, obrigatório)
subtitle  (String, opcional)
imageUrl  (String, opcional)
ctaLabel  (String, opcional)
ctaUrl    (String, opcional)
badgeText (String, opcional)
```

## Backend

```bash
cd backend
npm install
node server.js
```

Requer PostgreSQL local na porta `5432`, banco `mcn_app`.  
O emulador Android acessa o host via `10.0.2.2:3000`.

## Build

```bash
./gradlew assembleDebug
```

## Branches

| Branch | Propósito |
|---|---|
| `main` | Produção |
| `staging` | Pré-produção / validação |
| `develop` | Integração de features |
| `feature/*` | Desenvolvimento de funcionalidades |
| `hotfix/*` | Correções urgentes em produção |
