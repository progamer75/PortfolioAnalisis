package com.tvsoft.portfolioanalysis.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.tvsoft.portfolioanalysis.TinkoffDao

class HomeViewModel(val tinkoffDao: TinkoffDao, application: Application) :
    AndroidViewModel(application) {

    private val _portfolioSum = MutableLiveData<Float>().apply {
        value = 123F
    }
    val portfolioSum: LiveData<Float> = _portfolioSum

    fun getPortfolioSumToString(): String {
        return portfolioSum.toString()
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