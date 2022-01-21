package com.tvsoft.portfolioanalysis.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.tvsoft.portfolioanalysis.PAViewModel
import com.tvsoft.portfolioanalysis.TinkoffDB
import com.tvsoft.portfolioanalysis.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private val TAG = "HomeFragment"
    private val viewModel: PAViewModel by activityViewModels<PAViewModel>()
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val application = requireNotNull(this.activity).application
        val operationsDao = TinkoffDB.getDatabase(application).tinkoffDao
        val viewModelFactory = HomeViewModelFactory(operationsDao, application)
        homeViewModel =
            ViewModelProvider(this, viewModelFactory)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        //val recycleView: RecyclerView = binding.portfolioRecyclerView
/*        val tvPortfolioSum = binding.tvSum
        homeViewModel.portfolioSum.observe(viewLifecycleOwner, Observer {
            tvPortfolioSum.text = it.toString()
        })*/

        //TODO Все что связано с tinkoff_api надо заполнять после ввода тикета
        var adapter = PortfolioAdapter(this, viewModel.tinkoff_api)
        var pager = binding.pager
        pager.adapter = adapter
        val tab_pager = binding.tabPager
        TabLayoutMediator(tab_pager, pager) {tab, pos ->
            tab.text = "${viewModel.tinkoff_api.getPortfolioName(pos)}"
        }.attach()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}