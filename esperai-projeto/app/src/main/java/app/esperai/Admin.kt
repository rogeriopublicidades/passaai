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
fun AdminHome(nav: NavController) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<String?>(null) }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val t = DataSync.exportData(db)
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(t.toByteArray()) }
            info = "Arquivo salvo. Envie-o ao site para atualizar os usuários."
        }
    }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            info = try {
                val t = ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                DataSync.importData(db, t)
                "Dados importados."
            } catch (e: Exception) {
                "Arquivo inválido."
            }
        }
    }
    Screen("Cadastro", nav) {
        Button({ nav.navigate("admin/lines") }, Modifier.fillMaxWidth().height(52.dp)) { Text("Linhas") }
        Button({ nav.navigate("admin/stops") }, Modifier.fillMaxWidth().height(52.dp)) { Text("Paradas") }
        Section("Atualização dos usuários")
        Text("Exporte o arquivo dados.json e coloque-o no site. O app dos usuários baixa sozinho a versão mais nova.", color = Muted)
        OutlinedButton({ exp.launch("dados.json") }, Modifier.fillMaxWidth().height(52.dp)) { Text("Exportar dados (dados.json)") }
        OutlinedButton({ imp.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth().height(52.dp)) { Text("Importar dados de arquivo") }
        info?.let { Text(it, color = Teal) }
    }
}

@Composable
fun AdminLines(nav: NavController) {
    val dao = remember { Db.get(LocalContext.current).dao() }
    val lines by remember { dao.lines() }.collectAsState(emptyList())
    Screen("Linhas", nav, fab = {
        FloatingActionButton({ nav.navigate("admin/line/0") }, containerColor = Teal, contentColor = Color.White) { Icon(Icons.Filled.Add, "Nova linha") }
    }) {
        if (lines.isEmpty()) Text("Nenhuma linha. Toque em + para cadastrar.", color = Muted)
        lines.forEach { l ->
            CardRow({ nav.navigate("admin/line/${l.id}") }) {
                LineBadge(l.number)
                Text("${l.origin} → ${l.destination}", Modifier.weight(1f))
                Icon(Icons.Filled.Edit, "Editar")
            }
        }
    }
}

@Composable
fun LineForm(nav: NavController, id: Long) {
    val dao = remember { Db.get(LocalContext.current).dao() }
    val scope = rememberCoroutineScope()
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }
    var dest by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id != 0L) dao.line(id)?.let { number = it.number; name = it.name; origin = it.origin; dest = it.destination; notes = it.notes }
    }
    Screen(if (id == 0L) "Nova linha" else "Editar linha", nav) {
        Field("Número da linha", number) { number = it }
        Field("Nome da linha", name) { name = it }
        Field("Ponto inicial", origin) { origin = it }
        Field("Ponto final", dest) { dest = it }
        Field("Observações", notes) { notes = it }
        Button(
            {
                scope.launch {
                    dao.saveLine(Line(id, number.trim(), name.trim(), origin.trim(), dest.trim(), notes.trim()))
                    nav.popBackStack()
                }
            },
            Modifier.fillMaxWidth().height(48.dp),
            enabled = number.isNotBlank() && origin.isNotBlank() && dest.isNotBlank()
        ) { Text("Salvar linha") }
        if (id != 0L) {
            OutlinedButton({ nav.navigate("admin/order/$id") }, Modifier.fillMaxWidth().height(48.dp)) { Text("Ordem das paradas") }
            OutlinedButton({ nav.navigate("admin/times/$id") }, Modifier.fillMaxWidth().height(48.dp)) { Text("Horários") }
            TextButton({ confirm = true }) { Text("Excluir linha", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Excluir linha?") },
        text = { Text("Os horários e a ordem das paradas desta linha também serão apagados.") },
        confirmButton = { TextButton({ scope.launch { dao.deleteLineFull(id); nav.popBackStack() } }) { Text("Excluir") } },
        dismissButton = { TextButton({ confirm = false }) { Text("Cancelar") } }
    )
}

@Composable
fun AdminStops(nav: NavController) {
    val dao = remember { Db.get(LocalContext.current).dao() }
    val stops by remember { dao.stops() }.collectAsState(emptyList())
    Screen("Paradas", nav, fab = {
        FloatingActionButton({ nav.navigate("admin/stop/0") }, containerColor = Teal, contentColor = Color.White) { Icon(Icons.Filled.Add, "Nova parada") }
    }) {
        if (stops.isEmpty()) Text("Nenhuma parada. Toque em + para cadastrar.", color = Muted)
        stops.forEach { s ->
            CardRow({ nav.navigate("admin/stop/${s.id}") }) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, fontWeight = FontWeight.SemiBold)
                    if (s.reference.isNotBlank()) Text(s.reference, fontSize = 13.sp, color = Muted)
                }
                if (s.lat == null) Icon(Icons.Filled.Warning, "Sem localização")
                Icon(Icons.Filled.Edit, "Editar")
            }
        }
    }
}

@Composable
fun StopForm(nav: NavController, id: Long) {
    val ctx = LocalContext.current
    val dao = remember { Db.get(ctx).dao() }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }
    var coords by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id != 0L) dao.stop(id)?.let {
            name = it.name; ref = it.reference
            if (it.lat != null && it.lng != null) coords = "${it.lat}, ${it.lng}"
        }
    }
    Screen(if (id == 0L) "Nova parada" else "Editar parada", nav) {
        Field("Nome da parada", name) { name = it }
        Field("Referência do local", ref) { ref = it }
        Field("Coordenadas", coords, "No Google Maps, toque e segure no local, copie as coordenadas e cole aqui.") { coords = it; error = null }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton({ openUrl(ctx, "https://www.google.com/maps") }, Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Filled.LocationOn, null)
            Spacer(Modifier.width(8.dp))
            Text("Abrir Google Maps para marcar o local")
        }
        Button(
            {
                val c = if (coords.isBlank()) null else parseCoords(coords)
                if (coords.isNotBlank() && c == null) {
                    error = "Coordenadas inválidas. Exemplo: -23.123456, -47.123456"
                } else scope.launch {
                    dao.saveStop(Stop(id, name.trim(), ref.trim(), c?.first, c?.second))
                    nav.popBackStack()
                }
            },
            Modifier.fillMaxWidth().height(48.dp), enabled = name.isNotBlank()
        ) { Text("Salvar parada") }
        if (id != 0L) TextButton({ confirm = true }) { Text("Excluir parada", color = MaterialTheme.colorScheme.error) }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Excluir parada?") },
        text = { Text("Ela será removida das linhas e dos horários.") },
        confirmButton = { TextButton({ scope.launch { dao.deleteStopFull(id); nav.popBackStack() } }) { Text("Excluir") } },
        dismissButton = { TextButton({ confirm = false }) { Text("Cancelar") } }
    )
}

@Composable
fun OrderScreen(nav: NavController, lineId: Long) {
    val dao = remember { Db.get(LocalContext.current).dao() }
    val scope = rememberCoroutineScope()
    val all by remember { dao.stops() }.collectAsState(emptyList())
    val ids = remember { mutableStateListOf<Long>() }
    var adding by remember { mutableStateOf(false) }
    LaunchedEffect(lineId) { ids.clear(); ids.addAll(dao.stopsOfLineOnce(lineId).map { it.id }) }

    Screen("Paradas da linha", nav) {
        Text("Coloque as paradas na ordem em que o ônibus passa.", color = Muted)
        ids.forEachIndexed { i, sid ->
            val s = all.firstOrNull { it.id == sid }
            CardRow({}) {
                Text("${i + 1}", fontWeight = FontWeight.Bold, color = Teal)
                Column(Modifier.weight(1f)) {
                    Text(s?.name ?: "?")
                    if (i == 0) Text("Origem", fontSize = 12.sp, color = Muted) else if (i == ids.lastIndex) Text("Destino", fontSize = 12.sp, color = Muted)
                }
                IconButton({ ids.add(i - 1, ids.removeAt(i)) }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Subir") }
                IconButton({ ids.add(i + 1, ids.removeAt(i)) }, enabled = i < ids.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, "Descer") }
                IconButton({ ids.removeAt(i) }) { Icon(Icons.Filled.Close, "Remover da linha") }
            }
        }
        OutlinedButton({ adding = true }, Modifier.fillMaxWidth().height(48.dp)) { Text("Adicionar parada") }
        Button({ scope.launch { dao.setLineStops(lineId, ids.toList()); nav.popBackStack() } }, Modifier.fillMaxWidth().height(48.dp)) { Text("Salvar ordem") }
    }
    if (adding) AlertDialog(
        onDismissRequest = { adding = false },
        title = { Text("Adicionar parada") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val free = all.filter { it.id !in ids }
                if (free.isEmpty()) Text("Não há paradas disponíveis. Cadastre novas em Cadastro > Paradas.")
                free.forEach { s -> TextButton({ ids.add(s.id); adding = false }) { Text(s.name) } }
            }
        },
        confirmButton = { TextButton({ adding = false }) { Text("Fechar") } }
    )
}

class Draft(val id: Long, val times: SnapshotStateMap<Long, String>)

@Composable
fun TimesScreen(nav: NavController, lineId: Long) {
    val dao = remember { Db.get(LocalContext.current).dao() }
    val scope = rememberCoroutineScope()
    val stops by remember(lineId) { dao.stopsOfLine(lineId) }.collectAsState(emptyList())
    val trips by remember(lineId) { dao.tripsOfLine(lineId) }.collectAsState(emptyList())
    val times by remember(lineId) { dao.timesOfLine(lineId) }.collectAsState(emptyList())
    var day by remember { mutableIntStateOf(todayType()) }
    var draft by remember { mutableStateOf<Draft?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val views = tripViews(trips, times, stops, day)

    fun save(d: Draft) {
        val out = mutableListOf<StopTime>()
        for (s in stops) {
            val raw = d.times[s.id].orEmpty().trim()
            if (raw.isEmpty()) continue
            val n = normTime(raw)
            if (n == null) { error = "Horário inválido em ${s.name}: $raw"; return }
            out += StopTime(0, s.id, n)
        }
        if (out.none { it.stopId == stops.first().id } || out.none { it.stopId == stops.last().id }) {
            error = "Preencha ao menos o horário da primeira e da última parada."
            return
        }
        scope.launch { dao.saveTrip(Trip(d.id, lineId, day), out); draft = null; error = null }
    }

    Screen(if (draft == null) "Horários" else "Viagem · ${DAY_LABELS[day]}", nav) {
        val d = draft
        if (stops.size < 2) {
            Text("Cadastre pelo menos duas paradas na ordem da linha (Ordem das paradas) antes de lançar horários.", color = Muted)
        } else if (d == null) {
            DayTabs(day) { day = it }
            Section("Viagens")
            if (views.isEmpty()) Text("Nenhuma viagem neste tipo de dia.", color = Muted)
            views.forEach { tv ->
                CardRow({ draft = Draft(tv.trip.id, mutableStateMapOf<Long, String>().apply { putAll(tv.times) }) }) {
                    Text("${tv.dep ?: "--:--"} → ${tv.arr ?: "--:--"}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(duration(tv.dep, tv.arr), color = Muted)
                }
            }
            Button({ draft = Draft(0, mutableStateMapOf()) }, Modifier.fillMaxWidth().height(48.dp)) { Text("Nova viagem") }
        } else {
            stops.forEach { s ->
                OutlinedTextField(
                    value = d.times[s.id] ?: "", onValueChange = { d.times[s.id] = it; error = null },
                    modifier = Modifier.fillMaxWidth(), label = { Text(s.name) }, placeholder = { Text("HH:MM") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button({ save(d) }, Modifier.fillMaxWidth().height(48.dp)) { Text("Salvar viagem") }
            if (d.id != 0L) {
                OutlinedButton(
                    { draft = Draft(0, mutableStateMapOf<Long, String>().apply { putAll(d.times) }); error = null },
                    Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Duplicar (editar como nova viagem)") }
                TextButton({ scope.launch { dao.deleteTripFull(d.id); draft = null } }) { Text("Excluir viagem", color = MaterialTheme.colorScheme.error) }
            }
            TextButton({ draft = null; error = null }) { Text("Cancelar") }
        }
    }
}
