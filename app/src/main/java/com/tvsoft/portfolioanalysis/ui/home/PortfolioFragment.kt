package com.tvsoft.portfolioanalysis.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tvsoft.portfolioanalysis.R
import com.tvsoft.portfolioanalysis.TinkoffAPI

private const val ARG_PARAM1 = "param1"

/**
 * A simple [Fragment] subclass.
 * Use the [PortfolioFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PortfolioFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.item_portfolio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(ARG_PARAM1) }?.apply {
            /*val textView: TextView = view.findViewById(android.R.id.text1)
            textView.text = getInt(ARG_PARAM1).toString()*/
        }
    }
}

class PortfolioAdapter(fragment: Fragment, val tinkoff_api: TinkoffAPI): FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return tinkoff_api.portfolios.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = PortfolioFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_PARAM1, position + 1)
        }

        return fragment
    }

}