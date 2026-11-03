package com.example.salesforcemn

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.salesforcemn.data.Account
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.Locale

class AccountSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_selection)

        @Suppress("UNCHECKED_CAST")
        val accounts = (intent.getSerializableExtra("accounts") as? ArrayList<Account>) ?: emptyList()
        val userName = intent.getStringExtra("userName") ?: ""
        val switchMode = intent.getBooleanExtra("switchMode", false)

        val tvGreeting = findViewById<TextView>(R.id.tvWelcomeTitle)
        if (userName.isNotBlank()) tvGreeting?.text = "Olá, $userName!"

        val container = findViewById<LinearLayout>(R.id.llAccountsContainer)
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        accounts.forEach { account ->
            val cardView = LayoutInflater.from(this).inflate(R.layout.item_account_card, container, false)
            
            val card = cardView.findViewById<MaterialCardView>(R.id.cardAccount)
            val tvBadge = cardView.findViewById<TextView>(R.id.tvAccountBadge)
            val tvAgencyAccount = cardView.findViewById<TextView>(R.id.tvAgencyAccount)
            val tvName = cardView.findViewById<TextView>(R.id.tvAccountName)
            val tvCnpj = cardView.findViewById<TextView>(R.id.tvCnpjCompany)
            val tvBalance = cardView.findViewById<TextView>(R.id.tvAccountBalance)

            tvAgencyAccount.text = "Ag ${account.agency} • C/C ${account.accountNumber}"
            tvBalance.text = currencyFormat.format(account.balance)

            if (account.accountType == "PJ") {
                tvBadge.text = "Pessoa Jurídica"
                tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
                tvName.text = account.companyDetails?.tradeName ?: account.companyDetails?.companyName ?: "Empresa PJ"
                
                if (!account.companyDetails?.cnpj.isNullOrBlank()) {
                    tvCnpj.visibility = View.VISIBLE
                    tvCnpj.text = "CNPJ: ${account.companyDetails?.cnpj}"
                }
            } else {
                tvBadge.text = "Pessoa Física"
                tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4F46E5"))
                tvName.text = "Conta Pessoal"
                tvCnpj.visibility = View.GONE
            }

            card.setOnClickListener {
                if (switchMode) {
                    val result = Intent().apply {
                        putExtra("selectedAccount", account)
                        putExtra("userName", userName)
                    }
                    setResult(RESULT_OK, result)
                } else {
                    val intent = Intent(this, DashboardActivity::class.java).apply {
                        putExtra("selectedAccount", account)
                        putExtra("userName", userName)
                        putExtra("accounts", accounts as ArrayList<Account>)
                    }
                    startActivity(intent)
                }
                finish()
            }

            container.addView(cardView)
        }
    }

    private fun String?.isNullOrEmpty(): Boolean = this == null || this.trim().isEmpty()
}
