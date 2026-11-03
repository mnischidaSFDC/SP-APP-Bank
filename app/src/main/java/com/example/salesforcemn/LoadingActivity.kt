package com.example.salesforcemn

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.salesforcemn.data.DatabaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val contactKey = intent.getStringExtra("contactKey") ?: "00011122233"
        val userName = intent.getStringExtra("userName") ?: ""
        val tvMessage = findViewById<TextView>(R.id.tvLoadingMessage)
        tvMessage?.text = "Carregando suas contas e informações..."

        lifecycleScope.launch {
            // Busca as contas do banco de dados durante a animação
            val accounts = DatabaseRepository.getUserAccounts(contactKey)

            // Garante 3 segundos de permanência na tela de carregamento
            delay(3000)

            val intent = Intent(this@LoadingActivity, AccountSelectionActivity::class.java).apply {
                putExtra("contactKey", contactKey)
                putExtra("userName", userName)
                putExtra("accounts", ArrayList(accounts))
            }
            startActivity(intent)
            finish()
        }
    }
}
