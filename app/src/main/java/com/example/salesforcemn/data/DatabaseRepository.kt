package com.example.salesforcemn.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DatabaseRepository {

    private const val TAG = "DatabaseRepository"

    // 10.0.2.2 aponta para o localhost da máquina hospedeira no Emulador Android
    private const val API_BASE = "http://10.0.2.2:3000"

    suspend fun getUserByCpf(cpf: String): User? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE/users/$cpf")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val code = conn.responseCode
            if (code == 404) return@withContext null
            if (code != 200) {
                Log.e(TAG, "API retornou HTTP $code para CPF=$cpf")
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            return@withContext User(
                id = json.getString("id"),
                cpf = json.getString("cpf"),
                fullName = json.getString("fullName"),
                firstName = json.getString("firstName"),
                lastName = json.getString("lastName"),
                email = json.getString("email"),
                phone = json.getString("phone")
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao buscar usuário via API para CPF=$cpf: ${e.javaClass.simpleName} - ${e.message}")
            return@withContext null
        }
    }

    suspend fun getUserAccounts(cpf: String): List<Account> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE/users/$cpf/accounts")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode != 200) {
                Log.e(TAG, "API contas retornou HTTP ${conn.responseCode} para CPF=$cpf")
                return@withContext getMockAccounts(cpf)
            }
            val body = conn.inputStream.bufferedReader().readText()
            val jsonArray = JSONArray(body)
            val accounts = mutableListOf<Account>()
            for (i in 0 until jsonArray.length()) {
                val a = jsonArray.getJSONObject(i)
                val accountType = a.getString("accountType")
                val companyDetails = if (accountType == "PJ" && !a.isNull("companyDetails")) {
                    val cd = a.getJSONObject("companyDetails")
                    CompanyDetails(
                        cnpj = cd.optString("cnpj"),
                        companyName = cd.optString("companyName"),
                        tradeName = cd.optString("tradeName").ifEmpty { null },
                        corporateEmail = cd.optString("corporateEmail").ifEmpty { null }
                    )
                } else null
                accounts.add(Account(
                    id = a.getString("id"),
                    userId = a.getString("userId"),
                    accountType = accountType,
                    accountNumber = a.getString("accountNumber"),
                    agency = a.getString("agency"),
                    balance = a.getDouble("balance"),
                    availableLimit = a.getDouble("availableLimit"),
                    cardInvoice = a.getDouble("cardInvoice"),
                    companyDetails = companyDetails
                ))
            }
            Log.d(TAG, "Sucesso ao buscar contas via API: ${accounts.size} contas encontradas.")
            return@withContext if (accounts.isNotEmpty()) accounts else getMockAccounts(cpf)
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao buscar contas via API para CPF=$cpf: ${e.javaClass.simpleName} - ${e.message}")
            return@withContext getMockAccounts(cpf)
        }
    }

    suspend fun getTransactions(accountId: String): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_BASE/accounts/$accountId/transactions")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode != 200) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().readText()
            val jsonArray = JSONArray(body)
            val list = mutableListOf<Transaction>()
            for (i in 0 until jsonArray.length()) {
                val t = jsonArray.getJSONObject(i)
                list.add(Transaction(
                    id = t.getString("id"),
                    accountId = t.getString("accountId"),
                    title = t.getString("title"),
                    category = t.getString("category"),
                    amount = t.getDouble("amount"),
                    transactionType = t.getString("transactionType"),
                    transactionDate = t.getString("transactionDate")
                ))
            }
            return@withContext list
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao buscar transações: ${e.javaClass.simpleName} - ${e.message}")
            return@withContext emptyList()
        }
    }

    private fun getMockAccounts(cpf: String): List<Account> {
        val accounts = mutableListOf(
            Account(
                id = "11111111-1111-1111-1111-111111111111",
                userId = "user_$cpf",
                accountType = "PF",
                accountNumber = "100200-${cpf.takeLast(1)}",
                agency = "0001",
                balance = 12450.80,
                availableLimit = 7159.50,
                cardInvoice = 2840.50
            )
        )
        if (cpf != "33344455566" && cpf != "66677788899") {
            accounts.add(
                Account(
                    id = "22222222-2222-2222-2222-222222222222",
                    userId = "user_$cpf",
                    accountType = "PJ",
                    accountNumber = "500600-${cpf.takeLast(1)}",
                    agency = "0001",
                    balance = 85300.00,
                    availableLimit = 50000.00,
                    cardInvoice = 12350.00,
                    companyDetails = CompanyDetails(
                        cnpj = "12.345.678/0001-90",
                        companyName = "Empresa $cpf LTDA",
                        tradeName = "Grupo Corporativo $cpf",
                        corporateEmail = "contato@empresa.com.br"
                    )
                )
            )
        }
        return accounts
    }
}
