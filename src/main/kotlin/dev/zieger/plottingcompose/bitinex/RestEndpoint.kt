@file:Suppress("unused")

package dev.zieger.plottingcompose.bitinex

import dev.zieger.plottingcompose.indicators.candles.ICandle
import dev.zieger.utils.time.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class RestEndpoint {

    companion object {

        private const val BASE_HOST = "api-pub.bitfinex.com"
    }

    private val client = HttpClient(OkHttp) {
        followRedirects = true
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    fun candles(
        pair: BitfinexPair,
        interval: BitfinexInterval,
        start: ITimeStamp = TimeStamp(),
        sort: BitfinexSort = BitfinexSort.DESC,
        limit: Int = 10_000
    ): Flow<BitfinexCandle> = flow {
        client.get<List<BitfinexCandle>> {
            url {
                protocol = URLProtocol.HTTPS
                host = BASE_HOST

                path("v2", "candles", "trade:$interval:$pair", "hist")

                parameter("limit", limit)
                val end = start - (interval.duration * limit * sort.raw)
                parameter("start", min(start, end).millisLong)
                parameter("end", max(start, end).millisLong)
                parameter("sort", sort)
            }
        }.forEach { emit(it) }
    }
}

enum class BitfinexSort(val raw: Int) {
    ASC(-1),
    DESC(1);

    override fun toString(): String = "$raw"
}

enum class BitfinexSymbol {
    BTC,
    XMR,
    USD;

    override fun toString(): String = name
}

data class BitfinexPair(
    val base: BitfinexSymbol,
    val counter: BitfinexSymbol
) {
    override fun toString(): String = "t$base$counter"
}

enum class BitfinexInterval(
    val duration: ITimeSpan,
    val raw: String
) {

    M1(1.minutes, "1m"),
    M5(5.minutes, "5m"),
    M15(15.minutes, "15m"),
    M30(30.minutes, "30m"),
    H1(1.hours, "1h"),
    H3(3.hours, "3h"),
    H6(6.hours, "6h"),
    H12(12.hours, "12h"),
    D1(1.days, "1D"),
    W1(7.days, "1W"),
    D14(14.days, "14D"),
    MO1(1.months, "1M");

    override fun toString(): String = raw
}

@Serializable(with = BitfinexCandleSerializer::class)
data class BitfinexCandle(
    override val openTime: Long,
    override val open: Double,
    override val high: Double,
    override val close: Double,
    override val low: Double,
    override val volume: Double
) : ICandle {

    constructor(list: List<Double>) : this(list[0].toLong(), list[1], list[3], list[2], list[4], list[5])

    val list: List<Double> = listOf(openTime.toDouble(), open, close, high, low, volume.toDouble())
}

object BitfinexCandleSerializer : KSerializer<BitfinexCandle> {
    private val doubleSerializer = ListSerializer(Double.serializer())
    override val descriptor: SerialDescriptor
        get() = doubleSerializer.descriptor

    override fun serialize(encoder: Encoder, value: BitfinexCandle) =
        encoder.encodeSerializableValue(doubleSerializer, value.list)

    override fun deserialize(decoder: Decoder): BitfinexCandle =
        BitfinexCandle(decoder.decodeSerializableValue(doubleSerializer))

}