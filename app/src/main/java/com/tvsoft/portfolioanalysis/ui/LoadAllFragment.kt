package com.tvsoft.portfolioanalysis.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.tvsoft.portfolioanalysis.R
import com.tvsoft.portfolioanalysis.TinkoffDB
import com.tvsoft.portfolioanalysis.databinding.FragmentLoadAllBinding
import kotlinx.coroutines.launch

class LoadAllFragment : Fragment() {
    private lateinit var loadAllViewModel: LoadAllViewModel
    private var _binding: FragmentLoadAllBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val application = requireNotNull(activity).application
        val db = TinkoffDB.getDatabase(application)
        val viewModelFactory = LoadAllViewModelFactory(db.tinkoffDao, application)
        loadAllViewModel =
            ViewModelProvider(this, viewModelFactory)[LoadAllViewModel::class.java]

        _binding = FragmentLoadAllBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.vm = loadAllViewModel
        binding.lifecycleOwner = this
        loadAllViewModel.loadAllDataTitle.observe(viewLifecycleOwner, Observer { title ->
            title?.let {
                binding.title.text = it
            }
        })
        loadAllViewModel.loadAllDataProgress.observe(viewLifecycleOwner, Observer { progressBar ->
            progressBar?.let {
                binding.progressBar.progress = it
            }
        })
        loadAllViewModel.loadAllDataDone.observe(viewLifecycleOwner, Observer { done ->
            done?.let {
                val navController = requireActivity()
                    .findNavController(R.id.nav_host_fragment_activity_main)
                //val bundle = bundleOf("loadAll" to true)
                navController.popBackStack()
            }
        })
/*        arguments?.let {
            loadAll = it.getBoolean("load_all")
        }*/

        return root
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            loadAllViewModel.loadAllData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}