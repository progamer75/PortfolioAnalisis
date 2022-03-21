package com.tvsoft.portfolioanalysis

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@BindingAdapter("android:text")
fun setText(view: TextView, value: Double) {
    view.text = String.format("%.2f", value)
}

@BindingAdapter("android:text")
fun setText(view: TextView, value: LocalDate) {
    view.text = value.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}

@BindingAdapter(value = ["app:money", "app:currency"], requireAll = false)
fun setText(view: TextView, value: Double, currency: CurrenciesDB) {
    view.text = Utils.moneyFormat(value, currency)
    if(value < 0)
        view.setTextColor(ContextCompat.getColor(view.context, R.color.negative_color))
    else
        view.setTextColor(ContextCompat.getColor(view.context, R.color.text_color))
}

@BindingAdapter(value = ["app:quantity"], requireAll = false)
fun setText(view: TextView, value: Int) {
    view.text = "$value шт."
}

/*@BindingAdapter("app:money")
fun setText2(view: TextView, value: Double) {
        view.text = String.format("%.2f", value)
}
*/