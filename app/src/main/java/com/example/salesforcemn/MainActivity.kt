package com.example.salesforcemn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar

import com.salesforce.marketingcloud.cdp.CdpConfig
import com.salesforce.marketingcloud.cdp.CdpSdk
import com.salesforce.marketingcloud.cdp.consent.Consent
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdk
import com.salesforce.marketingcloud.sfmcsdk.SFMCSdkModuleConfig
import com.salesforce.marketingcloud.sfmcsdk.InitializationStatus

import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogLevel
import com.salesforce.marketingcloud.sfmcsdk.components.logging.LogListener.AndroidLogger

class MainActivity : AppCompatActivity() {

    private var sdkReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val appId = "e4fc0f7f-11f7-4b70-ac2e-0dd4d79c0b04"
        val endpoint = "https://gqyt0ntgg5tg0mrsgztgcmjxmq.c360a.salesforce.com"

        SFMCSdk.setLogging(LogLevel.DEBUG, AndroidLogger())

        val cdpModuleConfig = CdpConfig.Builder(this@MainActivity, appId, endpoint)
            .trackScreens(true)
            .trackLifecycle(true)
            .sessionTimeout(600)
            .eventFlushRate(CdpConfig.EventFlushRate.Quantity(1))
            .build()

        SFMCSdk.configure(this, SFMCSdkModuleConfig.build {
            this.cdpModuleConfig = cdpModuleConfig
        }) { status ->
            if (status.status == InitializationStatus.SUCCESS) {
                CdpSdk.requestSdk { sdk ->
                    sdk.consent = Consent.OPT_IN
                }
                sdkReady = true
            }
        }

        val btnUpdate = findViewById<MaterialButton>(R.id.btnUpdateIdentity)
        val btnOpenBanner = findViewById<MaterialButton>(R.id.btnOpenBanner)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        btnOpenBanner.setOnClickListener {
            val intent = Intent(this, BannerActivity::class.java).apply {
                putExtra("previousScreen", "MainActivity")
            }
            startActivity(intent)
        }

        btnUpdate.setOnClickListener {
            val firstName = findViewById<TextInputEditText>(R.id.etFirstName).text?.toString().orEmpty()
            val lastName = findViewById<TextInputEditText>(R.id.etLastName).text?.toString().orEmpty()
            val email = findViewById<TextInputEditText>(R.id.etEmail).text?.toString().orEmpty()
            val phoneNumber = findViewById<TextInputEditText>(R.id.etPhoneNumber).text?.toString().orEmpty()
            val contactKey = findViewById<TextInputEditText>(R.id.etContactKey).text?.toString().orEmpty()

            if (!sdkReady) {
                Snackbar.make(it, "SDK not ready yet, try again.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            tvStatus.text = "Identity updated!"
            tvStatus.visibility = View.VISIBLE
            Log.d("MainActivity", "Identity updated: contactKey=$contactKey $firstName $lastName / $email / $phoneNumber")
        }
    }
}
