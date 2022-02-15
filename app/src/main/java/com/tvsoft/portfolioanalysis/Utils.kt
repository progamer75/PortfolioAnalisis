package com.tvsoft.portfolioanalysis

interface Utils {
    fun round2(num: Double): Double

    companion object : Utils {
        override fun round2(num: Double): Double {
            return String.format("%.2f", num).toDouble()

            /*val df = DecimalFormat("#.###")
             df.roundingMode = RoundingMode.CEILING
             df.format(num).toDouble()
             */
        }
    }
}