package com.example.salesforcemn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import coil.load
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import com.salesforce.personalization.errors.PersonalizationException
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.salesforce.marketingcloud.sfmcsdk.components.events.EventManager

import com.salesforce.personalization.PersonalizationSdk
import com.salesforce.personalization.http.DecisionsRequestContext
import com.salesforce.personalization.http.DecisionsResponse
import com.salesforce.personalization.utils.Result
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "MCN SP"
private const val POINT_HOME_BANNER_PF = "banner_home_pf"
private const val POINT_HOME_BANNER_PJ = "home_banner_pj"

class HomeFragment : Fragment() {

    private var isValuesMasked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackScreenView("Home Tela", "Login")

        setupListeners(view)
        updateMaskedValues(view)

        fetchPersonalizationBanner(view)
    }

    private fun fetchPersonalizationBanner(view: View) {
        val account = (activity as? DashboardActivity)?.selectedAccount
        val pointName = if (account?.accountType == "PJ") POINT_HOME_BANNER_PJ else POINT_HOME_BANNER_PF

        viewLifecycleOwner.lifecycleScope.launch {
            val result = PersonalizationSdk.fetchDecisions(
                personalizationPointNames = listOf(pointName),
                decisionsRequestContext = DecisionsRequestContext.Builder().build()
            )
            when (result) {
                is Result.Success -> {
                    Log.d(TAG, "Decisions fetched: ${result.output}")
                    showBanner(view, result.output, pointName)
                }
                is Result.Failure -> {
                    Log.e(TAG, "Failed to fetch decisions", result.throwable)
                }
            }
        }
    }

    fun setValuesMasked(masked: Boolean) {
        isValuesMasked = masked
        view?.let { updateMaskedValues(it) }
    }



    private fun kotlinx.serialization.json.JsonElement?.asString(): String? =
        this?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun showBanner(view: View, response: DecisionsResponse, pointName: String) {
        val personalization = response.personalizationsByName[pointName] ?: return
        val content = personalization.data.firstOrNull()

        fun field(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            content?.get(key).asString() ?: personalization.attributes[key].asString()
        }

        val title    = field("Header", "title", "Title", "headline") ?: return
        val subtitle = field("Subheader", "subtitle", "Subtitle", "description", "body")
        val ctaLabel = field("CallToActionText", "ctaLabel", "cta_label", "buttonText")
        val ctaUrl   = field("CallToActionUrl", "ctaUrl", "cta_url", "url", "link")
        val imageUrl = field("BackgroundImageUrl", "imageUrl", "image_url", "bannerImage")
        val badge    = field("badge", "Badge", "label", "tag")

        val card     = view.findViewById<MaterialCardView>(R.id.cardHomeBanner)
        val ivImage  = view.findViewById<ImageView>(R.id.ivBannerImage)
        val tvTitle  = view.findViewById<TextView>(R.id.tvBannerTitle)
        val tvSub    = view.findViewById<TextView>(R.id.tvBannerSubtitle)
        val tvBadge  = view.findViewById<TextView>(R.id.tvBannerBadge)
        val btnCta   = view.findViewById<MaterialButton>(R.id.btnBannerCta)

        tvTitle.text = title
        if (subtitle != null) tvSub.text = subtitle
        if (ctaLabel != null) btnCta.text = ctaLabel
        if (badge != null) tvBadge.text = badge

        if (imageUrl != null) {
            ivImage.visibility = View.VISIBLE
            ivImage.load(imageUrl)
        }

        btnCta.setOnClickListener {
            if (ctaUrl != null) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ctaUrl)))
            }
        }

        card.visibility = View.VISIBLE
        Log.d(TAG, "Banner exibido: $title")
    }

    private fun updateMaskedValues(view: View) {
        val tvAccountBalance = view.findViewById<TextView>(R.id.tvAccountBalance)
        val tvCardInvoice = view.findViewById<TextView>(R.id.tvCardInvoice)
        val tvAvailableLimit = view.findViewById<TextView>(R.id.tvAvailableLimit)

        val account = (activity as? DashboardActivity)?.selectedAccount
        @Suppress("DEPRECATION")
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        val balanceText = currencyFormat.format(account?.balance ?: 12450.80)
        val invoiceText = currencyFormat.format(account?.cardInvoice ?: 2840.50)
        val limitText = currencyFormat.format(account?.availableLimit ?: 7159.50)

        if (isValuesMasked) {
            tvAccountBalance?.text = "••••••"
            tvCardInvoice?.text = "••••••"
            tvAvailableLimit?.text = "Limite disponível: ••••••"
        } else {
            tvAccountBalance?.text = balanceText
            tvCardInvoice?.text = invoiceText
            tvAvailableLimit?.text = "Limite disponível: $limitText"
        }
    }

    private fun setupListeners(view: View) {
        view.findViewById<ImageButton>(R.id.btnCloseBanner)?.setOnClickListener {
            view.findViewById<MaterialCardView>(R.id.cardHomeBanner)?.visibility = View.GONE
        }

        view.findViewById<MaterialButton>(R.id.btnHomeVerExtrato)?.setOnClickListener {
            (activity as? DashboardActivity)?.selectTab(R.id.nav_extrato)
        }

        view.findViewById<MaterialButton>(R.id.btnHomeVerFatura)?.setOnClickListener {
            (activity as? DashboardActivity)?.selectTab(R.id.nav_fatura)
        }

        view.findViewById<View>(R.id.btnActionPix)?.setOnClickListener {
            trackQuickAction("Pix")
            Snackbar.make(view, "Ação rápida Pix selecionada", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnActionBoleto)?.setOnClickListener {
            trackQuickAction("Pagar Boleto")
            Snackbar.make(view, "Ação rápida Pagar Boleto selecionada", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnActionTransfer)?.setOnClickListener {
            trackQuickAction("Transferir")
            Snackbar.make(view, "Ação rápida Transferir selecionada", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnActionRecarga)?.setOnClickListener {
            trackQuickAction("Recarga")
            Snackbar.make(view, "Ação rápida Recarga selecionada", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnActionEmprestimo)?.setOnClickListener {
            trackQuickAction("Empréstimo")
            Snackbar.make(view, "Ação rápida Empréstimo selecionada", Snackbar.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnActionBannerScreen)?.setOnClickListener {
            val intent = Intent(requireContext(), BannerActivity::class.java).apply {
                putExtra("previousScreen", "Home Tela")
            }
            startActivity(intent)
        }
    }

    private fun trackQuickAction(actionName: String) {
        EventManager.customEvent(
            name = "custom",
            attributes = mapOf(
                "interactionName" to "QuickAction_$actionName",
                "interactionType" to "userAction",
                "screenName" to "Home Tela"
            )
        )?.track()
        Log.d("HomeFragment", "Quick Action tracked: $actionName")
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
        Log.d("HomeFragment", "Custom Event tracked: $screenName")
    }
}
