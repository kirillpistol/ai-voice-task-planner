package dem.dev.timeflame.feature.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PreviewPlan(val id: String, val title: String, val description: String, val benefits: List<String>)
private val previewPlans = listOf(
    PreviewPlan("free", "FREE", "Personal basics", listOf("Calendar", "Task planning")),
    PreviewPlan("plus", "PLUS", "Expanded workspace", listOf("Genesis insights", "More integrations")),
    PreviewPlan("pro", "PRO", "Professional workspace", listOf("Genesis insights", "Team workflows", "Priority support"))
)

/** Preview only. Payments and entitlements remain disabled until a verified billing provider is connected. */
@Composable
fun BillingScreen(modifier: Modifier = Modifier, onBack: () -> Unit = {}) {
    var selected by remember { mutableStateOf("plus") }
    var detailsExpanded by remember { mutableStateOf(true) }
    val colors = MaterialTheme.colorScheme
    val background = colors.surface
    val foreground = colors.onSurface
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Back", color = foreground, modifier = Modifier.clickable(onClick = onBack))
        Text("PISTOL GENESIS", color = colors.onSurfaceVariant, fontSize = 12.sp, letterSpacing = 3.sp)
        Text("YOUR PLAN", color = foreground, fontSize = 36.sp, fontWeight = FontWeight.Black)
        Text("Explore the available plans", color = colors.onSurfaceVariant, fontSize = 13.sp)
        previewPlans.forEach { plan ->
            val active = plan.id == selected
            val border by animateColorAsState(if (active) colors.primary else colors.outline, label = "planBorder")
            val emphasis by animateFloatAsState(if (active) 1f else .72f, tween(250), label = "planEmphasis")
            Card(
                modifier = Modifier.fillMaxWidth().alpha(emphasis).clickable { selected = plan.id; detailsExpanded = true },
                colors = CardDefaults.cardColors(containerColor = background),
                border = BorderStroke(if (active) 2.dp else 1.dp, border),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.title, color = foreground, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.weight(1f))
                        if (active) Text("SELECTED", color = foreground, fontSize = 10.sp)
                    }
                    Text(plan.description, color = colors.onSurfaceVariant, fontSize = 14.sp)
                    AnimatedVisibility(visible = active && detailsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            plan.benefits.forEach { Text("- $it", color = foreground, fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
        Button(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (detailsExpanded) "Hide features" else "View features")
        }
        Text("Plans are for preview only. Payments are not available yet.", color = colors.onSurfaceVariant, fontSize = 12.sp)
    }
}
