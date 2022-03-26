package dev.zieger.plottingcompose.bitinex

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

class SocketEndpoint(
    private val scope: CoroutineScope
) {

    companion object {

        private const val BASE_HOST = "api-pub.bitfinex.com"
    }

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
                followSslRedirects(true)
                retryOnConnectionFailure(true)
            }
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
        install(WebSockets) {
            pingInterval = 30_000L
        }
    }

    private val initMutex = Mutex()
    private var isInitialized = false
    private val incoming = MutableSharedFlow<Frame>()
    private val outgoing = Channel<ChannelRequest>()

    private suspend fun connect() {
        val cont = Channel<Unit>()

        scope.launch {
            while (!initMutex.tryLock()) delay(1)
            if (isInitialized) {
                cont.send(Unit)
                return@launch
            }

            client.ws(host = BASE_HOST, path = "/ws/2/") {
                scope.launch {
                    for (channelRequest in this@SocketEndpoint.outgoing)
                        outgoing.send(Frame.Text(json.encodeToString(channelRequest)))
                }

                isInitialized = true
                initMutex.unlock()

                cont.send(Unit)

                for (frame in incoming)
                    this@SocketEndpoint.incoming.emit(frame)
            }
        }

        cont.receive()
    }

    private suspend fun subscribeChannel(
        type: BitfinexChannelType,
        keyAppendix: String,
        onData: suspend (List<BitfinexCandle>) -> Unit
    ) {
        val key = "${type.key}:$keyAppendix"
        outgoing.send(ChannelRequest("subscribe", type, key))

        var channelId = -1
        var snapshotReceived = false
        incoming.collect { frame ->
            val content = frame.readBytes().decodeToString()
            when {
                channelId < 0 && content.startsWith("{") -> {
                    Json.decodeFromString(ChannelResponse.serializer(), content)
                        .takeIf { it.chanId != null && it.channel == "$type" && it.key == key }
                        ?.also { channelId = it.chanId!! }
                }
                !snapshotReceived
                        && content.removePrefix("[").startsWith("$channelId,[[") -> {
                    Json.decodeFromString(
                        ListSerializer(BitfinexCandle.serializer()),
                        content.removePrefix("[$channelId,").removeSuffix("]")
                    ).also { data -> onData(data) }
                    snapshotReceived = true
                }
                snapshotReceived
                        && content.removePrefix("[").startsWith("$channelId,[") -> Json.decodeFromString(
                    BitfinexCandle.serializer(),
                    content.removePrefix("[$channelId,").removeSuffix("]")
                ).also { data -> onData(listOf(data)) }
            }
        }
    }

    fun candles(
        pair: BitfinexPair,
        interval: BitfinexInterval
    ): Flow<BitfinexCandle> = flow {
        connect()
        subscribeChannel(BitfinexChannelType.CANDLES, "$interval:$pair") { candles ->
            candles.forEach { emit(it) }
        }
    }
}

object BitfinexChannelTypeSerializer : KSerializer<BitfinexChannelType> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: BitfinexChannelType) =
        String.serializer().serialize(encoder, "$value")

    override fun deserialize(decoder: Decoder): BitfinexChannelType =
        BitfinexChannelType.valueOf(String.serializer().deserialize(decoder).uppercase())
}

@Serializable(with = BitfinexChannelTypeSerializer::class)
enum class BitfinexChannelType(val key: String) {
    CANDLES("trade");

    override fun toString(): String = name.lowercase()
}

@Serializable
data class ChannelRequest(
    val event: String,
    val channel: BitfinexChannelType,
    val key: String
)

@Serializable
data class ChannelResponse(
    val event: String? = null,
    val channel: String? = null,
    val chanId: Int? = null,
    val key: String? = null,
    val version: Int? = null,
    val serverId: String? = null,
    val platform: Platform? = null
)

@Serializable
data class Platform(val status: Int)