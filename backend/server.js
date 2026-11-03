const express = require('express')
const { Pool } = require('pg')

const app = express()
const PORT = 3000

const pool = new Pool({
  user: 'mnischida',
  host: 'localhost',
  database: 'mcn_app',
  password: '',
  port: 5432,
})

app.get('/users/:cpf', async (req, res) => {
  const { cpf } = req.params
  try {
    const result = await pool.query(
      'SELECT id, cpf, full_name, first_name, last_name, email, phone FROM users WHERE cpf = $1',
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

app.get('/users/:cpf/accounts', async (req, res) => {
  const { cpf } = req.params
  try {
    const result = await pool.query(
      `SELECT a.id, a.user_id, a.account_type, a.account_number, a.agency,
              a.balance, a.available_limit, a.card_invoice,
              p.cnpj, p.company_name, p.trade_name, p.corporate_email
       FROM accounts a
       JOIN users u ON a.user_id = u.id
       LEFT JOIN pj_company_details p ON a.id = p.account_id
       WHERE u.cpf = $1
       ORDER BY a.account_type ASC`,
      [cpf]
    )
    const accounts = result.rows.map(row => ({
      id: row.id,
      userId: row.user_id,
      accountType: row.account_type,
      accountNumber: row.account_number,
      agency: row.agency,
      balance: parseFloat(row.balance),
      availableLimit: parseFloat(row.available_limit),
      cardInvoice: parseFloat(row.card_invoice),
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
