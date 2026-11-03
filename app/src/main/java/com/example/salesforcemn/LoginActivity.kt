package com.example.salesforcemn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.salesforcemn.data.DatabaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.salesforce.marketingcloud.cdp.CdpConfig
import com.salesforce.marketingcloud.cdp.CdpSdk
import com.salesforce.marketingcloud.cdp.consent.Consent
import com.salesforce.marketingcloud.cdp.events.Event
import com.salesforce.marketingcloud.sfmcsdk.InitializationStatus
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener.AndroidLogger
import com.salesforce.personalization.PersonalizationConfig
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initializing Salesforce SDK
        initSalesforceSdk()

        // Track Login Screen view custom event
        trackScreenView("Login Tela", "AppLaunch")

        val etContactKey = findViewById<TextInputEditText>(R.id.etContactKey)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val contactKey = etContactKey.text?.toString().orEmpty().ifEmpty { "00011122233" }

            lifecycleScope.launch {
                // Busca os dados do usuário no PostgreSQL pelo CPF/ContactKey
                val dbUser = DatabaseRepository.getUserByCpf(contactKey)

                if (dbUser == null) {
                    Log.e("LoginActivity", "AVISO: usuário CPF=$contactKey não encontrado no banco. Verifique a conexão com o PostgreSQL.")
                    Toast.makeText(this@LoginActivity, "Erro: usuário não encontrado.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val firstName = dbUser.firstName
                val lastName = dbUser.lastName
                val email = dbUser.email
                val phoneNumber = dbUser.phone

                Log.d("LoginActivity", "Usuário encontrado no PostgreSQL: $firstName $lastName ($email)")

                // 1. Identity editor: gera identity, contactPointEmail, contactPointPhone
                //    profileId removido para não gerar partyIdentification "MC Subscriber Key"
                SFMCSdk.requestSdk { sdk ->
                    sdk.identity.edit {
                        attributes.putAll(mapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email,
                            "phoneNumber" to phoneNumber,
                            "contactKey" to contactKey
                        ))
                    }
                }

                // 2. Único partyIdentification enviado: IDName = "CPF"
                CdpSdk.requestSdk { cdp ->
                    Event.profile(
                        eventType = "partyIdentification",
                        eventAttributes = mapOf(
                            "IDName" to "CPF",
                            "IDType" to "Person Identifier",
                            "userId" to contactKey
                        )
                    )?.let {
                        cdp.track(it)
                        Log.d("LoginActivity", "partyIdentification CPF tracked.")
                    }
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
        val appId = "e4fc0f7f-11f7-4b70-ac2e-0dd4d79c0b04"
        val endpoint = "https://gqyt0ntgg5tg0mrsgztgcmjxmq.c360a.salesforce.com"

        SFMCSdk.setLogging(LogLevel.DEBUG, AndroidLogger())

        val cdpModuleConfig = CdpConfig.Builder(this, appId, endpoint)
            .trackScreens(true)
            .trackLifecycle(true)
            .sessionTimeout(600)
            .eventFlushRate(CdpConfig.EventFlushRate.Quantity(1))
            .build()

        val personalizationConfig = PersonalizationConfig.Builder(this)
            .cdnUrl("https://cdn.c360a.salesforce.com/beacon/module_configuration/e4fc0f7f-11f7-4b70-ac2e-0dd4d79c0b04/config/app-config.json")
            .build()

        SFMCSdk.configure(this, SFMCSdkModuleConfig.build {
            this.cdpModuleConfig = cdpModuleConfig
            this.personalizationModuleConfig = personalizationConfig
        }) { status ->
            if (status.status == InitializationStatus.SUCCESS) {
                CdpSdk.requestSdk { cdp ->
                    cdp.consent = Consent.OPT_IN
                }
                Log.d("LoginActivity", "SFMCSdk configured successfully!")
            }
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
