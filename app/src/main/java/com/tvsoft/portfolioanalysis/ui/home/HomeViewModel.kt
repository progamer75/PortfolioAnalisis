package com.tvsoft.portfolioanalysis.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tvsoft.portfolioanalysis.ExchangeRateAPI
import com.tvsoft.portfolioanalysis.TinkoffAPI
import com.tvsoft.portfolioanalysis.TinkoffDao
import kotlinx.coroutines.launch

class HomeViewModel(val tinkoffDao: TinkoffDao, application: Application) :
    AndroidViewModel(application) {

    fun onLoadAllData() { // TODO Добавить прогрессбар для отображения прогресса загрузки начальных данных
        //loadAllDataDone.value = false
        viewModelScope.launch {
            if(tinkoffDao.getNumMarketInstrument() < 100)
                TinkoffAPI.loadInstruments(tinkoffDao)
            tinkoffDao.deleteAllRates()
            ExchangeRateAPI.loadExchangeRates()
            tinkoffDao.deleteAllOperation()
            TinkoffAPI.loadAllData(tinkoffDao)

            //loadAllDataDone.value = true
        }
    }
}

class HomeViewModelFactory(
    private val tinkoffDao: TinkoffDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(tinkoffDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}