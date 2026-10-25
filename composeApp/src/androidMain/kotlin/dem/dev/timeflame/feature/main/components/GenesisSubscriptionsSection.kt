package dem.dev.timeflame.feature.main.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.R

/** Display-only catalogue: never grants access, creates balances, or charges the user. */
private data class GenesisTier(
    val name: String,
    val sampleRequests: Int,
    val subtitle: String,
    val features: List<String>
)

private val demoTiers = listOf(
    GenesisTier("CORE", 100, "Базовые инструменты", listOf("Основные запросы", "Личное пространство")),
    GenesisTier("PRO", 500, "Расширенные возможности", listOf("Возможности CORE", "Дополнительные инструменты", "Расширенная квота")),
    GenesisTier("ULTRA", 1500, "Максимальный доступ", listOf("Возможности PRO", "Приоритетные функции", "Увеличенная квота"))
)

private val graphite = Color(0xFF111318)
private val secondaryText = Color(0xFFB4B8C2)
private val gold = Color(0xFFD5B65E)

@Composable
private fun PistolTokenIcon(modifier: Modifier = Modifier, large: Boolean = false) {
    // This is the customer's original raster artwork: never redraw or recolor it.
    val context = LocalContext.current
    val resourceId = remember(context, large) {
        val resourceName = if (large) "pistol_token" else "pistol_token_micro"
        val requested = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        if (requested != 0) requested
        else context.resources.getIdentifier("pistol_token", "drawable", context.packageName)
    }
    if (resourceId == 0) return
    Image(
        painter = painterResource(resourceId),
        contentDescription = "Оригинальный токен PISTOL",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(if (large) 54.dp else 24.dp)
    )
}

@Composable
fun GenesisSubscriptionsSection(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf(1) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val tier = demoTiers[selected]
    val accent by animateColorAsState(
        if (selected == 2) Color(0xFFE8D2A0) else if (selected == 1) gold else Color(0xFFBEC4CD),
        animationSpec = tween(380), label = "tier accent"
    )
    val selectionGlow by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.75f,
        animationSpec = tween(450), label = "membership glow"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ПАКЕТЫ GENESIS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("ПОДПИСКИ / ЗАПРОСЫ / PISTOL", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("G", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
        }
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = graphite)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(66.dp).background(Color(0xFF25272E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PistolTokenIcon(large = true)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("PISTOL", color = Color.White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text("Расчётный токен GENESIS", color = secondaryText,
                            style = MaterialTheme.typography.bodySmall)
                        Text("Баланс: подключение биллинга", color = gold,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                HorizontalDivider(color = Color(0xFF373A43))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    demoTiers.forEachIndexed { index, plan ->
                        val active = selected == index
                        Box(
                            modifier = Modifier.weight(1f)
                                .background(if (active) Color(0xFF383A40) else Color(0xFF22252B),
                                    RoundedCornerShape(12.dp))
                                .clickable {
                                    selected = index
                                    expanded = false
                                }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(plan.name, textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (active) gold else secondaryText)
                        }
                    }
                }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = selectionGlow)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))
                ) {
                    Column(Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF292D35), Color(0xFF17191E))))
                        .padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("GENESIS / MEMBERSHIP", color = secondaryText,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f))
                            PistolTokenIcon()
                        }
                        AnimatedContent(targetState = selected, label = "tier change") { position ->
                            val current = demoTiers[position]
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(current.name, color = Color.White, fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.headlineMedium)
                                Text(current.subtitle, color = secondaryText,
                                    style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PistolTokenIcon()
                                    Text("${current.sampleRequests} запросов · пример квоты",
                                        color = accent, style = MaterialTheme.typography.bodyMedium)
                                }
                                current.features.forEach { feature ->
                                    Text("• $feature", color = Color(0xFFE4E5E8),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF3C4149))
                        Text("Концепт тарифа. Цена в P и реальные лимиты пока не утверждены.",
                            color = secondaryText, style = MaterialTheme.typography.labelSmall)
                    }
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Скрыть модель списаний" else "Как работает расчёт запросов",
                        color = gold)
                }
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Расход P = Σ (число запросов × цена класса запроса)",
                            color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text("Запросы и балансы сейчас не списываются. Подтверждённые сервером операции, платежи и история появятся на этапе биллинга.",
                            color = secondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}
