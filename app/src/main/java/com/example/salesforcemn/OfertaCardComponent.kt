package com.example.salesforcemn

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.salesforce.personalization.errors.PersonalizationException
import com.salesforce.personalization.lowcodemobile.Component
import com.salesforce.personalization.lowcodemobile.ComponentContext
import com.salesforce.personalization.lowcodemobile.ComponentModel
import com.salesforce.personalization.lowcodemobile.EngagementAction
import com.salesforce.personalization.utils.Result
import org.json.JSONException
import org.json.JSONObject
import kotlin.reflect.KClass

// Campos mapeados no recipe do Marketing Cloud para o componente "custom_oferta_card":
//   title     (String, obrigatório) – Título principal do card
//   subtitle  (String, opcional)   – Descrição ou subtítulo
//   imageUrl  (String, opcional)   – URL da imagem de capa
//   ctaLabel  (String, opcional)   – Texto do botão de ação
//   ctaUrl    (String, opcional)   – URL de destino ao clicar no botão
//   badgeText (String, opcional)   – Texto do badge/etiqueta

data class OfertaCardModel(
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val ctaLabel: String?,
    val ctaUrl: String?,
    val badgeText: String?
) : ComponentModel

class OfertaCardComponent : Component<OfertaCardModel> {

    override val name: String = COMPONENT_NAME

    override val modelClass: KClass<OfertaCardModel> = OfertaCardModel::class

    override fun validateAndCreateComponentModel(
        json: String,
        context: ComponentContext
    ): Result<OfertaCardModel, PersonalizationException.ContentZoneUnableToRender> {
        return try {
            val obj = JSONObject(json)
            val title = obj.optString("title").takeIf { it.isNotBlank() }
                ?: return Result.Failure(
                    PersonalizationException.ContentZoneUnableToRender(
                        "INVALID_COMPONENT_DATA",
                        "Campo obrigatório 'title' ausente no payload do componente",
                        null
                    )
                )
            Result.Success(
                OfertaCardModel(
                    title = title,
                    subtitle = obj.optString("subtitle").takeIf { it.isNotBlank() },
                    imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() },
                    ctaLabel = obj.optString("ctaLabel").takeIf { it.isNotBlank() },
                    ctaUrl = obj.optString("ctaUrl").takeIf { it.isNotBlank() },
                    badgeText = obj.optString("badgeText").takeIf { it.isNotBlank() }
                )
            )
        } catch (e: JSONException) {
            Result.Failure(
                PersonalizationException.ContentZoneUnableToRender(
                    "JSON_PARSE_ERROR",
                    e.message ?: "Erro ao parsear JSON do componente",
                    e
                )
            )
        }
    }

    @Composable
    override fun Compose(model: OfertaCardModel, context: ComponentContext) {
        LaunchedEffect(Unit) {
            context.trackEngagement(EngagementAction.VIEW)
        }
        OfertaCardView(model = model, onCtaClick = {
            context.trackEngagement(EngagementAction.CLICK)
        })
    }

    companion object {
        const val COMPONENT_NAME = "custom_oferta_card"
    }
}

@Composable
fun OfertaCardView(model: OfertaCardModel, onCtaClick: () -> Unit = {}) {
    val ctx = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (model.imageUrl != null) {
                AsyncImage(
                    model = model.imageUrl,
                    contentDescription = model.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(20.dp)) {
                if (model.badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF6366F1),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = model.badgeText.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = model.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (model.subtitle != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = model.subtitle,
                        color = Color(0xFFC7D2FE),
                        fontSize = 13.sp
                    )
                }
                if (model.ctaLabel != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onCtaClick()
                            model.ctaUrl?.let { url ->
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = model.ctaLabel,
                            color = Color(0xFF312E81),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
