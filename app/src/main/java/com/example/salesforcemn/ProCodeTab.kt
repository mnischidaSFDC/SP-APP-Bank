package com.example.salesforcemn

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.salesforce.personalization.PersonalizationSdk
import com.salesforce.personalization.http.DecisionsRequestContext
import com.salesforce.personalization.utils.Result
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val PRO_CODE_POINT = "banner_home_pf"

private data class ProCodeBannerData(
    val title: String,
    val subtitle: String?,
    val ctaLabel: String?,
    val ctaUrl: String?,
    val imageUrl: String?,
    val badge: String?
)

@Composable
internal fun ProCodeTab() {
    var bannerData by remember { mutableStateOf<ProCodeBannerData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = PersonalizationSdk.fetchDecisions(
            personalizationPointNames = listOf(PRO_CODE_POINT),
            decisionsRequestContext = DecisionsRequestContext.Builder().build()
        )
        when (result) {
            is Result.Success -> {
                val p = result.output.personalizationsByName[PRO_CODE_POINT]
                if (p != null) {
                    fun field(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
                        val fromData = (p.data.firstOrNull()?.get(key) as? JsonPrimitive)
                            ?.contentOrNull?.takeIf { it.isNotBlank() }
                        val fromAttrs = (p.attributes[key] as? JsonPrimitive)
                            ?.contentOrNull?.takeIf { it.isNotBlank() }
                        fromData ?: fromAttrs
                    }
                    val title = field("Header", "title", "Title", "headline")
                    if (title != null) {
                        bannerData = ProCodeBannerData(
                            title = title,
                            subtitle = field("Subheader", "subtitle", "Subtitle", "description", "body"),
                            ctaLabel = field("CallToActionText", "ctaLabel", "cta_label", "buttonText"),
                            ctaUrl = field("CallToActionUrl", "ctaUrl", "cta_url", "url", "link"),
                            imageUrl = field("BackgroundImageUrl", "imageUrl", "image_url", "bannerImage"),
                            badge = field("badge", "Badge", "label", "tag")
                        )
                    } else {
                        hasError = true
                    }
                } else {
                    hasError = true
                }
            }
            is Result.Failure -> hasError = true
        }
        isLoading = false
    }

    AndroidView(
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.fragment_procode_test, null, false)
        },
        update = { view ->
            val pbLoading = view.findViewById<View>(R.id.pbProCodeLoading)
            val cardBanner = view.findViewById<MaterialCardView>(R.id.cardProCodeBanner)
            val cardError = view.findViewById<MaterialCardView>(R.id.cardProCodeError)

            when {
                isLoading -> {
                    pbLoading.visibility = View.VISIBLE
                    cardBanner.visibility = View.GONE
                    cardError.visibility = View.GONE
                }
                hasError || bannerData == null -> {
                    pbLoading.visibility = View.GONE
                    cardBanner.visibility = View.GONE
                    cardError.visibility = View.VISIBLE
                }
                else -> {
                    pbLoading.visibility = View.GONE
                    cardError.visibility = View.GONE
                    populateProCodeBanner(view, bannerData!!)
                    cardBanner.visibility = View.VISIBLE
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun populateProCodeBanner(view: View, data: ProCodeBannerData) {
    val cardBanner = view.findViewById<MaterialCardView>(R.id.cardProCodeBanner)
    val ivImage    = view.findViewById<ImageView>(R.id.ivProCodeBannerImage)
    val tvTitle    = view.findViewById<TextView>(R.id.tvProCodeBannerTitle)
    val tvSub      = view.findViewById<TextView>(R.id.tvProCodeBannerSubtitle)
    val tvBadge    = view.findViewById<TextView>(R.id.tvProCodeBannerBadge)
    val btnCta     = view.findViewById<MaterialButton>(R.id.btnProCodeBannerCta)
    val btnClose   = view.findViewById<ImageButton>(R.id.btnProCodeCloseBanner)

    tvTitle.text = data.title
    if (data.subtitle != null) tvSub.text = data.subtitle
    if (data.ctaLabel != null) btnCta.text = data.ctaLabel
    if (data.badge != null) tvBadge.text = data.badge

    if (data.imageUrl != null) {
        ivImage.visibility = View.VISIBLE
        ivImage.load(data.imageUrl)
    }

    btnCta.setOnClickListener {
        data.ctaUrl?.let { url ->
            view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    btnClose.setOnClickListener {
        cardBanner.visibility = View.GONE
    }
}
