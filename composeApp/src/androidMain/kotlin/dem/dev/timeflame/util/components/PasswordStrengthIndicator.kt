package dem.dev.timeflame.util.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PasswordStrengthIndicator(
    strengthLevel: Int, // 1 - too easy, 2 - easy, 3 - medium, 4 - strength
    currentAction: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        PasswordIndicatorLine(
            strengthLevel = strengthLevel,
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedContent(
            targetState = currentAction,
            transitionSpec = {
                slideInVertically { it } togetherWith slideOutVertically { -it }
            }, label = ""
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }
    }

}

@Composable
fun PasswordIndicatorLine(
    strengthLevel: Int, // 1 - too easy, 2 - easy, 3 - medium, 4 - strength
    modifier: Modifier = Modifier
) {
    val firstBlockColor by animateColorAsState(if(strengthLevel>=1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    val secondBlockColor by animateColorAsState(if(strengthLevel>=2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    val thirdBlockColor by animateColorAsState(if(strengthLevel>=3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    val fourthBlockColor by animateColorAsState(if(strengthLevel>=4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    Row(
        modifier = modifier
            .height(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .weight(1f)
                .background(firstBlockColor, RoundedCornerShape(15.dp))
        )
        Spacer(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .weight(1f)
                .background(secondBlockColor, RoundedCornerShape(15.dp))
        )
        Spacer(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .weight(1f)
                .background(thirdBlockColor, RoundedCornerShape(15.dp))
        )
        Spacer(modifier = Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .weight(1f)
                .background(fourthBlockColor, RoundedCornerShape(15.dp))
        )
    }
}