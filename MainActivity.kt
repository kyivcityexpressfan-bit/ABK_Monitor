package ua.abk.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private val Bg = Color(0xFF101820)
private val Panel = Color(0xFF1B2732)
private val Orange = Color(0xFFFF8A00)

data class AlertItem(
    val title: String = "",
    val body: String = "",
    val region: String = "",
    val severity: String = "info"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MonitorScreen() }
    }
}

@Composable
fun MonitorScreen() {
    var alerts by remember { mutableStateOf<List<AlertItem>>(emptyList()) }
    var status by remember { mutableStateOf("Завантаження статусів…") }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val alertListener = db.collection("alerts")
            .whereEqualTo("active", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, e ->
                if (e != null) { error = e.localizedMessage; return@addSnapshotListener }
                alerts = snap?.documents?.map { d ->
                    AlertItem(
                        title = d.getString("title") ?: "Повідомлення",
                        body = d.getString("body") ?: "",
                        region = d.getString("region") ?: "Україна",
                        severity = d.getString("severity") ?: "info"
                    )
                } ?: emptyList()
            }
        val statusListener = db.collection("regional_status")
            .addSnapshotListener { snap, e ->
                if (e != null) { error = e.localizedMessage; return@addSnapshotListener }
                val docs = snap?.documents.orEmpty()
                status = if (docs.isEmpty()) "Немає опублікованих статусів" else
                    docs.joinToString("   •   ") { d ->
                        "${d.getString("region") ?: d.id}: ${statusLabel(d.getString("status") ?: "unknown")}"
                    }
            }
        onDispose { alertListener.remove(); statusListener.remove() }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Orange, background = Bg, surface = Panel
    )) {
        Column(
            Modifier.fillMaxSize().background(Bg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ABK MONITOR", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("КИЇВ • КИЇВСЬКА ОБЛАСТЬ", color = Orange, style = MaterialTheme.typography.labelLarge)
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(14.dp)) {
                    Text("ОФІЦІЙНІ РЕГІОНАЛЬНІ СТАТУСИ", color = Orange)
                    Spacer(Modifier.height(8.dp))
                    Text(status, color = Color.White)
                }
            }
            Text("ПОВІДОМЛЕННЯ", color = Orange, style = MaterialTheme.typography.titleMedium)
            error?.let { Text("Помилка підключення: $it", color = Color(0xFFFF6B6B)) }
            if (alerts.isEmpty()) {
                Text("Активних повідомлень немає", color = Color.LightGray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(alerts) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Panel)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text("${item.region} • ${severityLabel(item.severity)}", color = Orange)
                                Text(item.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Text(item.body, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Інформаційний сервіс. Дотримуйтеся офіційних повідомлень ДСНС та місцевої влади.",
                color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun statusLabel(value: String) = when (value) {
    "alert" -> "ТРИВОГА"
    "safe" -> "ВІДБІЙ"
    else -> "НЕВІДОМО"
}
private fun severityLabel(value: String) = when (value) {
    "alert" -> "ТРИВОГА"
    "warning" -> "УВАГА"
    "all_clear" -> "ВІДБІЙ"
    else -> "ІНФОРМАЦІЯ"
}
