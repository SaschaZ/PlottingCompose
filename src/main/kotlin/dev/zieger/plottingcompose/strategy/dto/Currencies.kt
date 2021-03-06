package dev.zieger.plottingcompose.strategy.dto

enum class Currencies(override val fullName: String, override val key: String) : Currency {
    XBT("Bitcoin", "XBT"),
    BTC("Bitcoin", "BTC"),
    USD("US Dollar", "USD"),
    XMR("Monero", "XMR");

    override fun toString(): String = key
}