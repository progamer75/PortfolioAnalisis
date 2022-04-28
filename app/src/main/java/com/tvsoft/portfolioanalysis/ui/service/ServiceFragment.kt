package com.tvsoft.portfolioanalysis.ui.service

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.tvsoft.portfolioanalysis.TinkoffDB
import com.tvsoft.portfolioanalysis.databinding.FragmentServiceBinding

class ServiceFragment : Fragment() {
    val TAG: String = "ServiceFragment"
    //private val viewModel: PAViewModel by activityViewModels<PAViewModel>()
    private lateinit var serviceViewModel: ServiceViewModel
    private var _binding: FragmentServiceBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val application = requireNotNull(activity).application
        val db = TinkoffDB.getDatabase(application)

        val viewModelFactory = ServiceViewModelFactory(db.tinkoffDao, application)
        serviceViewModel =
            ViewModelProvider(this, viewModelFactory)[ServiceViewModel::class.java]

        _binding = FragmentServiceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.vm = serviceViewModel
        binding.lifecycleOwner = this
        serviceViewModel.loadAllDataDone.observe(viewLifecycleOwner, Observer { status ->
            status?.let {
                if(status) {
                    val toast = Toast.makeText(application, "Данные загружены", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        })

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}