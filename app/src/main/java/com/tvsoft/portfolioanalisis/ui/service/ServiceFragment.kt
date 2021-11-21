package com.tvsoft.portfolioanalisis.ui.service

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.tvsoft.portfolioanalisis.R
import com.tvsoft.portfolioanalisis.databinding.FragmentServiceBinding

class ServiceFragment : Fragment() {

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
        serviceViewModel =
            ViewModelProvider(this).get(ServiceViewModel::class.java)

        _binding = FragmentServiceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        serviceViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        val btnLoadInstruments: Button = binding.loadInstruments
        btnLoadInstruments.setOnClickListener {item ->

        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}