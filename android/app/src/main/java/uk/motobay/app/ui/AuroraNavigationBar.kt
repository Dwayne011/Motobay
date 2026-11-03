package uk.motobay.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.motobay.app.ui.theme.AuroraPalette

/**
 * The spec's bottom navigation bar for phones: 80dp over the gesture inset,
 * a 1dp top hairline, and a 56 x 30dp indicator pill behind the active 20dp
 * icon with an 11sp label 4dp below. The pill's colour and a small spring
 * scale animate as the selection moves.
 */
@Composable
fun AuroraNavigationBar(palette: AuroraPalette, modifier: Modifier = Modifier, items: @Composable RowScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(palette.navBarBg).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = palette.navBarBorder)
        Row(
            Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 8.dp).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            content = items,
        )
    }
}

@Composable
fun RowScope.AuroraNavigationItem(
    selected: Boolean,
    label: String,
    palette: AuroraPalette,
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit,
) {
    val pill by animateColorAsState(if (selected) palette.navPillActiveBg else Color.Transparent, label = "NavPill")
    val scale by animateFloatAsState(if (selected) 1f else 0.92f, spring(dampingRatio = 0.65f, stiffness = 400f), label = "NavPillScale")
    val tint = if (selected) palette.navIconActive else palette.navTextInactive
    Column(
        Modifier
            .weight(1f)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .width(56.dp)
                .height(30.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(15.dp))
                .background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(20.dp)) { icon(tint) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (selected) palette.navTextActive else palette.navTextInactive,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
