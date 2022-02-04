package com.tvsoft.portfolioanalysis

fun round2(num: Double): Double {
    return String.format("%.2f", num).toDouble()

    /*val df = DecimalFormat("#.###")
     df.roundingMode = RoundingMode.CEILING
     df.format(num).toDouble()
     */
}