package dev.zieger.plottingcompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.*
import dev.zieger.bybitapi.ByBitExchange
import dev.zieger.bybitapi.dto.enumerations.Interval
import dev.zieger.bybitapi.dto.enumerations.Symbol
import dev.zieger.bybitapi.utils.plus
import dev.zieger.plottingcompose.bitinex.*
import dev.zieger.plottingcompose.bitinex.BitfinexInterval.H1
import dev.zieger.plottingcompose.bitinex.BitfinexSymbol.USD
import dev.zieger.plottingcompose.bitinex.BitfinexSymbol.XMR
import dev.zieger.plottingcompose.definition.*
import dev.zieger.plottingcompose.definition.TickHelper.ticksIdx
import dev.zieger.plottingcompose.definition.TickHelper.timeFormat
import dev.zieger.plottingcompose.indicators.candles.*
import dev.zieger.plottingcompose.strategy.BbStrategy
import dev.zieger.plottingcompose.strategy.BbStrategyOptions
import dev.zieger.plottingcompose.strategy.Strategy
import dev.zieger.plottingcompose.strategy.dto.Pair
import dev.zieger.plottingcompose.strategy.dto.Pairs
import dev.zieger.plottingcompose.styles.*
import dev.zieger.plottingcompose.utils.Spinner
import dev.zieger.plottingcompose.utils.SpinnerColors
import dev.zieger.utils.time.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun buildByBitFlow(
    scope: CoroutineScope,
    symbol: Symbol = Symbol.BTCUSD,
    interval: Interval = Interval.H1,
    barsBack: Int = 10_000
) =
    ByBitExchange(scope).candles(symbol, interval, barsBack)
        .map { Ohcl.Companion.Ohcl(it.open, it.high, it.close, it.low, it.volume.toDouble(), it.openTime) }

fun buildBitfinexFlow(
    scope: CoroutineScope,
    pair: BitfinexPair = BitfinexPair(XMR, USD),
    interval: BitfinexInterval = H1,
    barsBack: Int = 10_000
) = RestEndpoint().candles(pair, interval, limit = barsBack)
    .plus(SocketEndpoint(scope).candles(pair, interval))

fun main() = application {
    MaterialTheme(MaterialTheme.colors.copy(background = Color.Black, onBackground = Color.White)) {
        val scope = rememberCoroutineScope()

        val byBitFlow = remember { mutableStateOf<Flow<ICandle>>(buildByBitFlow(scope)) }
        val finexFlow = remember { mutableStateOf<Flow<ICandle>>(buildBitfinexFlow(scope)) }

        val chartDefinition = remember {
            val bbOptions = BollingerBandsParameter(20, 2.0, AverageType.SMA)
            val bbKey = BollingerBands.key(bbOptions)
            val bbStrategyKey = BbStrategy.key(BbStrategyOptions(bbOptions))

            ChartDefinition(
                Chart(
                    LineSeries(
                        bbKey with BollingerBands.HIGH,
                        Color.Yellow.copy(alpha = 0.5f), 1f
                    ),
                    LineSeries(
                        bbKey with BollingerBands.MID,
                        Color.Yellow.copy(alpha = 0.5f), 1f
                    ),
                    LineSeries(
                        bbKey with BollingerBands.LOW,
                        Color(0xFF7700).copy(alpha = 0.5f), 1f
                    ),
                    FillBetween(
                        (bbKey with BollingerBands.HIGH) to
                                (bbKey with BollingerBands.LOW),
                        Color.Yellow.copy(alpha = 0.2f)
                    ),
                    SingleFocusable(
                        CandleSticks(
                            Ohcl.key() with Ohcl.OHCL,
                            positiveColor = Color(0xFF39a59a),
                            negativeColor = Color(0xFFe95751)
                        ), CandleSticks(
                            Ohcl.key() with Ohcl.OHCL,
                            positiveColor = Color(0xFF00FF00),
                            negativeColor = Color(0xFFFF0000)
                        )
                    ),
                    *(0..bbStrategyKey.param.dcaNumMax).map { idx ->
                        LineSeries(
                            bbStrategyKey with Strategy.BULL_BUY_ORDERS(idx),
                            color = Color.Cyan, width = 0.5f
                        )
                    }.toTypedArray(),
                    *(0..bbStrategyKey.param.dcaNumMax).map { idx ->
                        LineSeries(
                            bbStrategyKey with Strategy.BEAR_SELL_ORDERS(idx),
                            color = Color.Magenta, width = 0.5f
                        )
                    }.toTypedArray(),
                    verticalWeight = 0.8f,
                    drawXLabels = false,
                    xTicks = { idxRange, xRange ->
                        idxRange.ticksIdx(chartSize.value.width, 150f).timeFormat(xRange, idxRange)
                    }
                ),
                Chart(
                    SingleFocusable(
                        Impulses(
                            Volume.key() with Volume.VOLUME,
                            positiveColor = Color(0xFF39a59a),
                            negativeColor = Color(0xFFe95751)
                        ),
                        Impulses(
                            Volume.key() with Volume.VOLUME,
                            positiveColor = Color(0xFF00FF00),
                            negativeColor = Color(0xFFFF0000)
                        )
                    ),
                    verticalWeight = 0.2f,
                    yTicks = {
                        TickHelper.ticksY(it, chartSize.value.height, 250f)
                    },
                    xTicks = { idxRange, xRange ->
                        idxRange.ticksIdx(chartSize.value.width, 150f).timeFormat(xRange, idxRange)
                    }
                ),
                visibleArea = VisibleArea(0.8f, NumData.Fixed(300))
            )
        }

        val ctrlPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        val shiftPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        val altPressed: MutableState<Boolean> = remember { mutableStateOf(false) }
        Window(
            onCloseRequest = ::exitApplication, onKeyEvent = {
                ctrlPressed.value = it.isCtrlPressed
                shiftPressed.value = it.isShiftPressed
                altPressed.value = it.isAltPressed
                false
            }, state = rememberWindowState(
                position = WindowPosition(Alignment.TopStart),
                placement = WindowPlacement.Maximized
            )
        ) {
            Column(Modifier.background(Color.Black)) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    TopSelection(chartDefinition, CandleProvider.ByBit, 0, Modifier.fillMaxWidth(0.5f)) {
//                        byBitFlow.value = it
//                    }
                    TopSelection(chartDefinition, CandleProvider.Bitfinex, 0) {
                        finexFlow.value = it
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    MultiChart(
//                        chartDefinition,
//                        byBitFlow.value,
//                        modifier = Modifier.fillMaxWidth(0.5f)
//                    )
                    MultiChart(
                        chartDefinition,
                        finexFlow.value,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TopSelection(
    chartDefinition: ChartDefinition<*>,
    provider: CandleProvider,
    initialPairIdx: Int,
    modifier: Modifier = Modifier,
    onChange: (Flow<ICandle>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val spinnerColors = remember { SpinnerColors(Color.White, Color.White, Color(0xFF292929), Color.White) }
    Row(
        modifier.padding(
            start = chartDefinition.margin.left(IntSize.Zero),
            end = chartDefinition.margin.right(IntSize.Zero),
            top = chartDefinition.margin.top(IntSize.Zero)
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val pairSpinnerOpen = remember { mutableStateOf(false) }
        val pairSpinnerItems = remember {
            mutableStateListOf(*provider.providedPairs.map { it.base.key to it.counter.key }.toTypedArray())
        }
        val pairSpinnerSelected = remember { mutableStateOf(initialPairIdx) }
        val intervalSpinnerOpen = remember { mutableStateOf(false) }
        val intervalSpinnerItems = remember { mutableStateListOf(1.minutes, 5.minutes, 1.hours, 1.days) }
        val intervalSpinnerSelected = remember { mutableStateOf(2) }

        Spinner(true, pairSpinnerOpen, pairSpinnerItems, pairSpinnerSelected, "", spinnerColors) { _, _ ->
            onChange(
                provider.build(
                    scope,
                    pairSpinnerItems[pairSpinnerSelected.value].first,
                    pairSpinnerItems[pairSpinnerSelected.value].second,
                    intervalSpinnerItems[intervalSpinnerSelected.value]
                )
            )
        }

        Spinner(
            true,
            intervalSpinnerOpen,
            intervalSpinnerItems,
            intervalSpinnerSelected,
            "",
            spinnerColors
        ) { _, _ ->
            onChange(
                provider.build(
                    scope,
                    pairSpinnerItems[pairSpinnerSelected.value].first,
                    pairSpinnerItems[pairSpinnerSelected.value].second,
                    intervalSpinnerItems[intervalSpinnerSelected.value]
                )
            )
        }
    }
}

enum class CandleProvider(
    val providedPairs: List<Pair>,
    val build: (scope: CoroutineScope, base: String, counter: String, interval: ITimeSpan) -> Flow<ICandle>
) {
    ByBit(listOf(Pairs.XBTUSD), { scope, base, counter, interval ->
        buildByBitFlow(
            scope,
            Symbol.valueOf("${base.uppercase()}${counter.uppercase()}"),
            Interval.values().minByOrNull { (it.duration - interval).abs }!!
        )
    }),
    Bitfinex(listOf(Pairs.XMRUSD), { scope, base, counter, interval ->
        println("Bitfinex: new flow for base=$base and counter=$counter with interval=$interval")
        buildBitfinexFlow(
            scope,
            BitfinexPair(BitfinexSymbol.valueOf(base.uppercase()), BitfinexSymbol.valueOf(counter.uppercase())),
            BitfinexInterval.values().minByOrNull { (it.duration - interval).abs }!!
        )
    })
}
