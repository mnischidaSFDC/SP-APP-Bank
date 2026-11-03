package com.example.salesforcemn

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager

class BannerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banner)

        val tvEventStatus = findViewById<TextView>(R.id.tvEventStatus)
        val btnTrackAgain = findViewById<MaterialButton>(R.id.btnTrackAgain)
        val lastScreen = intent.getStringExtra("previousScreen") ?: "MainActivity"

        if (savedInstanceState == null) {
            sendBannerEvent(lastScreen, tvEventStatus)
        }
        btnTrackAgain.setOnClickListener { sendBannerEvent(lastScreen, tvEventStatus) }
    }

    private fun sendBannerEvent(lastScreen: String, statusView: TextView) {
        val event = EventManager.customEvent(
            name = "custom",
            attributes = mapOf(
                "interactionName" to "Banner Tela",
                "interactionType" to "screenView",
                "screenName" to "Banner Tela",
                "previousScreen" to lastScreen
            )
        )

        if (event != null) {
            event.track()
            Log.d("BannerActivity", "Custom Event 'Banner Tela' tracked successfully.")
            statusView.text = "Custom Event enviado para o Data Cloud!"
            Snackbar.make(statusView, "Evento 'Banner Tela' enviado!", Snackbar.LENGTH_SHORT).show()
        } else {
            Log.e("BannerActivity", "Failed to create custom event.")
            statusView.text = "Erro ao criar o evento customizado."
        }
    }
}
