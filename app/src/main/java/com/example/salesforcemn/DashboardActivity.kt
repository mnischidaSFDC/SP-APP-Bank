package com.example.salesforcemn

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.salesforcemn.data.Account
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager

class DashboardActivity : AppCompatActivity() {

    var selectedAccount: Account? = null
        private set

    private var isValuesMasked = false
    private var currentFragment: Fragment? = null
    private var userName = ""
    private var accounts: ArrayList<Account> = arrayListOf()

    private val switchAccountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val newAccount = result.data?.getSerializableExtra("selectedAccount") as? Account
            val newUserName = result.data?.getStringExtra("userName") ?: userName
            if (newAccount != null) {
                selectedAccount = newAccount
                userName = newUserName
                setupAccountTheme(userName)
                selectTab(R.id.nav_home)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        @Suppress("DEPRECATION")
        selectedAccount = intent.getSerializableExtra("selectedAccount") as? Account
        userName = intent.getStringExtra("userName") ?: ""
        @Suppress("UNCHECKED_CAST")
        accounts = (intent.getSerializableExtra("accounts") as? ArrayList<Account>) ?: arrayListOf()

        setupAccountTheme(userName)
        setupHeaderControls()
        setupBottomNavigation()

        // Load default tab (Home)
        if (savedInstanceState == null) {
            selectTab(R.id.nav_home)
        }
    }

    private fun setupAccountTheme(userName: String = "") {
        val account = selectedAccount ?: return

        val llHeaderBar = findViewById<LinearLayout>(R.id.llHeaderBar)
        val tvWelcomeLabel = findViewById<TextView>(R.id.tvWelcomeLabel)
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvAvatarInitials = findViewById<TextView>(R.id.tvAvatarInitials)
        val flAvatarCircle = findViewById<FrameLayout>(R.id.flAvatarCircle)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (account.accountType == "PJ") {
            // Aplica o tema escuro/médio para conta PJ
            llHeaderBar?.setBackgroundColor(Color.parseColor("#0F172A"))
            tvWelcomeLabel?.text = "Conta Pessoa Jurídica • C/C ${account.accountNumber}"
            
            val companyName = account.companyDetails?.tradeName 
                ?: account.companyDetails?.companyName 
                ?: "Empresa PJ"
            
            tvGreeting?.text = companyName
            
            // Iniciais da empresa
            val initials = companyName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
            tvAvatarInitials?.text = if (initials.isNotEmpty()) initials else "PJ"
            
            flAvatarCircle?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0284C7"))
            bottomNav?.setBackgroundColor(Color.parseColor("#0F172A"))
        } else {
            // Tema padrão para conta PF
            llHeaderBar?.setBackgroundColor(Color.parseColor("#312E81"))
            tvWelcomeLabel?.text = "Conta Pessoa Física • C/C ${account.accountNumber}"
            tvGreeting?.text = if (userName.isNotBlank()) "Olá, $userName" else "Olá"
            val initials = userName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
            tvAvatarInitials?.text = if (initials.isNotEmpty()) initials else "PF"
            flAvatarCircle?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6366F1"))
        }
    }

    fun selectTab(tabId: Int) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = tabId
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_fatura -> {
                    replaceFragment(FaturaFragment())
                    true
                }
                R.id.nav_produtos -> {
                    replaceFragment(ProdutosFragment())
                    true
                }
                R.id.nav_extrato -> {
                    replaceFragment(ExtratoFragment())
                    true
                }
                R.id.nav_lowcode -> {
                    replaceFragment(TestesFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.flFragmentContainer, fragment)
            .commit()

        // Sync values masked state
        applyMaskedStateToFragment(fragment)
    }

    private fun setupHeaderControls() {
        val btnSwitchAccount = findViewById<ImageButton>(R.id.btnSwitchAccount)
        val btnToggleValues = findViewById<ImageButton>(R.id.btnToggleValues)
        val btnNotifications = findViewById<ImageButton>(R.id.btnNotifications)

        btnSwitchAccount.setOnClickListener {
            val intent = Intent(this, AccountSelectionActivity::class.java).apply {
                putExtra("accounts", accounts)
                putExtra("userName", userName)
                putExtra("switchMode", true)
            }
            switchAccountLauncher.launch(intent)
        }

        btnToggleValues.setOnClickListener {
            isValuesMasked = !isValuesMasked
            btnToggleValues.setImageResource(if (isValuesMasked) R.drawable.ic_eye_off else R.drawable.ic_eye)

            currentFragment?.let { applyMaskedStateToFragment(it) }

            // Track Custom Event on Value Toggle
            EventManager.customEvent(
                name = "custom",
                attributes = mapOf(
                    "interactionName" to "Toggle_Values_Mask",
                    "interactionType" to "userAction",
                    "isMasked" to isValuesMasked
                )
            )?.track()

            val msg = if (isValuesMasked) "Valores ocultados" else "Valores visíveis"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnNotifications.setOnClickListener {
            // Track Custom Event on Notifications Click
            EventManager.customEvent(
                name = "custom",
                attributes = mapOf(
                    "interactionName" to "Notification_Click",
                    "interactionType" to "userAction"
                )
            )?.track()

            Toast.makeText(this, "Você não tem novas notificações.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyMaskedStateToFragment(fragment: Fragment) {
        when (fragment) {
            is HomeFragment -> fragment.setValuesMasked(isValuesMasked)
            is FaturaFragment -> fragment.setValuesMasked(isValuesMasked)
            is ExtratoFragment -> fragment.setValuesMasked(isValuesMasked)
        }
    }
}
