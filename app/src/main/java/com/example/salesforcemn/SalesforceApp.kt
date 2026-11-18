package com.example.salesforcemn

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.salesforce.marketingcloud.cdp.CdpConfig
import com.salesforce.marketingcloud.cdp.CdpSdk
import com.salesforce.marketingcloud.cdp.consent.Consent
import com.salesforce.marketingcloud.mobileappmessaging.MobileAppMessaging
import com.salesforce.marketingcloud.mobileappmessaging.MobileAppMessagingConfig
import com.salesforce.marketingcloud.mobileappmessaging.registration.RegistrationManager
import com.salesforce.marketingcloud.pushfeature.PushFeature
import com.salesforce.marketingcloud.pushfeature.config.PushFeatureConfig
import com.salesforce.marketingcloud.pushfeature.notifications.NotificationCustomizationOptions
import com.salesforce.marketingcloud.sfmcsdk.InitializationStatus
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener.AndroidLogger
import com.salesforce.personalization.PersonalizationConfig

/**
 * Inicializa o SFMCSdk sempre que o processo sobe — inclusive quando o FCM acorda o
 * SalesforceFcmService com o app fechado ou quando o Android restaura uma Activity
 * (ex.: Dashboard) após matar o processo.
 */
class SalesforceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initSalesforceSdk()
    }

    private fun initSalesforceSdk() {
        SFMCSdk.setLogging(LogLevel.DEBUG, AndroidLogger())

        val cdpModuleConfig = CdpConfig.Builder(this, BuildConfig.SFMC_CDP_APP_ID, BuildConfig.SFMC_CDP_ENDPOINT)
            .trackScreens(true)
            .trackLifecycle(true)
            .sessionTimeout(600)
            .eventFlushRate(CdpConfig.EventFlushRate.Quantity(1))
            .build()

        val personalizationConfig = PersonalizationConfig.Builder(this)
            .cdnUrl(BuildConfig.SFMC_PERSONALIZATION_CDN_URL)
            .build()

        val mamConfig = MobileAppMessagingConfig.builder()
            .moduleApplicationId(BuildConfig.SFMC_MAM_MODULE_APP_ID)
            .tenantId(BuildConfig.SFMC_MAM_TENANT_ID)
            .accessToken(BuildConfig.SFMC_MAM_ACCESS_TOKEN)
            .endpointUrl(BuildConfig.SFMC_MAM_ENDPOINT_URL)
            .build()

        val pushConfig = PushFeatureConfig.builder()
            .setSenderId(BuildConfig.FCM_SENDER_ID)
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
            Log.d("SalesforceApp", "SFMCSdk status: ${status.status}")

            // CDP consent é independente do MAM/Push — não bloqueia Data Cloud se outro módulo falhar
            CdpSdk.requestSdk { cdp ->
                cdp.consent = Consent.OPT_IN
                Log.d("SalesforceApp", "CDP Consent.OPT_IN confirmado")
            }

            if (status.status == InitializationStatus.SUCCESS) {
                Log.d("SalesforceApp", "SFMCSdk configurado com sucesso (todos os módulos)")
                registerMamListener()
                registerFcmToken()
            } else {
                Log.w("SalesforceApp", "SFMCSdk status não é SUCCESS: ${status.status} — MAM ou Push pode estar falhando")
            }
        }
    }

    // onNewToken pode não disparar se o token já existe: registra o atual a cada start
    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                PushFeature.requestSdk { push ->
                    push.getPushMessageManager().setPushToken(token)
                    Log.d("SalesforceApp", "Token FCM registrado no PushFeature")
                }
            } else {
                Log.e("SalesforceApp", "Falha ao obter FCM token", task.exception)
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
                        Log.d("MAM_REGISTRATION", "Registro MAM recebido — partyIdentificationNumber: " +
                            responseBody.optString("partyIdentificationNumber") +
                            " | deviceId: " + responseBody.optString("deviceId"))
                    }
                }
            )
        }
    }
}
