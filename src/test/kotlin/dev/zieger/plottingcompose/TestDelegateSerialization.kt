package dev.zieger.plottingcompose

import dev.zieger.utils.time.ITimeStamp
import dev.zieger.utils.time.TimeStamp
import dev.zieger.utils.time.timeSerializerModule
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TestDelegateSerialization : AnnotationSpec() {

    private val json = Json { serializersModule = timeSerializerModule }

    @Test
    fun testDelegatedSerialization() {
        val source = TestClass("FooBoo", TimeStamp())
        val str = json.encodeToString(source)
        val destination = json.decodeFromString<TestClass>(str)
        destination shouldBe source
        destination.startOfMonth shouldBe source.startOfMonth
    }

}

@Serializable
data class TestClass(
    val name: String,
    val time: ITimeStamp
) : ITimeStamp by time