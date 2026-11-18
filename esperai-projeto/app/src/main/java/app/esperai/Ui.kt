@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.esperai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

val Teal = Color(0xFF0A5C52)
val TealLight = Color(0xFF12806F)
val Mint = Color(0xFFD9EFEA)
val Ground = Color(0xFFF2F6F6)
val Muted = Color(0xFF52606D)
val Ink = Color(0xFF14202B)
private val HeaderShape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
private val HeaderBrush = Brush.horizontalGradient(listOf(Teal, TealLight))

@Composable
fun EsperaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal, onPrimary = Color.White, secondary = Teal,
            background = Ground, surface = Color.White,
            primaryContainer = Mint, onPrimaryContainer = Color(0xFF00201C)
        ),
        content = content
    )
}

fun timeRange(dep: String?, arr: String?): AnnotatedString = buildAnnotatedString {
    append(dep ?: "--:--")
    withStyle(SpanStyle(color = Muted, fontWeight = FontWeight.Normal)) { append(" → ") }
    append(arr ?: "--:--")
}

@Composable
fun Screen(
    title: String,
    nav: NavController?,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    brand: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        floatingActionButton = { Box(Modifier.navigationBarsPadding()) { fab() } },
        containerColor = Ground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box(Modifier.fillMaxWidth().shadow(6.dp, HeaderShape).background(HeaderBrush, HeaderShape)) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = if (brand) 20.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (brand) {
                        Box(
                            Modifier.size(56.dp)
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(painterResource(R.drawable.ic_bus), null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                        Box(Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.45f)))
                        Column(Modifier.weight(1f)) {
                            Text("Esperaí", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (BuildConfig.ADMIN) "Modo administrador" else "Seu ônibus, na hora certa!",
                                color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontStyle = FontStyle.Italic
                            )
                        }
                    } else {
                        if (nav != null) {
                            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White) }
                        } else {
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                    CompositionLocalProvider(LocalContentColor provides Color.White) { actions() }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
fun Section(t: String, accent: String? = null) {
    val s = buildAnnotatedString {
        append(t.uppercase())
        if (accent != null) {
            append("  ·  ")
            withStyle(SpanStyle(color = Teal)) { append(accent.uppercase()) }
        }
    }
    Text(s, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
fun LineBadge(n: String) {
    Box(
        Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 44.dp).background(Teal, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(n, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}

@Composable
fun BusBadge(n: String, size: Dp = 76.dp, showIcon: Boolean = true) {
    Column(
        Modifier.width(size).heightIn(min = size)
            .background(Brush.verticalGradient(listOf(TealLight, Teal)), RoundedCornerShape(size / 4))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showIcon) Icon(painterResource(R.drawable.ic_bus), null, tint = Color.White, modifier = Modifier.size(size * 0.38f))
        Text(
            n, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (size >= 90.dp) 16.sp else 14.sp,
            textAlign = TextAlign.Center, maxLines = 2, lineHeight = 17.sp
        )
    }
}

@Composable
fun ChevronCircle() {
    Box(Modifier.size(36.dp).background(Mint, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = Teal)
    }
}

@Composable
fun InfoLine(icon: Painter, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = Teal, modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Text(text, fontSize = 13.sp, color = Muted, lineHeight = 17.sp)
    }
}

@Composable
fun DurationPill(text: String) {
    if (text.isBlank()) return
    Row(
        Modifier.background(Mint, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(painterResource(R.drawable.ic_clock), null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
        Text(text, color = Teal, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun CardRow(onClick: () -> Unit, highlight: Boolean = false, pad: Dp = 14.dp, content: @Composable RowScope.() -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (highlight) androidx.compose.foundation.BorderStroke(2.dp, Teal) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E8E8))
    ) {
        Row(
            Modifier.padding(pad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun PillButton(text: String, filled: Boolean, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val fg = if (filled) Color.White else Teal
    var m = Modifier.fillMaxWidth().height(56.dp).alpha(if (enabled) 1f else 0.45f).clip(CircleShape)
    m = if (filled) m.background(Brush.horizontalGradient(listOf(Teal, TealLight))) else m.background(Color.White).border(2.dp, Teal, CircleShape)
    Row(
        m.clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

@Composable
fun DayTabs(sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DAY_LABELS.forEachIndexed { i, l ->
            val on = i == sel
            val tint = if (on) Color.White else Teal
            Row(
                Modifier.weight(1f).height(44.dp)
                    .shadow(if (on) 0.dp else 2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (on) Teal else Color.White)
                    .clickable(role = Role.Tab) { onSel(i) }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (i == 1) {
                    Icon(painterResource(R.drawable.ic_sun), null, tint = tint, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Filled.DateRange, null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(l, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
fun Field(label: String, v: String, hint: String? = null, kb: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = v, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = if (hint != null) ({ Text(hint) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = kb),
        singleLine = true
    )
}
