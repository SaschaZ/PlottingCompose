package dev.zieger.plottingcompose.di

import dev.zieger.exchange.dto.DataSource
import dev.zieger.exchange.dto.Input
import dev.zieger.plottingcompose.InputContainer
import dev.zieger.plottingcompose.ProcessingSource
import dev.zieger.utils.time.ITimeStamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.koin.dsl.module

fun inputModule(dataSource: DataSource<out Input>) = module {

    single<ProcessingSource<Input>>(PROCESSING_SOURCE) { ProcessingSourceImpl(dataSource) }
}

class ProcessingSourceImpl<out I : Input>(
    private val dataSource: DataSource<I>
) : ProcessingSource<I> {
    override fun input(range: ClosedRange<ITimeStamp>): Flow<InputContainer<I>> =
        dataSource.data(range).toInputContainer()

    private fun Flow<I>.toInputContainer(): Flow<InputContainer<I>> {
        var lastX: Input? = null
        var lastIdx = 0L

        return mapNotNull {
            val x = it.x
            when {
                lastX?.x == x || lastX == null -> {
                    lastX = it
                    InputContainer(it, lastIdx)
                }
                lastX?.let { lx -> lx.x.toDouble() < x.toDouble() } == true -> {
                    lastX = it
                    InputContainer(it, ++lastIdx)
                }
                else -> null
            }
        }
    }

}