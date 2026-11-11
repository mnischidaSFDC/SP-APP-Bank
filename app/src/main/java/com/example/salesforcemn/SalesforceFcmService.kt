package com.example.salesforcemn

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.salesforce.marketingcloud.pushfeature.PushFeature

class SalesforceFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("SalesforceFcmService", "Token FCM atualizado")
        PushFeature.requestSdk { push ->
            push.getPushMessageManager().setPushToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        PushFeature.requestSdk { push ->
            push.getPushMessageManager().handleMessage(message)
        }
    }
}
