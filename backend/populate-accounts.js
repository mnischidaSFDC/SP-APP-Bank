// Populates accounts table:
// - PF account for every ClientePF user
// - PJ account (+ pj_company_details) for first 150 users that appear in Empresas CPF dataspace
// - Paired 1:1 with ClientePJ companies (150 companies, 150 PJ accounts)

const fs = require('fs')
const path = require('path')
const { Pool } = require('pg')

const pool = new Pool({
  user: 'mnischida',
  host: 'localhost',
  database: 'mcn_app',
  password: '',
  port: 5432,
})

function randomBalance(min, max) {
  return (Math.random() * (max - min) + min).toFixed(2)
}

function randomAccountNumber(prefix, index) {
  return prefix + String(index).padStart(5, '0') + '-' + Math.floor(Math.random() * 9 + 1)
}

async function main() {
  const csvPath = path.join(process.env.HOME, 'Downloads', 'documentos_cpf_cnpj.csv')
  const lines = fs.readFileSync(csvPath, 'utf8').split('\n').filter(Boolean)

  // Extract CPFs that appear in Empresas dataspace (these users get PJ accounts)
  const empresasCpfs = new Set()
  for (const line of lines.slice(1)) {
    const cols = line.split(';')
    if (cols[0] === 'Empresas' && cols[1] === 'CPF') {
      empresasCpfs.add(cols[2].trim())
    }
  }

  console.log(`Empresas CPFs encontrados: ${empresasCpfs.size}`)

  // Get all ClientePF users
  const pfResult = await pool.query(
    `SELECT "idClientePF", cpf, nome, sobrenome FROM "ClientePF" ORDER BY "idClientePF"`
  )
  const pfUsers = pfResult.rows
  console.log(`ClientePF users: ${pfUsers.length}`)

  // Get all ClientePJ companies in order
  const pjResult = await pool.query(
    `SELECT "idClientePJ", cnpj, "razaoSocial", "nomeFantasia" FROM "ClientePJ" ORDER BY "idClientePJ"`
  )
  const companies = pjResult.rows
  console.log(`ClientePJ companies: ${companies.length}`)

  // Identify PF users who get PJ accounts (intersection with empresasCpfs, limited to 150)
  const pjCandidates = pfUsers.filter(u => empresasCpfs.has(u.cpf)).slice(0, companies.length)
  console.log(`PJ account candidates (capped at ${companies.length}): ${pjCandidates.length}`)

  const client = await pool.connect()
  try {
    await client.query('BEGIN')

    let pfCreated = 0
    let pjCreated = 0

    // Create PF account for every ClientePF user
    for (let i = 0; i < pfUsers.length; i++) {
      const u = pfUsers[i]
      const accountNumber = randomAccountNumber('PF', i + 1)
      const balance = randomBalance(500, 50000)
      const limit = randomBalance(1000, 20000)

      await client.query(
        `INSERT INTO accounts (id_cliente_pf, account_type, account_number, agency, balance, available_limit, card_invoice)
         VALUES ($1, 'PF', $2, '0001', $3, $4, 0)
         ON CONFLICT (account_number) DO NOTHING`,
        [u.idClientePF, accountNumber, balance, limit]
      )
      pfCreated++
    }

    // Create PJ account for each PJ candidate paired with a company
    for (let i = 0; i < pjCandidates.length; i++) {
      const u = pjCandidates[i]
      const company = companies[i]
      const accountNumber = randomAccountNumber('PJ', i + 1)
      const balance = randomBalance(5000, 500000)
      const limit = randomBalance(10000, 100000)

      const accountInsert = await client.query(
        `INSERT INTO accounts (id_cliente_pf, account_type, account_number, agency, balance, available_limit, card_invoice)
         VALUES ($1, 'PJ', $2, '0001', $3, $4, 0)
         ON CONFLICT (account_number) DO NOTHING
         RETURNING id`,
        [u.idClientePF, accountNumber, balance, limit]
      )

      if (accountInsert.rows.length > 0) {
        const accountId = accountInsert.rows[0].id
        const tradeName = company.nomeFantasia || null
        const corpEmail = `contato@${company.cnpj}.salesforcebank.com.br`

        await client.query(
          `INSERT INTO pj_company_details (account_id, cnpj, company_name, trade_name, corporate_email)
           VALUES ($1, $2, $3, $4, $5)
           ON CONFLICT (cnpj) DO NOTHING`,
          [accountId, company.cnpj, company.razaoSocial, tradeName, corpEmail]
        )
        pjCreated++
      }
    }

    await client.query('COMMIT')
    console.log(`\nContas criadas:`)
    console.log(`  PF: ${pfCreated}`)
    console.log(`  PJ: ${pjCreated}`)
  } catch (err) {
    await client.query('ROLLBACK')
    console.error('Erro:', err.message)
    throw err
  } finally {
    client.release()
    pool.end()
  }
}

main().catch(err => {
  console.error(err)
  process.exit(1)
})
