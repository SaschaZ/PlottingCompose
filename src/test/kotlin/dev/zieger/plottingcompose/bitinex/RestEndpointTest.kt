package dev.zieger.plottingcompose.bitinex

import dev.zieger.plottingcompose.bitinex.BitfinexInterval.H1
import dev.zieger.plottingcompose.bitinex.BitfinexSymbol.BTC
import dev.zieger.plottingcompose.bitinex.BitfinexSymbol.USD
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.AnnotationSpec

class RestEndpointTest : AnnotationSpec() {

    @Test
    fun testCandles() = runBlocking {
        RestEndpoint().candles(BitfinexPair(BTC, USD), H1, limit = 200).collect {
            println(it)
        }
    }
}
