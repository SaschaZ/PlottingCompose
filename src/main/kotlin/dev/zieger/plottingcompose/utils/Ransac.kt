package dev.zieger.plottingcompose.utils

import java.util.*

object Ransac {
    private val random = Random()

    // RANDPERM(N,K) returns a vector of K unique values. This is sometimes
    // referred to as a K-permutation of 1:N or as sampling without replacement.
    private fun randPerm(N: Int, K: Int): Set<Int> {
        val res: MutableSet<Int> = LinkedHashSet() // unsorted set.
        while (res.size < K) {
            res.add(random.nextInt(N)) // [0, number-1]
        }
        return res
    }

    private fun norm(vec: List<Double>): Double {
        return Math.sqrt(Math.pow(vec[0], 2.0) + Math.pow(vec[1], 2.0))
    }

    private fun findLessThan(distance: List<Double>, threshDist: Double): List<Int> {
        val res: MutableList<Int> = ArrayList()
        for (i in distance.indices) {
            val dist = distance[i]
            if (Math.abs(dist) <= threshDist) {
                res.add(i)
            }
        }
        return res
    }

    fun perform(
        data_Y: List<Double>,
        num: Int = 2,
        iter: Int = 1000,
        threshDist: Double = 1.0,
        inlierRatio: Double = 0.9
    ): List<Double> {
        val number = data_Y.size
        val data_X: MutableList<Int> = ArrayList()
        for (i in 0 until number) {
            data_X.add(i + 1)
        }
        var bestInNum = 0.0
        var bestParameter1 = 0.0
        var bestParameter2 = 0.0
        for (i in 0 until iter) {
            val idx = randPerm(number, num)
            val sample_X: MutableList<Int> = ArrayList()
            val sample_Y: MutableList<Double> = ArrayList()
            for (idxVal in idx) {
                sample_X.add(data_X[idxVal])
                sample_Y.add(data_Y[idxVal])
            }
            val kLine: MutableList<Double> = ArrayList()
            kLine.add((sample_X[1] - sample_X[0]).toDouble())
            kLine.add(sample_Y[1] - sample_Y[0])
            val kLineNorm: MutableList<Double> = ArrayList()
            val norm = norm(kLine)
            kLineNorm.add(kLine[0] / norm)
            kLineNorm.add(kLine[1] / norm)
            val normVector: MutableList<Double> = ArrayList()
            normVector.add(-kLineNorm[1])
            normVector.add(kLineNorm[0])
            val distance: MutableList<Double> = ArrayList()
            for (j in 0 until number) {
                var distTmp = normVector[0] * (data_X[j] - sample_X[0])
                distTmp += normVector[1] * (data_Y[j] - sample_Y[0])
                distance.add(distTmp)
            }
            val inlierIdx = findLessThan(distance, threshDist)
            val inlierNum = inlierIdx.size
            var parameter1 = 0.0
            var parameter2 = 0.0
            if (inlierNum >= Math.round(inlierRatio * number) && inlierNum > bestInNum) {
                bestInNum = inlierNum.toDouble()
                parameter1 = (sample_Y[1] - sample_Y[0]) / (sample_X[1] - sample_X[0])
                parameter2 = sample_Y[0] - parameter1 * sample_X[0]
                bestParameter1 = parameter1
                bestParameter2 = parameter2
            }
        }
        val res: MutableList<Double> = ArrayList()
        res.add(bestParameter1)
        res.add(bestParameter2)
        return res
    }
}