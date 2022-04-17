package dev.zieger.plottingcompose.strategy.dto

enum class Pairs(override val base: Currency, override val counter: Currency, override val key: String) : Pair {
    XBTUSD(Currencies.XBT, Currencies.USD, "${Currencies.XBT}${Currencies.USD}"),
    BTCUSD(Currencies.BTC, Currencies.USD, "${Currencies.BTC}${Currencies.USD}"),
    XMRUSD(Currencies.XMR, Currencies.USD, "${Currencies.XMR}${Currencies.USD}");

    override fun toString(): String = key
}