package com.tvsoft.portfolioanalysis.ui.service

import android.app.Application
import android.view.View
import androidx.lifecycle.*
import androidx.navigation.findNavController
import com.tvsoft.portfolioanalysis.ExchangeRateAPI
import com.tvsoft.portfolioanalysis.R
import com.tvsoft.portfolioanalysis.TinkoffAPI
import com.tvsoft.portfolioanalysis.TinkoffDao
import kotlinx.coroutines.launch

class ServiceViewModel(private val tinkoffDao: TinkoffDao,
                       application: Application) :
    AndroidViewModel(application) {

    private val TAG = "ServiceViewModel"

    private val _text = MutableLiveData<String>().apply {
        value = "This is service Fragment"
    }
    val text: LiveData<String> = _text
    var loadAllDataDone = MutableLiveData<Boolean?>()
/*
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
    Если я правильно понял, то вот это все не нужно если вместо uiScope использовать viewModelScope
*/

    init {
    }

    fun fillPortfolios() {
        val api = TinkoffAPI.api
        for(account in TinkoffAPI.userAccounts) {

        }
    }

    fun onLoadInstruments() {
        viewModelScope.launch {
            TinkoffAPI.loadInstruments(tinkoffDao)
        }
    }

    fun onSettings(view: View) {
        val navController = view.findNavController().navigate(R.id.action_global_settings_fragment)
    }

    fun onLoadAllData() { // TODO Добавить прогрессбар для отображения прогресса загрузки начальных данных
        loadAllDataDone.value = false
        viewModelScope.launch {
            tinkoffDao.deleteAllOperation()
            tinkoffDao.deleteAllRates()
            TinkoffAPI.loadInstruments(tinkoffDao)
            TinkoffAPI.loadAllData(tinkoffDao)
            ExchangeRateAPI.loadExchangeRates()
            loadAllDataDone.value = true
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            tinkoffDao.deleteAllOperation()
        }
    }

    fun onLoadExchangeRates() {
        viewModelScope.launch {
            ExchangeRateAPI.loadExchangeRates()
        }
    }
}

class ServiceViewModelFactory(
    private val tinkoffDao: TinkoffDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ServiceViewModel::class.java)) {
            return ServiceViewModel(tinkoffDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}