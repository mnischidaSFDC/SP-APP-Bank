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

class MainActivity : AppCompatActivity() {

    private var sdkReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SDK inicializado em LoginActivity antes de chegar aqui
        sdkReady = true

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
