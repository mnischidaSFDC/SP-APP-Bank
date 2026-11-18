package com.example.salesforcemn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.salesforcemn.data.DatabaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.salesforce.marketingcloud.cdp.CdpSdk
import com.salesforce.marketingcloud.cdp.events.Event
import com.google.firebase.messaging.FirebaseMessaging
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("LoginActivity", "POST_NOTIFICATIONS permission granted: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Solicita permissão de notificações no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Diagnóstico Firebase independente do SDK
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_DIAG", "Token gerado com sucesso: ${task.result}")
            } else {
                Log.e("FCM_DIAG", "Erro ao gerar token: ${task.exception?.javaClass?.simpleName} — ${task.exception?.message}")
            }
        }

        // Initializing Salesforce SDK — trackScreenView é chamado dentro do callback de sucesso
        initSalesforceSdk()

        val etContactKey = findViewById<AutoCompleteTextView>(R.id.etContactKey)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnClearCache = findViewById<MaterialButton>(R.id.btnClearCache)

        // Carrega usuários com conta PJ como sugestões no campo CPF
        lifecycleScope.launch {
            val pjUsers = DatabaseRepository.getUsersWithPjAccount()
            if (pjUsers.isNotEmpty()) {
                val labels = pjUsers.map { "${it.fullName}  •  ${it.cpf}" }
                val adapter = ArrayAdapter(this@LoginActivity, android.R.layout.simple_dropdown_item_1line, labels)
                etContactKey.setAdapter(adapter)
                etContactKey.setOnItemClickListener { _, _, position, _ ->
                    etContactKey.setText(pjUsers[position].cpf)
                    etContactKey.dismissDropDown()
                }
            }
        }

        btnClearCache.setOnClickListener {
            clearAppCache()
        }

        btnLogin.setOnClickListener {
            val contactKey = etContactKey.text?.toString().orEmpty().trim()
            val password = etPassword.text?.toString().orEmpty()

            if (contactKey.isEmpty() || password.isEmpty()) {
                Toast.makeText(this@LoginActivity, "Informe o CPF e a senha.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val dbUser = DatabaseRepository.login(contactKey, password)

                if (dbUser == null) {
                    Log.e("LoginActivity", "Login falhou para CPF=$contactKey")
                    Toast.makeText(this@LoginActivity, "CPF ou senha inválidos.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val firstName = dbUser.firstName
                val lastName = dbUser.lastName
                val email = dbUser.email
                val phoneNumber = dbUser.phone

                Log.d("LoginActivity", "Usuário encontrado no PostgreSQL: $firstName $lastName ($email)")

                // 1. Identity: partyIdentification (MAM) + atributos CDP
                // Monta attrs sem valores nulos/vazios — null em putAll aborta o edit silenciosamente
                val cdpAttrs = mutableMapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                )
                if (email.isNotEmpty()) cdpAttrs["email"] = email
                if (!phoneNumber.isNullOrEmpty()) cdpAttrs["phoneNumber"] = phoneNumber

                SFMCSdk.requestSdk { sdk ->
                    sdk.identity.edit {
                        partyIdentificationNumber = contactKey
                        partyIdentificationName = "CPF"
                        partyIdentificationType = "CPF"
                        attributes.putAll(cdpAttrs)
                    }
                    Log.d("CDP_IDENTITY", "identity.edit enviado — partyId=$contactKey attrs=$cdpAttrs")
                }

                // Envia partyIdentification como ProfileEvent para o Data Cloud (CDP)
                // sdk.identity.edit envia partyIdentification* apenas para o MAM;
                // o CdpSdk.track com Event.profile é necessário para o Data Cloud receber o evento
                val partyIdAttrs = mapOf(
                    "partyIdentificationNumber" to contactKey,
                    "partyIdentificationName" to "CPF",
                    "partyIdentificationType" to "CPF"
                )
                CdpSdk.requestSdk { cdp ->
                    val event = Event.profile("partyIdentification", partyIdAttrs)
                    cdp.track(event)
                    Log.d("CDP_IDENTITY", "ProfileEvent partyIdentification tracked no Data Cloud — cpf=$contactKey")
                }

                // 2. Track Custom Event on login action
                EventManager.customEvent("custom", mapOf(
                    "interactionName" to "Login Efetuado",
                    "interactionType" to "userAction",
                    "screenName" to "Login Tela",
                    "userEmail" to email,
                    "contactKey" to contactKey
                ))?.track()

                Toast.makeText(this@LoginActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@LoginActivity, LoadingActivity::class.java).apply {
                    putExtra("contactKey", contactKey)
                    putExtra("userName", firstName)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun initSalesforceSdk() {
        // O SDK é configurado em SalesforceApp.onCreate; aqui só registramos a tela de login
        CdpSdk.requestSdk { trackScreenView("Login Tela", "AppLaunch") }
    }

    private fun clearAppCache() {
        try {
            // Cache de arquivos do app
            cacheDir.deleteRecursively()
            // SharedPreferences de todos os namespaces do app
            val prefsDir = applicationContext.filesDir.parentFile?.resolve("shared_prefs")
            prefsDir?.listFiles()?.forEach { it.delete() }
            // Reseta identidade no SDK para desvincular o usuário anterior
            SFMCSdk.requestSdk { sdk ->
                sdk.identity.edit {
                    partyIdentificationNumber = ""
                    partyIdentificationName = ""
                    partyIdentificationType = ""
                }
            }
            Log.d("LoginActivity", "Cache limpo com sucesso")
            Toast.makeText(this, "Cache limpo. Reiniciando...", Toast.LENGTH_SHORT).show()
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)!!.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(launchIntent)
            Runtime.getRuntime().exit(0)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Erro ao limpar cache: ${e.message}")
            Toast.makeText(this, "Erro ao limpar cache.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun trackScreenView(screenName: String, previousScreen: String) {
        EventManager.customEvent(
            name = "custom",
            attributes = mapOf(
                "interactionName" to screenName,
                "interactionType" to "screenView",
                "screenName" to screenName,
                "previousScreen" to previousScreen
            )
        )?.track()
        Log.d("LoginActivity", "Custom Event tracked: $screenName")
    }
}
