package com.example.salesforcemn.data

import java.io.Serializable

data class User(
    val id: String,
    val cpf: String,
    val fullName: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?
) : Serializable

data class CompanyDetails(
    val cnpj: String,
    val companyName: String,
    val tradeName: String?,
    val corporateEmail: String?
) : Serializable

data class Account(
    val id: String,
    val userId: String,
    val accountType: String, // "PF" or "PJ"
    val accountNumber: String,
    val agency: String,
    val balance: Double,
    val availableLimit: Double,
    val cardInvoice: Double,
    val companyDetails: CompanyDetails? = null
) : Serializable

data class Transaction(
    val id: String,
    val accountId: String,
    val title: String,
    val category: String,
    val amount: Double,
    val transactionType: String, // "CREDIT" or "DEBIT"
    val transactionDate: String
) : Serializable
