package com.pindou.app.domain.color

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CIEDE2000 色差公式
 * ISO/CIE 11664-6:2014 — 工业标准最接近人眼感知的色差算法
 * 用于印刷、纺织、拼豆调色。
 *
 * ΔE < 3: 几乎看不出差别
 * ΔE 3-8: 明显差别但可接受
 * ΔE > 8: 明显不同
 */
object ColorDifference {

    fun ciede2000(lab1: DoubleArray, lab2: DoubleArray): Double {
        val L1 = lab1[0]; val a1 = lab1[1]; val b1 = lab1[2]
        val L2 = lab2[0]; val a2 = lab2[1]; val b2 = lab2[2]

        val C1 = sqrt(a1 * a1 + b1 * b1)
        val C2 = sqrt(a2 * a2 + b2 * b2)
        val avgC = (C1 + C2) / 2.0
        val avgC7 = avgC.pow(7)
        val G = 0.5 * (1 - sqrt(avgC7 / (avgC7 + 25.0.pow(7))))

        val a1p = (1 + G) * a1
        val a2p = (1 + G) * a2
        val C1p = sqrt(a1p * a1p + b1 * b1)
        val C2p = sqrt(a2p * a2p + b2 * b2)
        val avgCp = (C1p + C2p) / 2.0

        val h1p = if (b1 == 0.0 && a1p == 0.0) 0.0 else atan2deg(b1, a1p)
        val h2p = if (b2 == 0.0 && a2p == 0.0) 0.0 else atan2deg(b2, a2p)

        val dLp = L2 - L1
        val dCp = C2p - C1p

        val dhpSmall = h2p - h1p
        val dhp = when {
            abs(dhpSmall) <= 180.0 -> dhpSmall
            dhpSmall > 180.0 -> dhpSmall - 360.0
            else -> dhpSmall + 360.0
        }
        val dHp = 2 * sqrt(C1p * C2p) * sin(deg2rad(dhp / 2))

        val avgLp = (L1 + L2) / 2.0
        val avgHp = when {
            C1p * C2p == 0.0 -> h1p + h2p
            abs(h1p - h2p) > 180.0 && (h1p + h2p) < 360.0 -> (h1p + h2p + 360.0) / 2.0
            abs(h1p - h2p) > 180.0 && (h1p + h2p) >= 360.0 -> (h1p + h2p - 360.0) / 2.0
            else -> (h1p + h2p) / 2.0
        }

        val T = 1 - 0.17 * cos(deg2rad(avgHp - 30)) +
                0.24 * cos(deg2rad(2 * avgHp)) +
                0.32 * cos(deg2rad(3 * avgHp + 6)) -
                0.20 * cos(deg2rad(4 * avgHp - 63))

        val dTheta = 30 * exp(-((avgHp - 275) / 25).pow(2))
        val RC = 2 * sqrt(avgCp.pow(7) / (avgCp.pow(7) + 25.0.pow(7)))
        val SL = 1 + (0.015 * (avgLp - 50).pow(2)) / sqrt(20 + (avgLp - 50).pow(2))
        val SC = 1 + 0.045 * avgCp
        val SH = 1 + 0.015 * avgCp * T
        val RT = -sin(deg2rad(2 * dTheta)) * RC

        val kL = 1.0; val kC = 1.0; val kH = 1.0

        val term1 = dLp / (kL * SL)
        val term2 = dCp / (kC * SC)
        val term3 = dHp / (kH * SH)

        return sqrt(term1 * term1 + term2 * term2 + term3 * term3 + RT * term2 * term3)
    }

    private fun atan2deg(y: Double, x: Double): Double {
        var deg = rad2deg(atan2(y, x))
        if (deg < 0) deg += 360.0
        return deg
    }

    private fun deg2rad(d: Double) = d * PI / 180.0
    private fun rad2deg(r: Double) = r * 180.0 / PI
}
