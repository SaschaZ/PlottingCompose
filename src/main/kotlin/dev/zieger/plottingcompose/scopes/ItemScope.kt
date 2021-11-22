package dev.zieger.plottingcompose.scopes

import dev.zieger.plottingcompose.Series

data class ItemScope(
    val set: (Series<*>) -> Unit,
    val add: (Series<*>) -> Unit = {}
)