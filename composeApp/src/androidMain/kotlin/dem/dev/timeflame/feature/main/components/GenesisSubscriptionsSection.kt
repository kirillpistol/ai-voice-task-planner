package dem.dev.timeflame.feature.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * UI-only subscription prototype. No billing, balance or entitlement changes occur here.
 * Amounts are sample request-unit budgets, not token prices or credits actually issued.
 */
private data class GenesisTier(val name: String, val sampleRequests: Int, val description: String)

private val sampleTiers = listOf(
    GenesisTier("CORE", 100, "Basic access"),
    GenesisTier("PRO", 500, "Extended tools"),
    GenesisTier("ULTRA", 1500, "Higher request budget")
)

@Composable
private fun PistolTokenIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Render only the owner's authentic supplied PNG; never substitute a redrawn token.
    // Add the untouched file as res/drawable-nodpi/pistol_token.png to enable this asset.
    val tokenId = remember(context) {
        context.resources.getIdentifier("pistol_token", "drawable", context.packageName)
    }
    if (tokenId != 0) {
        Image(
            painter = painterResource(tokenId),
            contentDescription = "PISTOL token",
            modifier = modifier.size(25.dp)
        )
    }
}

@Composable
fun GenesisSubscriptionsSection(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf(1) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val tier = sampleTiers[selected]
    val intensity by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.7f,
        label = "genesis tier emphasis"
    )
    val surface by animateColorAsState(
        targetValue = if (expanded) Color(0xFF262A32) else Color(0xFF191C22),
        label = "genesis tier background"
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Пакеты GENESIS", style = MaterialTheme.typography.titleLarge)
        Text(
            "PISTOL · вычислительные запросы",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14161B)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PistolTokenIcon()
                    Column {
                        Text("PISTOL", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Токен расчётов · баланс появится после подключения биллинга",
                            color = Color(0xFFB9BDC6),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFF363941))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sampleTiers.forEachIndexed { index, plan ->
                        val active = selected == index
                        Card(
                            modifier = Modifier.weight(1f).clickable {
                                selected = index
                                expanded = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (active) Color(0xFF47404A) else Color(0xFF24262D)
                            ),
                            border = BorderStroke(1.dp, if (active) Color(0xFFD4AF37) else Color(0xFF41444B))
                        ) {
                            Column(Modifier.padding(vertical = 14.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(plan.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF665B38))
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp).alpha(intensity),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("GENESIS ${tier.name}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(Modifier.weight(1f))
                            PistolTokenIcon()
                        }
                        Text(tier.description, color = Color(0xFFDFDFE3))
                        Text(
                            "Пример лимита: ${tier.sampleRequests} запросов",
                            color = Color(0xFFD4AF37),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Демонстрационные значения — не цена и не начисленный баланс P",
                            color = Color(0xFFB8BAC1),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Скрыть расчёт" else "Как будут списываться запросы", color = Color(0xFFE6C45E))
                }
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Расход P = сумма (число запросов × тариф операции).", color = Color.White)
                        Text(
                            "Реальный курс P, лимиты, серверный журнал операций и подтверждение оплаты подключаются отдельно. Демо не списывает средства.",
                            color = Color(0xFFC6C7CC),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
