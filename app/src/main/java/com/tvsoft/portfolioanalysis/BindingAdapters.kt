package com.tvsoft.portfolioanalysis

import android.widget.TextView
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
fun setText(view: TextView, value: Double, currency: String) {
        view.text = String.format("%.2f", value) + currency
}

/*@BindingAdapter("app:money")
fun setText2(view: TextView, value: Double) {
        view.text = String.format("%.2f", value)
}
*/