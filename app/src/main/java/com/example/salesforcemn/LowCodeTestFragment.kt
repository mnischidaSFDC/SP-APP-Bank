package com.example.salesforcemn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesforce.personalization.PersonalizationSdk
import com.salesforce.personalization.http.DecisionsRequestContext
import com.salesforce.personalization.lowcodemobile.ContentZoneController
import com.salesforce.personalization.lowcodemobile.MockDataContentZone
import com.salesforce.personalization.utils.Result
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

// Content Zone a criar no Marketing Cloud:  mn_lowcode_test
// Componente custom a mapear no recipe:      custom_oferta_card
// Personalization Point name (no MC):        Low_Code
private const val CONTENT_ZONE_NAME = "mn_lowcode_test"
private const val POINT_NAME = "Low_Code"

@Composable
internal fun LowCodeTab() {
    var liveModel by remember { mutableStateOf<OfertaCardModel?>(null) }
    var liveLoading by remember { mutableStateOf(true) }
    var liveError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = PersonalizationSdk.fetchDecisions(
            personalizationPointNames = listOf(POINT_NAME),
            decisionsRequestContext = DecisionsRequestContext.Builder().build()
        )
        when (result) {
            is Result.Success -> {
                val attrs = result.output.personalizationsByName[POINT_NAME]?.attributes
                val title = attrs?.get("title")?.jsonPrimitive?.contentOrNull
                if (title != null) {
                    liveModel = OfertaCardModel(
                        title = title,
                        subtitle = attrs["subtitle"]?.jsonPrimitive?.contentOrNull,
                        imageUrl = attrs["imageUrl"]?.jsonPrimitive?.contentOrNull,
                        ctaLabel = attrs["ctaLabel"]?.jsonPrimitive?.contentOrNull,
                        ctaUrl = attrs["ctaUrl"]?.jsonPrimitive?.contentOrNull,
                        badgeText = attrs["badgeText"]?.jsonPrimitive?.contentOrNull
                    )
                } else {
                    liveError = true
                }
            }
            is Result.Failure -> liveError = true
        }
        liveLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E1B4B)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Low-Code Personalization",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Zone: $CONTENT_ZONE_NAME  •  Point: $POINT_NAME",
                    color = Color(0xFFA5B4FC),
                    fontSize = 12.sp
                )
                Text(
                    text = "Componente: ${OfertaCardComponent.COMPONENT_NAME}",
                    color = Color(0xFFA5B4FC),
                    fontSize = 12.sp
                )
            }
        }

        SectionHeader(title = "Live (Marketing Cloud — attributes)")

        when {
            liveLoading -> ZoneLoading()
            liveError || liveModel == null -> ZoneError()
            else -> OfertaCardView(model = liveModel!!)
        }

        SectionHeader(title = "Preview (Dados de Teste)")

        MockDataContentZone(
            name = CONTENT_ZONE_NAME,
            allowedComponents = listOf(OfertaCardComponent()),
            mockContent = Result.Success(
                OfertaCardModel(
                    title = "Oferta Exclusiva — Preview",
                    subtitle = "Componente custom_oferta_card renderizado com dados locais de teste.",
                    imageUrl = null,
                    ctaLabel = "Ver Oferta",
                    ctaUrl = null,
                    badgeText = "TESTE"
                )
            ),
            mockLoadingMs = 0L,
            controller = ContentZoneController(),
            loading = { ZoneLoading() },
            fallback = { _ -> ZoneError() }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE0E7FF)
    ) {
        Text(
            text = title,
            color = Color(0xFF3730A3),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun ZoneLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF6366F1))
    }
}

@Composable
internal fun ZoneError() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sem conteúdo disponível",
                color = Color(0xFF1E1B4B),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Verifique se o Personalization Point \"$POINT_NAME\" está configurado no Marketing Cloud com os campos do componente \"${OfertaCardComponent.COMPONENT_NAME}\".",
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
