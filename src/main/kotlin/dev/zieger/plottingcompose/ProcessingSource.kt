package dev.zieger.plottingcompose

import dev.zieger.plottingcompose.definition.Input
import dev.zieger.utils.time.ITimeStamp
import kotlinx.coroutines.flow.Flow

interface ProcessingSource<I : Input> {

    fun input(range: ClosedRange<ITimeStamp>): Flow<InputContainer<I>>
}