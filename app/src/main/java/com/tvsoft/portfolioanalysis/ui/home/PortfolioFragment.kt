package com.tvsoft.portfolioanalysis.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tvsoft.portfolioanalysis.R
import com.tvsoft.portfolioanalysis.TinkoffAPI
import com.tvsoft.portfolioanalysis.TinkoffDB
import com.tvsoft.portfolioanalysis.databinding.FragmentHomeBinding
import com.tvsoft.portfolioanalysis.databinding.ItemPortfolioBinding

private const val ARG_PARAM1 = "portfolioNum"

/**
 * A simple [Fragment] subclass.
 * Use the [PortfolioFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PortfolioFragment : Fragment() {
    private var portfolioNum: Int = 0
    private lateinit var viewModel: PortfolioFragmentViewModel
    private var _binding: ItemPortfolioBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val application = requireNotNull(this.activity).application
        val viewModelFactory = PortfolioFragmentViewModelFactory(portfolioNum, application)
        val viewModel =
            ViewModelProvider(this, viewModelFactory)[PortfolioFragmentViewModel::class.java]

        _binding = ItemPortfolioBinding.inflate(inflater, container, false)
        binding.vm = viewModel
        val root: View = binding.root

        val portfolioAdapter = PortfolioAdapter()
        binding.portfolioRecyclerView.adapter = portfolioAdapter
        viewModel.activesList.observe(viewLifecycleOwner, Observer {
            it?.let{
                portfolioAdapter.data = it
            }
        })

        binding.lifecycleOwner = this

        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.item_portfolio, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(ARG_PARAM1) }?.apply {
            portfolioNum = getInt(ARG_PARAM1)
        }
    }
}

class PortfolioFragmentAdapter(fragment: Fragment, private val tinkoff_api: TinkoffAPI): FragmentStateAdapter(fragment) {
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