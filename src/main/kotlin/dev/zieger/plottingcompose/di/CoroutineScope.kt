package dev.zieger.plottingcompose.di

import dev.zieger.plottingcompose.di.Scope.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.dsl.module

enum class Scope(val qualifier: Qualifier) : Qualifier by qualifier {
    DEFAULT(named("DEFAULT")),
    IO(named("IO")),
    MAIN(named("MAIN"))
}

fun coroutineModule() = module {

    single(DEFAULT) { CoroutineScope(Dispatchers.Default) }
    single(IO) { CoroutineScope(Dispatchers.IO) }
    single(MAIN) { CoroutineScope(Dispatchers.Main) }
}