package com.example.salesforcemn

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.salesforce.personalization.PersonalizationSdk
import com.salesforce.personalization.http.DecisionsRequestContext
import com.salesforce.personalization.http.DecisionsResponsePersonalization
import com.salesforce.personalization.utils.Result
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private val KNOWN_POINTS = listOf("banner_home_pf", "home_banner_pj", "Low_Code")

@Composable
internal fun DecisioningApiScreen() {
    var selectedPoint by remember { mutableStateOf("") }
    var customInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var requestId by remember { mutableStateOf<String?>(null) }
    var personalization by remember { mutableStateOf<DecisionsResponsePersonalization?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun fetchPoint(pointName: String) {
        if (pointName.isBlank()) return
        selectedPoint = pointName
        isLoading = true
        personalization = null
        requestId = null
        errorMsg = null
        scope.launch {
            when (val result = PersonalizationSdk.fetchDecisions(
                personalizationPointNames = listOf(pointName),
                decisionsRequestContext = DecisionsRequestContext.Builder().build()
            )) {
                is Result.Success -> {
                    requestId = result.output.requestId
                    personalization = result.output.personalizationsByName[pointName]
                    if (personalization == null) {
                        errorMsg = "Nenhuma personalização retornada para \"$pointName\""
                    }
                }
                is Result.Failure -> {
                    errorMsg = result.throwable.message ?: "Erro ao buscar decisão"
                }
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Decisioning API",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Consulte personalizações diretamente via SDK",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Personalization Points",
                color = Color(0xFF334155),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KNOWN_POINTS.forEach { point ->
                    FilterChip(
                        selected = selectedPoint == point && !isLoading,
                        onClick = { fetchPoint(point) },
                        label = { Text(point, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    label = { Text("Point personalizado") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
                )
                Button(
                    onClick = { fetchPoint(customInput.trim()) },
                    enabled = customInput.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("Buscar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                }
            }

            if (errorMsg != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Erro: $errorMsg",
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (!isLoading && personalization != null) {
                // Rendered preview for banner_home_pf
                if (selectedPoint == "banner_home_pf") {
                    SectionChip("Renderização — banner_home_pf")
                    Spacer(modifier = Modifier.height(6.dp))
                    BannerHomePfCard(personalization = personalization!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SectionChip("Resposta bruta")
                Spacer(modifier = Modifier.height(6.dp))
                DecisionResultCard(requestId = requestId, personalization = personalization!!)
            }
        }
    }
}

// Horizontal card layout — distinct from the vertical full-width Home banner.
// Home: dark purple bg, full-width top image, vertical, close button.
// Here: white card, gradient top stripe, horizontal thumbnail + content, TextButton CTA.
@Composable
private fun BannerHomePfCard(personalization: DecisionsResponsePersonalization) {
    fun field(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        val fromData = (personalization.data.firstOrNull()?.get(key) as? JsonPrimitive)
            ?.contentOrNull?.takeIf { it.isNotBlank() }
        val fromAttrs = (personalization.attributes[key] as? JsonPrimitive)
            ?.contentOrNull?.takeIf { it.isNotBlank() }
        fromData ?: fromAttrs
    }

    val title    = field("Header", "title", "Title", "headline") ?: return
    val subtitle = field("Subheader", "subtitle", "Subtitle", "description", "body")
    val ctaLabel = field("CallToActionText", "ctaLabel", "cta_label", "buttonText")
    val ctaUrl   = field("CallToActionUrl", "ctaUrl", "cta_url", "url", "link")
    val imageUrl = field("BackgroundImageUrl", "imageUrl", "image_url", "bannerImage")
    val badge    = field("badge", "Badge", "label", "tag")
    val ctx = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Gradient accent stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF0EA5E9))
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Thumbnail — left side
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Monogram placeholder using first letter of title
                        Text(
                            text = title.first().uppercaseChar().toString(),
                            color = Color(0xFF6366F1),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Content — right side
                Column(modifier = Modifier.weight(1f)) {
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = badge.uppercase(),
                                color = Color(0xFF6366F1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = title,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (ctaLabel != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                ctaUrl?.let {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                                }
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "$ctaLabel →",
                                color = Color(0xFF6366F1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Point name footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "banner_home_pf  •  ${personalization.personalizationPointId.take(8)}…",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DecisionResultCard(
    requestId: String?,
    personalization: DecisionsResponsePersonalization
) {
    Column {
        if (requestId != null) {
            MetaRow(label = "Request ID", value = requestId)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = personalization.personalizationPointName,
                    color = Color(0xFF1E1B4B),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${personalization.personalizationId}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Decision: ${personalization.decisionId}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (personalization.attributes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionChip("Attributes (${personalization.attributes.size})")
                    personalization.attributes.forEach { (key, value) ->
                        val display = (value as? JsonPrimitive)?.contentOrNull ?: value.toString()
                        AttributeRow(key = key, value = display)
                    }
                }

                if (personalization.data.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionChip("Data Items (${personalization.data.size})")
                    personalization.data.forEachIndexed { i, item ->
                        Text(
                            text = "Item $i  •  ${item.personalizationContentId}",
                            color = Color(0xFF475569),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                        item.keys().forEach { key ->
                            val value = item[key]
                            val display = (value as? JsonPrimitive)?.contentOrNull ?: value.toString()
                            AttributeRow(key = "  $key", value = display)
                        }
                    }
                }

                if (personalization.attributes.isEmpty() && personalization.data.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Resposta vazia — verifique a configuração do recipe no Marketing Cloud.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFE0E7FF),
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF3730A3),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AttributeRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = key,
            color = Color(0xFF6366F1),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            color = Color(0xFF334155),
            fontSize = 12.sp,
            modifier = Modifier.weight(0.65f)
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            modifier = Modifier.weight(0.25f)
        )
        Text(
            text = value,
            color = Color(0xFF334155),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.75f)
        )
    }
}
