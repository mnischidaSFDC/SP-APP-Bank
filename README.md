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

O emulador Android acessa o host via `10.0.2.2:3000`.

---

## Banco de Dados

### Pré-requisitos

- PostgreSQL 14+ (recomendado: PostgreSQL 18 via Homebrew)
- Porta padrão: `5432`

```bash
# Instalar via Homebrew (macOS)
brew install postgresql@18

# Iniciar o serviço
/opt/homebrew/opt/postgresql@18/bin/pg_ctl \
  -D /opt/homebrew/var/postgresql@18 start
```

### Criação do banco

```sql
CREATE DATABASE mcn_app;
```

Conectar ao banco antes de executar os DDLs:

```bash
psql -U <seu_usuario> -d mcn_app
```

### DDL — Tabelas

#### Tabelas do modelo Salesforce Data Cloud

```sql
-- Clientes Pessoa Física (espelha o dataspace "default" do Data Cloud)
CREATE TABLE "ClientePF" (
    "idClientePF"    VARCHAR(20)     NOT NULL PRIMARY KEY,
    nome             VARCHAR(100)    NOT NULL,
    sobrenome        VARCHAR(100)    NOT NULL,
    cpf              VARCHAR(20)     UNIQUE,
    "tipoPessoa"     VARCHAR(2)      NOT NULL DEFAULT 'PF',
    segmento         VARCHAR(50),
    agencia          VARCHAR(10),
    genero           VARCHAR(1),
    "dataNascimento" DATE,
    "rendaMensal"    NUMERIC(15,2),
    fonte            VARCHAR(50),
    password_hash    VARCHAR(255),
    "lastModified"   TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Clientes Pessoa Jurídica (espelha o dataspace "Empresas" do Data Cloud)
CREATE TABLE "ClientePJ" (
    "idClientePJ"      VARCHAR(20)   NOT NULL PRIMARY KEY,
    "razaoSocial"      VARCHAR(200)  NOT NULL,
    "nomeFantasia"     VARCHAR(200),
    cnpj               VARCHAR(20)   UNIQUE,
    "tipoPessoa"       VARCHAR(2)    NOT NULL DEFAULT 'PJ',
    porte              VARCHAR(20),
    segmento           VARCHAR(50),
    cnae               VARCHAR(20),
    "faturamentoAnual" NUMERIC(18,2),
    "dataFundacao"     DATE,
    agencia            VARCHAR(10),
    fonte              VARCHAR(50),
    "lastModified"     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Contatos de e-mail (PF ou PJ)
CREATE TABLE "ContatoEmail" (
    "idEmail"      VARCHAR(30)   NOT NULL PRIMARY KEY,
    "partyId"      VARCHAR(20)   NOT NULL,
    "partyType"    VARCHAR(2)    NOT NULL,   -- 'PF' ou 'PJ'
    email          VARCHAR(150)  NOT NULL,
    principal      BOOLEAN       NOT NULL DEFAULT true,
    "lastModified" TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Contatos de telefone (PF ou PJ)
CREATE TABLE "ContatoTelefone" (
    "idTelefone"   VARCHAR(30)  NOT NULL PRIMARY KEY,
    "partyId"      VARCHAR(20)  NOT NULL,
    "partyType"    VARCHAR(2)   NOT NULL,   -- 'PF' ou 'PJ'
    telefone       VARCHAR(30)  NOT NULL,
    pais           VARCHAR(5),
    principal      BOOLEAN      NOT NULL DEFAULT true,
    "lastModified" TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Documentos de identificação
CREATE TABLE "Documento" (
    "idDocumento"     VARCHAR(30)  NOT NULL PRIMARY KEY,
    "partyId"         VARCHAR(20)  NOT NULL,
    "partyType"       VARCHAR(2)   NOT NULL,
    "tipoDocumento"   VARCHAR(10)  NOT NULL,
    "numeroDocumento" VARCHAR(20)  NOT NULL,
    "lastModified"    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE ("partyId", "tipoDocumento")
);

-- Vínculo entre PF e PJ (sócios/representantes)
CREATE TABLE "VinculoPFPJ" (
    "idVinculo"        VARCHAR(20)   NOT NULL PRIMARY KEY,
    "idClientePF"      VARCHAR(20)   NOT NULL REFERENCES "ClientePF"("idClientePF"),
    "idClientePJ"      VARCHAR(20)   NOT NULL REFERENCES "ClientePJ"("idClientePJ"),
    papel              VARCHAR(50)   NOT NULL,
    "percParticipacao" NUMERIC(5,2),
    "poderesAssinatura" BOOLEAN      NOT NULL DEFAULT false,
    ativo              BOOLEAN       NOT NULL DEFAULT true,
    "dataInicio"       DATE,
    "dataFim"          DATE,
    "lastModified"     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Produtos contratados pelos clientes
CREATE TABLE "ProdutoContratado" (
    "idProdutoContratado" VARCHAR(20)   NOT NULL PRIMARY KEY,
    "idIndividual"        TEXT          NOT NULL,
    "cpfContratante"      VARCHAR(11)   NOT NULL,
    "tipoPessoa"          VARCHAR(2)    NOT NULL,
    "idAccount"           VARCHAR(20),
    cnpj                  VARCHAR(14),
    "idConta"             VARCHAR(50),
    "codigoProduto"       VARCHAR(20)   NOT NULL,
    "nomeProduto"         VARCHAR(100)  NOT NULL,
    "categoriaProduto"    VARCHAR(30)   NOT NULL,
    status                VARCHAR(20)   NOT NULL,
    "valorContratado"     NUMERIC(15,2) NOT NULL DEFAULT 0,
    "dataContratacao"     TIMESTAMPTZ   NOT NULL,
    "canalContratacao"    VARCHAR(30)   NOT NULL,
    "lastModified"        TIMESTAMPTZ   NOT NULL DEFAULT now()
);
```

#### Tabelas do app mobile

```sql
-- Usuários de autenticação do app (legacy — login usa ClientePF diretamente)
CREATE TABLE users (
    id            UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    cpf           VARCHAR(11)   NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    full_name     VARCHAR(150)  NOT NULL,
    email         VARCHAR(150)  NOT NULL UNIQUE,
    phone         VARCHAR(20),
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    fonte         VARCHAR(50)   DEFAULT 'AppMobile',
    created_at    TIMESTAMPTZ   DEFAULT now(),
    updated_at    TIMESTAMPTZ   DEFAULT now(),
    last_modified TIMESTAMPTZ   DEFAULT now()
);

-- Contas bancárias (PF e PJ) vinculadas a ClientePF
CREATE TABLE accounts (
    id              UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          REFERENCES users(id) ON DELETE CASCADE,
    id_cliente_pf   VARCHAR(20)   REFERENCES "ClientePF"("idClientePF"),
    account_type    VARCHAR(10)   NOT NULL CHECK (account_type IN ('PF', 'PJ')),
    account_number  VARCHAR(20)   NOT NULL UNIQUE,
    agency          VARCHAR(10)   NOT NULL DEFAULT '0001',
    balance         NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    available_limit NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    card_invoice    NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    is_active       BOOLEAN       DEFAULT true,
    created_at      TIMESTAMPTZ   DEFAULT now(),
    last_modified   TIMESTAMPTZ   DEFAULT now()
);

-- Detalhes da empresa para contas PJ
CREATE TABLE pj_company_details (
    id               UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID         NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE CASCADE,
    cnpj             VARCHAR(14)  NOT NULL UNIQUE,
    company_name     VARCHAR(200) NOT NULL,
    trade_name       VARCHAR(200),
    corporate_email  VARCHAR(150),
    created_at       TIMESTAMPTZ  DEFAULT now(),
    last_modified    TIMESTAMPTZ  DEFAULT now()
);

-- Transações financeiras por conta
CREATE TABLE transactions (
    id               UUID          NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id       UUID          NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    title            VARCHAR(100)  NOT NULL,
    category         VARCHAR(50)   NOT NULL,
    amount           NUMERIC(15,2) NOT NULL,
    transaction_type VARCHAR(10)   NOT NULL CHECK (transaction_type IN ('CREDIT', 'DEBIT')),
    transaction_date TIMESTAMPTZ   DEFAULT now(),
    last_modified    TIMESTAMPTZ   DEFAULT now()
);
```

### População dos dados

Após criar as tabelas, execute os scripts de migração na ordem:

```bash
cd backend

# 1. Importa ClientePF (501 CPFs) e ClientePJ (150 CNPJs) a partir do CSV
node migrate-cpf-cnpj.js

# 2. Cria contas PF para todos os usuários e contas PJ
#    para os 150 usuários presentes no dataspace "Empresas"
node populate-accounts.js
```

O arquivo CSV de origem é `~/Downloads/documentos_cpf_cnpj.csv`  
(formato: `dataspace;tipo_documento;documento;id_origem;nome;cpf_tambem_no_default;documento_repetido_no_dataspace`).

> **Senha padrão de todos os clientes:** `123456` (bcrypt hash armazenado em `ClientePF.password_hash`)

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
