package com.tvsoft.portfolioanalysis

import android.util.Log
import com.google.protobuf.Timestamp
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

private const val TAG = "Utils"

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
            if(res == "NaN")
                Log.i(TAG, "$res / $num")
            return res.toDouble()
        }

        override fun moneyFormat(num: Double, cur: CurrenciesDB): String {
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.HALF_UP
            return df.format(num) + cur.symbol()
        }

        override fun percentFormat(num: Double): String {
            val df = DecimalFormat("#.#")
            df.roundingMode = RoundingMode.HALF_UP
            return df.format(num) + "%"
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

        suspend fun moneyConvert(
            num: Double,
            from: CurrenciesDB,
            to: CurrenciesDB,
            date: LocalDate
        ): Double {
            val fromRate =
                if(from == CurrenciesDB.RUB)
                    1.0
                else
                    ExchangeRateAPI.getRate(from, date)
            val toRate =
                if(to == CurrenciesDB.RUB)
                    1.0
                else
                    ExchangeRateAPI.getRate(to, date)

            return if(from == to)
                num
            else
                num * fromRate / toRate
        }

        fun ts2LocalDate(ts: Timestamp): LocalDate =
            LocalDateTime.ofEpochSecond(ts.seconds, ts.nanos, ZoneOffset.UTC).toLocalDate()

        fun ts2OffsetDateTime(ts: Timestamp): OffsetDateTime =
            OffsetDateTime.of(LocalDateTime.ofEpochSecond(ts.seconds, ts.nanos, ZoneOffset.UTC), ZoneOffset.UTC)
    }
}

enum class Periods(a: Int) {
    Year(0), Month(1), Quarter(2), AllTime(3)
}