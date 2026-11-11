const express = require('express')
const bcrypt = require('bcrypt')
const { Pool } = require('pg')

const app = express()
app.use(express.json())
const PORT = 3000

const pool = new Pool({
  user: 'mnischida',
  host: 'localhost',
  database: 'mcn_app',
  password: '',
  port: 5432,
})

// Login: valida CPF + senha e retorna dados do usuário
app.post('/login', async (req, res) => {
  const { cpf, password } = req.body
  if (!cpf || !password) {
    return res.status(400).json({ error: 'CPF e senha são obrigatórios' })
  }
  try {
    const result = await pool.query(
      `SELECT
          pf."idClientePF" AS id,
          pf.cpf,
          pf.nome AS first_name,
          pf.sobrenome AS last_name,
          pf.nome || ' ' || pf.sobrenome AS full_name,
          pf.password_hash,
          e.email,
          t.telefone AS phone
       FROM "ClientePF" pf
       LEFT JOIN "ContatoEmail" e
              ON e."partyId" = pf."idClientePF" AND e."partyType" = 'PF' AND e.principal = true
       LEFT JOIN "ContatoTelefone" t
              ON t."partyId" = pf."idClientePF" AND t."partyType" = 'PF' AND t.principal = true
       WHERE pf.cpf = $1`,
      [cpf]
    )
    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'CPF ou senha inválidos' })
    }
    const row = result.rows[0]
    const valid = row.password_hash && await bcrypt.compare(password, row.password_hash)
    if (!valid) {
      return res.status(401).json({ error: 'CPF ou senha inválidos' })
    }
    res.json({
      id: row.id,
      cpf: row.cpf,
      fullName: row.full_name,
      firstName: row.first_name,
      lastName: row.last_name,
      email: row.email,
      phone: row.phone,
    })
  } catch (err) {
    console.error('Erro no login:', err.message)
    res.status(500).json({ error: 'Erro interno' })
  }
})

// Lista CPFs que possuem conta PJ (para o seletor na tela de login)
app.get('/users/with-pj', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT DISTINCT pf.cpf, pf.nome || ' ' || pf.sobrenome AS full_name
       FROM "ClientePF" pf
       JOIN accounts a ON a.id_cliente_pf = pf."idClientePF" AND a.account_type = 'PJ'
       ORDER BY pf.nome || ' ' || pf.sobrenome`
    )
    res.json(result.rows.map(r => ({ cpf: r.cpf, fullName: r.full_name })))
  } catch (err) {
    console.error('Erro ao buscar usuários com conta PJ:', err.message)
    res.status(500).json({ error: 'Erro interno' })
  }
})

// Busca usuário por CPF (sem validar senha — uso interno)
app.get('/users/:cpf', async (req, res) => {
  const { cpf } = req.params
  try {
    const result = await pool.query(
      `SELECT
          pf."idClientePF" AS id,
          pf.cpf,
          pf.nome AS first_name,
          pf.sobrenome AS last_name,
          pf.nome || ' ' || pf.sobrenome AS full_name,
          pf.password_hash,
          e.email,
          t.telefone AS phone
       FROM "ClientePF" pf
       LEFT JOIN "ContatoEmail" e
              ON e."partyId" = pf."idClientePF" AND e."partyType" = 'PF' AND e.principal = true
       LEFT JOIN "ContatoTelefone" t
              ON t."partyId" = pf."idClientePF" AND t."partyType" = 'PF' AND t.principal = true
       WHERE pf.cpf = $1`,
      [cpf]
    )
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Usuário não encontrado' })
    }
    const row = result.rows[0]
    res.json({
      id: row.id,
      cpf: row.cpf,
      fullName: row.full_name,
      firstName: row.first_name,
      lastName: row.last_name,
      email: row.email,
      phone: row.phone,
    })
  } catch (err) {
    console.error('Erro ao buscar usuário:', err.message)
    res.status(500).json({ error: 'Erro interno' })
  }
})

// Contas: via id_cliente_pf (sem dependência da tabela users)
app.get('/users/:cpf/accounts', async (req, res) => {
  const { cpf } = req.params
  try {
    const result = await pool.query(
      `SELECT
          a.id, a.id_cliente_pf, a.account_type, a.account_number, a.agency,
          a.balance, a.available_limit, a.card_invoice,
          p.cnpj, p.company_name, p.trade_name, p.corporate_email,
          pj."idClientePJ"
       FROM accounts a
       JOIN "ClientePF" pf ON pf."idClientePF" = a.id_cliente_pf
       LEFT JOIN pj_company_details p ON a.id = p.account_id
       LEFT JOIN "ClientePJ" pj ON pj.cnpj = p.cnpj
       WHERE pf.cpf = $1
       ORDER BY a.account_type ASC`,
      [cpf]
    )
    const accounts = result.rows.map(row => ({
      id: row.id,
      userId: row.id_cliente_pf,
      accountType: row.account_type,
      accountNumber: row.account_number,
      agency: row.agency,
      balance: parseFloat(row.balance),
      availableLimit: parseFloat(row.available_limit),
      cardInvoice: parseFloat(row.card_invoice),
      idClientePJ: row.idClientePJ || null,
      companyDetails: row.account_type === 'PJ' ? {
        cnpj: row.cnpj || '',
        companyName: row.company_name || '',
        tradeName: row.trade_name || null,
        corporateEmail: row.corporate_email || null,
      } : null,
    }))
    res.json(accounts)
  } catch (err) {
    console.error('Erro ao buscar contas:', err.message)
    res.status(500).json({ error: 'Erro interno' })
  }
})

app.get('/accounts/:accountId/transactions', async (req, res) => {
  const { accountId } = req.params
  try {
    const result = await pool.query(
      `SELECT id, account_id, title, category, amount, transaction_type,
              to_char(transaction_date AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS transaction_date
       FROM transactions
       WHERE account_id = $1
       ORDER BY transaction_date DESC
       LIMIT 20`,
      [accountId]
    )
    const transactions = result.rows.map(row => ({
      id: row.id,
      accountId: row.account_id,
      title: row.title,
      category: row.category,
      amount: parseFloat(row.amount),
      transactionType: row.transaction_type,
      transactionDate: row.transaction_date,
    }))
    res.json(transactions)
  } catch (err) {
    console.error('Erro ao buscar transações:', err.message)
    res.status(500).json({ error: 'Erro interno' })
  }
})

app.listen(PORT, '0.0.0.0', () => {
  console.log(`API local rodando em http://0.0.0.0:${PORT}`)
})
