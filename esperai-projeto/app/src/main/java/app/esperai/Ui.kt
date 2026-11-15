@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.esperai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

val Teal = Color(0xFF0A5C52)
val Muted = Color(0xFF52606D)

@Composable
fun EsperaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal, onPrimary = Color.White, secondary = Teal,
            background = Color(0xFFF4F6F8), surface = Color.White,
            primaryContainer = Color(0xFFD5EDE8), onPrimaryContainer = Color(0xFF00201C)
        ),
        content = content
    )
}

@Composable
fun Screen(
    title: String,
    nav: NavController?,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (nav != null) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = fab,
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun Section(t: String) {
    Text(t.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
fun LineBadge(n: String) {
    Box(
        Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 44.dp).background(Teal, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(n, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}

@Composable
fun CardRow(onClick: () -> Unit, highlight: Boolean = false, content: @Composable RowScope.() -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (highlight) BorderStroke(2.dp, Teal) else BorderStroke(1.dp, Color(0xFFDDE3E9))
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun DayTabs(sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DAY_LABELS.forEachIndexed { i, l ->
            val m = Modifier.weight(1f)
            if (i == sel) {
                Button({ onSel(i) }, m, contentPadding = PaddingValues(4.dp)) { Text(l, maxLines = 1, fontSize = 13.sp) }
            } else {
                OutlinedButton({ onSel(i) }, m, contentPadding = PaddingValues(4.dp)) { Text(l, maxLines = 1, fontSize = 13.sp) }
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
