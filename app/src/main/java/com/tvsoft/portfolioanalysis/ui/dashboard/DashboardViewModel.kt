package com.tvsoft.portfolioanalysis.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvsoft.portfolioanalysis.PortfolioAnalysisApplication.Companion.context
import com.tvsoft.portfolioanalysis.TinkoffDB
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    init{
        viewModelScope.launch {
            click()
        }
    }

    fun onClick() {
        viewModelScope.launch {
            click()
        }
    }

    private suspend fun click() {
        val tinkoffDao = TinkoffDB.getDatabase(context!!).tinkoffDao
        val list = tinkoffDao.getAllOperation(0)
        _text.value = ""
        list.forEach {
            _text.value.plus(it.toString())
        }
    }
}