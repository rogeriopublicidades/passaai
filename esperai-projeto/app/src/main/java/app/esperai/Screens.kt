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

@Composable
fun LinesScreen(nav: NavController) {
    val ctx = LocalContext.current
    val dao = remember { Db.get(ctx).dao() }
    val lines by remember { dao.lines() }.collectAsState(emptyList())
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

    Screen("Esperaí", null, brand = true, actions = {
        if (BuildConfig.ADMIN) {
            IconButton({ nav.navigate("admin") }) { Icon(Icons.Filled.Settings, "Cadastro") }
        } else {
            IconButton({ refresh(true) }) { Icon(Icons.Filled.Refresh, "Atualizar horários") }
        }
    }) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, null, tint = Teal)
            BasicTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = Ink), cursorBrush = SolidColor(Teal),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (q.isEmpty()) Text("Buscar linha ou cidade", color = Muted, fontSize = 16.sp)
                        inner()
                    }
                }
            )
        }
        info?.let { Text(it, color = Teal, fontWeight = FontWeight.SemiBold) }
        appUrl?.let { u -> PillButton("Nova versão do app disponível", true) { openUrl(ctx, u) } }

        val shown = lines.filter {
            q.isBlank() || listOf(it.number, it.name, it.origin, it.destination, it.notes).any { f -> f.contains(q.trim(), ignoreCase = true) }
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_list), null, tint = Teal, modifier = Modifier.size(20.dp))
            Text(
                "${shown.size} ${if (shown.size == 1) "LINHA" else "LINHAS"}",
                color = Teal, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp
            )
        }
        if (shown.isEmpty()) Text("Nenhuma linha encontrada.", color = Muted)
        shown.forEach { l ->
            CardRow({ nav.navigate("line/${l.id}") }) {
                BusBadge(l.number)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${l.origin} → ${l.destination}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink, lineHeight = 21.sp)
                    if (l.name.isNotBlank()) InfoLine(rememberVectorPainter(Icons.Filled.LocationOn), l.name)
                    if (l.notes.isNotBlank()) InfoLine(painterResource(R.drawable.ic_route), l.notes)
                }
                ChevronCircle()
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
    val views = tripViews(trips, times, stops, day)
    val now = nowHm()
    val next = if (day == todayType()) views.firstOrNull { it.dep != null && it.dep >= now } else null

    Screen("Linha ${line?.number ?: ""}", nav) {
        line?.let { l ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BusBadge(l.number, 96.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${l.origin} → ${l.destination}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 26.sp)
                    if (l.name.isNotBlank()) Text(l.name, color = Muted, fontSize = 14.sp)
                }
            }
            if (l.notes.isNotBlank()) InfoLine(painterResource(R.drawable.ic_route), l.notes)
        }
        DayTabs(day) { day = it }
        if (next != null) {
            Section("Próxima viagem")
            CardRow({ nav.navigate("trip/$id/${next.trip.id}") }, highlight = true, pad = 18.dp) {
                Text(timeRange(next.dep, next.arr), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                DurationPill(duration(next.dep, next.arr))
            }
        }
        Section("Todas as viagens · toque para ver as paradas")
        if (views.isEmpty()) Text("Sem viagens cadastradas para este dia.", color = Muted)
        views.forEach { tv ->
            CardRow({ nav.navigate("trip/$id/${tv.trip.id}") }) {
                Icon(painterResource(R.drawable.ic_clock), null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                Text(timeRange(tv.dep, tv.arr), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
                Text(duration(tv.dep, tv.arr), color = Muted)
                Icon(Icons.Filled.KeyboardArrowRight, null, tint = Teal)
            }
        }
    }
}

@Composable
fun TripScreen(nav: NavController, lineId: Long, tripId: Long) {
    val ctx = LocalContext.current
    val dao = remember { Db.get(ctx).dao() }
    var line by remember { mutableStateOf<Line?>(null) }
    LaunchedEffect(lineId) { line = dao.line(lineId) }
    val stops by remember(lineId) { dao.stopsOfLine(lineId) }.collectAsState(emptyList())
    val trips by remember(lineId) { dao.tripsOfLine(lineId) }.collectAsState(emptyList())
    val times by remember(lineId) { dao.timesOfLine(lineId) }.collectAsState(emptyList())
    val trip = trips.firstOrNull { it.id == tripId }
    val tv = trip?.let { t -> tripViews(trips, times, stops, t.dayType).firstOrNull { it.trip.id == t.id } }

    Screen("Viagem", nav) {
        val l = line
        if (tv != null && l != null) {
            val dur = duration(tv.dep, tv.arr)
            val sub = listOfNotNull(DAY_LABELS[tv.trip.dayType], if (dur.isBlank()) null else "duração $dur").joinToString(" · ")
            Column(
                Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(22.dp)).background(Color.White, RoundedCornerShape(22.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BusBadge(l.number, 72.dp, showIcon = false)
                    Text("${l.origin} → ${l.destination}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                }
                Text(timeRange(tv.dep, tv.arr), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(sub, color = Muted)
            }
            Section("Próximas paradas e horários previstos")
            stops.forEach { s ->
                val t = tv.times[s.id]
                CardRow({ nav.navigate("stop/${s.id}") }, pad = 12.dp) {
                    Box(Modifier.size(12.dp).background(Teal, CircleShape))
                    Text(s.name, fontSize = 16.sp, color = Ink, modifier = Modifier.weight(1f))
                    Text(t ?: "--:--", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (t == null) Muted else Ink)
                }
            }
            Text("Os horários nas paradas são previstos e podem variar com o trânsito.", fontSize = 13.sp, color = Muted)
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
            Text(s.name, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink, lineHeight = 32.sp)
            if (s.reference.isNotBlank()) Text(s.reference, color = Muted)
            PillButton("Abrir no Google Maps", true, ok, Icons.Filled.LocationOn) { openMaps(ctx, s, false) }
            PillButton("Como chegar a pé", false, ok) { openMaps(ctx, s, true) }
            if (!ok) Text("Esta parada ainda não tem localização cadastrada.", color = Muted)
            Section("Linhas que passam aqui", DAY_LABELS[today])
            lines.filter { l -> tl.any { it.lineId == l.id } }.forEach { l ->
                val ts = tl.filter { it.lineId == l.id && it.dayType == today }.map { it.time }.sorted()
                CardRow({ nav.navigate("line/${l.id}") }, pad = 16.dp) {
                    BusBadge(l.number, 84.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${l.origin} → ${l.destination}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink, lineHeight = 22.sp)
                        Text(if (ts.isEmpty()) "Sem horários hoje" else ts.joinToString(" · "), color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            }
        }
    }
}
