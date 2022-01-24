package com.tvsoft.portfolioanalysis.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.tvsoft.portfolioanalysis.TinkoffAPI
import com.tvsoft.portfolioanalysis.TinkoffDao
import ru.tinkoff.invest.openapi.model.rest.PortfolioPosition

class PortfolioFragmentViewModel(private val portfolioNum: Int, application: Application) :
    AndroidViewModel(application) {
    private val tinkoffApi = TinkoffAPI.getInstance()
    var activesList: MutableLiveData<List<PortfolioPosition>> = MutableLiveData<List<PortfolioPosition>>()
    init {
        activesList.value = tinkoffApi.portfolios[portfolioNum].positions
    }

}

class PortfolioFragmentViewModelFactory(private val portfolioNum: Int,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioFragmentViewModel::class.java)) {
            return PortfolioFragmentViewModel(portfolioNum, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}