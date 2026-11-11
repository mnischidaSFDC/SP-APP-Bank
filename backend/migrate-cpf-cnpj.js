const { Pool } = require('pg')
const fs = require('fs')
const path = require('path')

const pool = new Pool({
  user: 'mnischida',
  host: 'localhost',
  database: 'mcn_app',
  password: '',
  port: 5432,
})

const CSV_PATH = path.join(process.env.HOME || '/Users/mnischida', 'Downloads/documentos_cpf_cnpj.csv')

function parseCsv(content) {
  const lines = content.trim().split('\n')
  const headers = lines[0].split(';').map(h => h.trim())
  return lines.slice(1).map(line => {
    const values = line.split(';')
    return Object.fromEntries(headers.map((h, i) => [h, (values[i] ?? '').trim()]))
  })
}

function splitName(fullName) {
  const parts = fullName.trim().split(' ')
  return {
    nome: parts[0] || '',
    sobrenome: parts.slice(1).join(' ') || '',
  }
}

async function migrate() {
  const content = fs.readFileSync(CSV_PATH, 'utf-8')
  const rows = parseCsv(content)

  // Somente entradas primárias PF (dataspace default, id_origem PF-XXXXX — exclui PFD)
  const pfRows = rows.filter(r =>
    r.dataspace === 'default' &&
    r.tipo_documento === 'CPF' &&
    /^PF-\d+$/.test(r.id_origem)
  )

  // Somente entradas primárias PJ (dataspace Empresas, tipo CNPJ, id_origem PJ-XXXX — exclui PJD)
  const pjRows = rows.filter(r =>
    r.dataspace === 'Empresas' &&
    r.tipo_documento === 'CNPJ' &&
    /^PJ-\d+$/.test(r.id_origem)
  )

  console.log(`CSV lido: ${pfRows.length} CPFs (PF-XXXXX) e ${pjRows.length} CNPJs (PJ-XXXX)`)

  const client = await pool.connect()
  try {
    await client.query('BEGIN')

    // --- ClientePF ---
    // INSERT ou UPDATE de nome/sobrenome se CPF já existe (não altera idClientePF para preservar FKs)
    let pfInserted = 0
    let pfUpdated = 0
    for (const row of pfRows) {
      const { nome, sobrenome } = splitName(row.nome)
      const result = await client.query(
        `INSERT INTO "ClientePF" ("idClientePF", nome, sobrenome, cpf, "tipoPessoa")
         VALUES ($1, $2, $3, $4, 'PF')
         ON CONFLICT (cpf) DO UPDATE
           SET nome      = EXCLUDED.nome,
               sobrenome = EXCLUDED.sobrenome
         RETURNING (xmax = 0) AS inserted`,
        [row.id_origem, nome, sobrenome, row.documento]
      )
      if (result.rows[0]?.inserted) pfInserted++
      else pfUpdated++
    }

    // --- ClientePJ ---
    // INSERT ou UPDATE de razaoSocial se CNPJ já existe
    let pjInserted = 0
    let pjUpdated = 0
    for (const row of pjRows) {
      const result = await client.query(
        `INSERT INTO "ClientePJ" ("idClientePJ", "razaoSocial", cnpj, "tipoPessoa")
         VALUES ($1, $2, $3, 'PJ')
         ON CONFLICT (cnpj) DO UPDATE
           SET "razaoSocial" = EXCLUDED."razaoSocial"
         RETURNING (xmax = 0) AS inserted`,
        [row.id_origem, row.nome, row.documento]
      )
      if (result.rows[0]?.inserted) pjInserted++
      else pjUpdated++
    }

    await client.query('COMMIT')
    console.log(`✓ ClientePF : ${pfInserted} inseridos, ${pfUpdated} atualizados`)
    console.log(`✓ ClientePJ : ${pjInserted} inseridos, ${pjUpdated} atualizados`)

    // Relatório de registros no banco que não estão no CSV (não são removidos automaticamente)
    const pfCpfs = pfRows.map(r => r.documento)
    const orphanPF = await pool.query(
      `SELECT "idClientePF", cpf, nome, sobrenome
         FROM "ClientePF"
        WHERE cpf <> ALL($1::text[])
        ORDER BY "idClientePF"`,
      [pfCpfs]
    )
    if (orphanPF.rows.length > 0) {
      console.log(`\n⚠  ${orphanPF.rows.length} CPF(s) no banco NÃO presentes no CSV (não removidos automaticamente):`)
      orphanPF.rows.forEach(r =>
        console.log(`   ${r.idClientePF}  ${r.cpf}  ${r.nome} ${r.sobrenome}`)
      )
    }

    const pjCnpjs = pjRows.map(r => r.documento)
    const orphanPJ = await pool.query(
      `SELECT "idClientePJ", cnpj, "razaoSocial"
         FROM "ClientePJ"
        WHERE cnpj <> ALL($1::text[])
        ORDER BY "idClientePJ"`,
      [pjCnpjs]
    )
    if (orphanPJ.rows.length > 0) {
      console.log(`\n⚠  ${orphanPJ.rows.length} CNPJ(s) no banco NÃO presentes no CSV (não removidos automaticamente):`)
      orphanPJ.rows.forEach(r =>
        console.log(`   ${r.idClientePJ}  ${r.cnpj}  ${r.razaoSocial}`)
      )
    }

  } catch (err) {
    await client.query('ROLLBACK')
    throw err
  } finally {
    client.release()
    await pool.end()
  }
}

migrate().catch(err => {
  console.error('Erro na migração:', err.message)
  process.exit(1)
})
