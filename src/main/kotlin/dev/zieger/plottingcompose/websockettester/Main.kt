package dev.zieger.plottingcompose.websockettester

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.singleWindowApplication
import dev.zieger.tablecomposable.Table
import dev.zieger.tablecomposable.entities.HeaderRow
import dev.zieger.tablecomposable.entities.HeaderValue
import dev.zieger.tablecomposable.entities.TableRow
import dev.zieger.tablecomposable.entities.TableValue
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

object WebSocketTester {

    @JvmStatic
    fun main(args: Array<String>) = singleWindowApplication {
        val host = remember { mutableStateOf("") }
        val path = remember { mutableStateOf("/") }
        val port = remember { mutableStateOf(80) }
        val pingInterval = remember { mutableStateOf<Long?>(null) }

        val incomingFrames = remember { mutableStateListOf<TableRow>() }
        val testSocket = remember { mutableStateOf<TestSocket?>(null) }

        Column {
            Row {
                Text("Host: ")
                TextField(host.value, { host.value = it }, Modifier.fillMaxWidth())
            }
            Row {
                Text("Path: ")
                TextField(path.value, { path.value = it }, Modifier.fillMaxWidth())
            }
            Row {
                Text("Port: ")
                TextField("${port.value}", { port.value = it.toIntOrNull() ?: 80 }, Modifier.fillMaxWidth())
            }
            Row {
                Text("Ping Interval: ")
                TextField(
                    "${pingInterval.value ?: ""}",
                    { pingInterval.value = it.toLongOrNull() },
                    Modifier.fillMaxWidth()
                )
            }

            val scope = rememberCoroutineScope { Dispatchers.IO }
            var buttonText by remember { mutableStateOf("connect") }
            Button({
                testSocket.value = TestSocket(
                    scope, host.value, path.value, port.value,
                    pingInterval.value, incomingFrames
                )

                runCatching {
                    println("connect…")
                    buttonText = "…"
                    testSocket.value?.connect()
                }.onFailure {
                    println("failed with $it")
                    buttonText = "failed"
                }.onSuccess {
                    println("success")
                    buttonText = "connected"
                }
            }) { Text(buttonText) }

            Table(
                HeaderRow(
                    HeaderValue("Frame")
                ),
                incomingFrames
            )
        }
    }
}

class TestSocket(
    private val scope: CoroutineScope,
    private val host: String,
    private val path: String,
    private val port: Int,
    private val pingInterval: Long?,
    private val incomingFrames: SnapshotStateList<TableRow>
) {

    private val client = HttpClient(CIO) {
        followRedirects = true

        install(WebSockets) {
            this@TestSocket.pingInterval?.also {
                pingInterval = it
            }
        }
    }

    private lateinit var outChan: SendChannel<Frame>

    fun connect() = scope.launch {
        client.wss(host = host, path = path, port = port) {
            outChan = outgoing
            var idx = 0
            for (frame in incoming) incomingFrames.add(
                TableRow(
                    TableValue("$frame"),
                    key = idx++
                ) {})
        }
    }

    fun sendFrame(frame: Frame) = scope.launch { outChan.send(frame) }
}