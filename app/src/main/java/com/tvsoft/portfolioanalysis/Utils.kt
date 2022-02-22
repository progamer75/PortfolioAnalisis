package com.tvsoft.portfolioanalysis

import java.math.RoundingMode
import java.text.DecimalFormat

interface Utils {
    fun round2(num: Double): Double
    fun moneyFormat(num: Double, cur: CurrenciesDB): String
    fun percentFormat(num: Double): String
    fun moneyConvert(num: Double, from: CurrenciesDB, fromRate: Double, to: CurrenciesDB, toRate: Double): Double

    companion object : Utils {
        override fun round2(num: Double): Double {
            //return String.format("%.2f", num).toDouble()

            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.HALF_UP
            var res = df.format(num)
            res = res.replace(",", ".")
            return res.toDouble()
        }

        override fun moneyFormat(num: Double, cur: CurrenciesDB): String {
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.HALF_UP
            return df.format(num) + cur.symbol()
        }

        override fun percentFormat(num: Double): String {
            val df = DecimalFormat("#")
            df.roundingMode = RoundingMode.HALF_UP
            return df.format(num)
        }

        inline override fun moneyConvert(
            num: Double,
            from: CurrenciesDB,
            fromRate: Double,
            to: CurrenciesDB,
            toRate: Double
        ): Double {
            return if(from == to)
                num
            else
                num * fromRate / toRate
        }
    }
}

enum class Periods(a: Int) {
    Year(0), Month(1), Quarter(2), AllTime(3)
}