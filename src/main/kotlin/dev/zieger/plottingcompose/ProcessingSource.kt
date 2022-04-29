package dev.zieger.plottingcompose

import dev.zieger.exchange.dto.Input
import dev.zieger.utils.time.ITimeStamp
import kotlinx.coroutines.flow.Flow

interface ProcessingSource<out I : Input> {

    fun input(range: ClosedRange<ITimeStamp>): Flow<InputContainer<I>>
}