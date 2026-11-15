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

@Composable
fun LinesScreen(nav: NavController) {
    val ctx = LocalContext.current
    val dao = remember { Db.get(ctx).dao() }
    val lines by remember { dao.lines() }.collectAsState(emptyList())
    val deps by remember { dao.departures() }.collectAsState(emptyList())
    var q by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    var appUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh(manual: Boolean) {
        scope.launch {
            if (manual) info = "Verificando..."
            val r = DataSync.check(ctx)
            appUrl = r.appUrl
            info = when {
                r.updated -> "Horários atualizados."
                manual && r.error -> "Sem conexão ou endereço indisponível."
                manual -> "Você já está com os horários mais recentes."
                else -> null
            }
        }
    }
    LaunchedEffect(Unit) { if (!BuildConfig.ADMIN) refresh(false) }

    Screen("Esperaí", null, actions = {
        if (BuildConfig.ADMIN) {
            IconButton({ nav.navigate("admin") }) { Icon(Icons.Filled.Settings, "Cadastro") }
        } else {
            IconButton({ refresh(true) }) { Icon(Icons.Filled.Refresh, "Atualizar horários") }
        }
    }) {
        OutlinedTextField(
            value = q, onValueChange = { q = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar linha ou cidade") },
            leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true
        )
        info?.let { Text(it, color = Teal) }
        appUrl?.let { u -> Button({ openUrl(ctx, u) }, Modifier.fillMaxWidth()) { Text("Nova versão do app disponível") } }

        val shown = lines.filter {
            q.isBlank() || listOf(it.number, it.name, it.origin, it.destination).any { f -> f.contains(q.trim(), ignoreCase = true) }
        }
        val today = todayType()
        val now = nowHm()
        Section("${shown.size} linhas")
        if (shown.isEmpty()) Text("Nenhuma linha encontrada.", color = Muted)
        shown.forEach { l ->
            val next = deps.filter { it.lineId == l.id && it.dayType == today && it.time >= now }.minOfOrNull { it.time }
            CardRow({ nav.navigate("line/${l.id}") }) {
                LineBadge(l.number)
                Column(Modifier.weight(1f)) {
                    Text("${l.origin} → ${l.destination}", fontWeight = FontWeight.SemiBold)
                    if (l.name.isNotBlank()) Text(l.name, fontSize = 13.sp, color = Muted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Próxima saída", fontSize = 12.sp, color = Muted)
                    Text(next ?: "--:--", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LineScreen(nav: NavController, id: Long) {
    val ctx = LocalContext.current
    val dao = remember { Db.get(ctx).dao() }
    var line by remember { mutableStateOf<Line?>(null) }
    LaunchedEffect(id) { line = dao.line(id) }
    val stops by remember(id) { dao.stopsOfLine(id) }.collectAsState(emptyList())
    val trips by remember(id) { dao.tripsOfLine(id) }.collectAsState(emptyList())
    val times by remember(id) { dao.timesOfLine(id) }.collectAsState(emptyList())
    var day by remember { mutableIntStateOf(todayType()) }
    var sel by remember { mutableStateOf<Long?>(null) }
    val views = tripViews(trips, times, stops, day)
    val now = nowHm()
    val next = if (day == todayType()) views.firstOrNull { it.dep != null && it.dep >= now } else null
    val shownTrip = views.firstOrNull { it.trip.id == sel } ?: next ?: views.firstOrNull()

    Screen("Linha ${line?.number ?: ""}", nav) {
        line?.let { l ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LineBadge(l.number)
                Column {
                    Text("${l.origin} → ${l.destination}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (l.name.isNotBlank()) Text(l.name, color = Muted)
                }
            }
            if (l.notes.isNotBlank()) Text(l.notes, color = Muted)
        }
        DayTabs(day) { day = it; sel = null }
        Section("Viagens")
        if (views.isEmpty()) Text("Sem viagens cadastradas para este dia.", color = Muted)
        views.forEach { tv ->
            CardRow({ sel = tv.trip.id }, highlight = tv.trip.id == shownTrip?.trip?.id) {
                Text("${tv.dep ?: "--:--"} → ${tv.arr ?: "--:--"}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(duration(tv.dep, tv.arr), color = Muted)
                if (tv.trip.id == next?.trip?.id) {
                    Text(
                        "Próxima", color = Color(0xFF6B4200), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.background(Color(0xFFFFF1CC), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Section("Paradas · toque para ver no mapa")
        stops.forEach { s ->
            CardRow({ nav.navigate("stop/${s.id}") }) {
                Box(Modifier.size(12.dp).background(Teal, CircleShape))
                Text(s.name, Modifier.weight(1f))
                Text(shownTrip?.times?.get(s.id) ?: "", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StopScreen(nav: NavController, id: Long) {
    val ctx = LocalContext.current
    val dao = remember { Db.get(ctx).dao() }
    val stop by remember(id) { dao.stopFlow(id) }.collectAsState(null)
    val lines by remember { dao.lines() }.collectAsState(emptyList())
    val tl by remember(id) { dao.timesAtStop(id) }.collectAsState(emptyList())
    val today = todayType()

    Screen("Parada", nav) {
        stop?.let { s ->
            val ok = s.lat != null && s.lng != null
            Text(s.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (s.reference.isNotBlank()) Text(s.reference, color = Muted)
            Button({ openMaps(ctx, s, false) }, Modifier.fillMaxWidth().height(48.dp), enabled = ok) {
                Icon(Icons.Filled.LocationOn, null)
                Spacer(Modifier.width(8.dp))
                Text("Abrir no Google Maps")
            }
            OutlinedButton({ openMaps(ctx, s, true) }, Modifier.fillMaxWidth().height(48.dp), enabled = ok) { Text("Como chegar a pé") }
            if (!ok) Text("Esta parada ainda não tem localização cadastrada.", color = Muted)
            Section("Linhas que passam aqui · ${DAY_LABELS[today]}")
            lines.filter { l -> tl.any { it.lineId == l.id } }.forEach { l ->
                val ts = tl.filter { it.lineId == l.id && it.dayType == today }.map { it.time }.sorted()
                CardRow({ nav.navigate("line/${l.id}") }) {
                    LineBadge(l.number)
                    Column(Modifier.weight(1f)) {
                        Text("${l.origin} → ${l.destination}", fontWeight = FontWeight.SemiBold)
                        Text(if (ts.isEmpty()) "Sem horários hoje" else ts.joinToString(" · "), color = Muted)
                    }
                }
            }
        }
    }
}
