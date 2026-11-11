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
import com.salesforce.marketingcloud.cdp.CdpConfig
import com.salesforce.marketingcloud.cdp.CdpSdk
import com.salesforce.marketingcloud.cdp.consent.Consent
import com.google.firebase.messaging.FirebaseMessaging
import com.salesforce.marketingcloud.mobileappmessaging.MobileAppMessaging
import com.salesforce.marketingcloud.mobileappmessaging.MobileAppMessagingConfig
import com.salesforce.marketingcloud.mobileappmessaging.registration.RegistrationManager
import com.salesforce.marketingcloud.pushfeature.config.PushFeatureConfig
import com.salesforce.marketingcloud.pushfeature.notifications.NotificationCustomizationOptions
import com.salesforce.marketingcloud.sfmcsdk.InitializationStatus
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener.AndroidLogger
import com.salesforce.personalization.PersonalizationConfig
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    companion object {
        private var sdkInitialized = false
    }

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
        // SDK já inicializado: apenas re-registra o listener MAM para o novo ciclo de login
        if (sdkInitialized) {
            Log.d("LoginActivity", "SFMCSdk já inicializado — re-registrando listener MAM")
            registerMamListener()
            trackScreenView("Login Tela", "AppLaunch")
            return
        }
        sdkInitialized = true

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

        val mamConfig = MobileAppMessagingConfig.builder()
            .moduleApplicationId("a6f9d046-be13-4061-84d0-392ffd0d6cbb")
            .tenantId("core/prod/00DgK00000ZBjNRUA1")
            .accessToken("2G9mnXLQ8PsZpjKJU0CDudcQ")
            .endpointUrl("https://api.salesforce.com/")
            .build()

        val pushConfig = PushFeatureConfig.builder()
            .setSenderId("989495332954")
            .setNotificationCustomizationOptions(
                NotificationCustomizationOptions.create(R.mipmap.ic_launcher)
            )
            .build()

        SFMCSdk.configure(this, SFMCSdkModuleConfig.build {
            this.cdpModuleConfig = cdpModuleConfig
            this.personalizationModuleConfig = personalizationConfig
            this.mamModuleConfig = mamConfig
            this.pushFeatureModuleConfig = pushConfig
        }) { status ->
            Log.d("LoginActivity", "SFMCSdk status: ${status.status}")

            // CDP consent é setado independentemente do MAM/Push — não bloqueia Data Cloud se outro módulo falhar
            CdpSdk.requestSdk { cdp ->
                cdp.consent = Consent.OPT_IN
                Log.d("LoginActivity", "CDP Consent.OPT_IN confirmado — trackScreenView disparando")
                trackScreenView("Login Tela", "AppLaunch")
            }

            if (status.status == InitializationStatus.SUCCESS) {
                Log.d("LoginActivity", "SFMCSdk configurado com sucesso (todos os módulos)")

                registerMamListener()

                // Registra o token atual no PushFeature (onNewToken pode não disparar se o token já existe)
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("LoginActivity", "FCM SystemToken: $token")
                        com.salesforce.marketingcloud.pushfeature.PushFeature.requestSdk { push ->
                            push.getPushMessageManager().setPushToken(token)
                            Log.d("LoginActivity", "Token registrado no PushFeature com sucesso")
                        }
                    } else {
                        Log.e("LoginActivity", "Falha ao obter FCM token", task.exception)
                    }
                }
            } else {
                Log.w("LoginActivity", "SFMCSdk status não é SUCCESS: ${status.status} — MAM ou Push pode estar falhando")
            }
        }
    }

    private fun registerMamListener() {
        Log.d("MAM_REGISTRATION", "MAM pronto: ${MobileAppMessaging.isReady()} | registrando listener...")
        MobileAppMessaging.requestSdk { mam ->
            Log.d("MAM_REGISTRATION", "requestSdk callback executou — deviceId: ${mam.getRegistrationManager().getDeviceId()}")
            mam.getRegistrationManager().registerForRegistrationEvents(
                object : RegistrationManager.RegistrationEventListener {
                    override fun onRegistrationReceived(responseBody: org.json.JSONObject) {
                        Log.d("MAM_REGISTRATION", "Payload enviado ao servidor:")
                        Log.d("MAM_REGISTRATION", "  partyIdentificationNumber: ${responseBody.optString("partyIdentificationNumber")}")
                        Log.d("MAM_REGISTRATION", "  partyIdentificationName  : ${responseBody.optString("partyIdentificationName")}")
                        Log.d("MAM_REGISTRATION", "  partyIdentificationType  : ${responseBody.optString("partyIdentificationType")}")
                        Log.d("MAM_REGISTRATION", "  deviceSystemToken        : ${responseBody.optString("deviceSystemToken")}")
                        Log.d("MAM_REGISTRATION", "  deviceId                 : ${responseBody.optString("deviceId")}")
                        Log.d("MAM_REGISTRATION", "  payload completo         : $responseBody")
                    }
                }
            )
        }
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
