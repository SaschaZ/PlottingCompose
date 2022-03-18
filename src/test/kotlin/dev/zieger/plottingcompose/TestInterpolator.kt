//package dev.zieger.plottingcompose
//
//import io.kotest.core.spec.style.AnnotationSpec
//
//class TestInterpolator : AnnotationSpec() {
//
//    @Test
//    fun testLinear() {
//        LinearInterpolator.test()
//    }
//
//    @Test
//    fun testEaseIn() {
//        EaseInterpolator.test()
//    }
//
//    @Test
//    fun testEaseInOut() {
//        (Flip() * EaseIn() * Flip()).test()
//    }
//
//    private fun Interpolator.test() {
//        ((0..500) step 33).map { it / 500.0 }.forEach {
//            println(((0..(interpolate(it) * 20).toInt()).joinToString("") { " " } + "#"))
//        }
//    }
//}