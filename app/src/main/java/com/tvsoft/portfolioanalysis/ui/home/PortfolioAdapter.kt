package com.tvsoft.portfolioanalysis.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tvsoft.portfolioanalysis.R
import com.tvsoft.portfolioanalysis.TinkoffAPI
import ru.tinkoff.invest.openapi.model.rest.PortfolioPosition

class TextItemViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView)

//data class

class PortfolioAdapter: RecyclerView.Adapter<TextItemViewHolder>() {
    var data = listOf<PortfolioPosition>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.portfolio_row, parent, false) as TextView
        return TextItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: TextItemViewHolder, position: Int) {
        val item = data[position]
        holder.textView.text = "${item.name} - ${item.balance}"
    }

    override fun getItemCount(): Int = data.size
}