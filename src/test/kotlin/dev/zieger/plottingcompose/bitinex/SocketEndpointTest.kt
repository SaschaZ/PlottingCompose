package dev.zieger.plottingcompose.bitinex

import dev.zieger.plottingcompose.bitinex.BitfinexInterval.M1
import dev.zieger.plottingcompose.bitinex.BitfinexSymbol.USD
import dev.zieger.plottingcompose.bitinex.BitfinexSymbol.XMR
import io.kotest.core.spec.style.AnnotationSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class SocketEndpointTest : AnnotationSpec() {

    @Test
    fun testCandles() = runBlocking(Dispatchers.IO) {
        SocketEndpoint(this).candles(BitfinexPair(XMR, USD), M1).collect {
            println(it)
        }
        delay(60_000L)
    }
}
