package com.tvsoft.portfolioanalysis.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tvsoft.portfolioanalysis.R
import ru.tinkoff.invest.openapi.model.rest.PortfolioPosition

private const val TAG = "PortfolioAdapter"

class PortfolioAdapter: RecyclerView.Adapter<PortfolioAdapter.RowViewHolder>() {
    var data = listOf<PortfolioRow>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.portfolio_row, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val item = data[position]
        /*val balance = item.balance
        val lots = item.lots*/
        holder.name.text = "${item.name}"
        holder.profit.text = "${item.profit}"
        holder.dividends.text = "${item.dividends}"
        holder.tax.text = "${item.tax}"
        holder.profitWithoutTax.text = "${item.profitWithoutTax}"
    }

    override fun getItemCount(): Int = data.size

    class RowViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val profit: TextView = view.findViewById(R.id.profit)
        val dividends: TextView = view.findViewById(R.id.dividends)
        val tax: TextView = view.findViewById(R.id.tax)
        val profitWithoutTax: TextView = view.findViewById(R.id.profitWithoutTax)
    }
}