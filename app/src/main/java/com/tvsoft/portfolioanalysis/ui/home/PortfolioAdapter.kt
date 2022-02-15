package com.tvsoft.portfolioanalysis.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import android.widget.TextView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tvsoft.portfolioanalysis.databinding.HeaderItemBinding
import com.tvsoft.portfolioanalysis.databinding.PortfolioRowBinding

private const val TAG = "PortfolioAdapter"

class PortfolioAdapter(private val header: Boolean): ListAdapter<PortfolioRow, RecyclerView.ViewHolder>(PortfolioRawDiffCallback())//RecyclerView.Adapter<PortfolioAdapter.RowViewHolder>()
{
/*    var data = listOf<PortfolioRow>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
*/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(header) HeaderViewHolder.from(parent)
            else RowViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)//data[position]
        if(header)
            (holder as HeaderViewHolder).bind(item)
        else
            (holder as  RowViewHolder).bind(item)
    }

 //   override fun getItemCount(): Int = data.size

    class RowViewHolder private constructor(val binding: PortfolioRowBinding): RecyclerView.ViewHolder(binding.root) {
        val res = itemView.context.resources

        fun bind(item: PortfolioRow) {
            binding.row = item
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): RowViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                //val view = layoutInflater.inflate(R.layout.portfolio_row, parent, false)
                val binding = PortfolioRowBinding.inflate(layoutInflater, parent, false)
                return RowViewHolder(binding)
            }
        }
    }

    class HeaderViewHolder private constructor(val binding: HeaderItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PortfolioRow) {
            binding.row = item
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                //val view = layoutInflater.inflate(R.layout.header_item, parent, false)
                val binding = HeaderItemBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }
}

class PortfolioRawDiffCallback : DiffUtil.ItemCallback<PortfolioRow>() {
    override fun areItemsTheSame(oldItem: PortfolioRow, newItem: PortfolioRow): Boolean {
        return oldItem.figi == newItem.figi
    }

    override fun areContentsTheSame(oldItem: PortfolioRow, newItem: PortfolioRow): Boolean {
        return oldItem == newItem
    }

}