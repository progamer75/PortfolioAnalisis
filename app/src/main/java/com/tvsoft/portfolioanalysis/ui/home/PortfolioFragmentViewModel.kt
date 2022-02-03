package com.tvsoft.portfolioanalysis.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.tvsoft.portfolioanalysis.MarketInstrumentDB
import com.tvsoft.portfolioanalysis.TinkoffAPI
import com.tvsoft.portfolioanalysis.TinkoffDB
import com.tvsoft.portfolioanalysis.TinkoffDao
import kotlinx.coroutines.launch
import ru.tinkoff.invest.openapi.model.rest.InstrumentType
import ru.tinkoff.invest.openapi.model.rest.PortfolioPosition

private const val TAG = "PortfolioFragmentViewModel"

data class PortfolioRow(
    var name: String,
    var profit: Double = 0.0,
    var dividends: Double = 0.0,
    var tax: Double = 0.0,
    var profitWithoutTax: Double = 0.0
){
}

class PortfolioFragmentViewModel(private val portfolioNum: Int, application: Application) :
    AndroidViewModel(application) {
    private val tinkoffDao = TinkoffDB.getDatabase(application).tinkoffDao
    private val tinkoffApi = TinkoffAPI.getInstance()
    val rowList = MutableLiveData<List<PortfolioRow>>()
    init {
    }

    fun onRefreshActivesList() {
        viewModelScope.launch {
            refreshActivesList()
        }
    }

    private suspend fun refreshActivesList() {
        val portfolioPositions = tinkoffApi.portfolios[portfolioNum].positions
        val list = mutableListOf<PortfolioRow>()
        for(pos in portfolioPositions) {
            if(pos.instrumentType == InstrumentType.CURRENCY)
                continue
            val instr = tinkoffDao.getMarketInstrument(pos.figi) ?:
                throw IllegalArgumentException("Не найден инструмент ${pos.figi}")

            val posQuantity = pos.lots * instr.lot // вроде это pos.balance.toDouble()
            val row = PortfolioRow(pos.name).apply{
            // profit
                var buyQuantity: Int = 0
                var buySumRub: Double = 0.0
                val buys = tinkoffDao.getOperations(portfolioNum, pos.figi)
                var commission: Double = 0.0
/*                for(buy in buys) {
                    commission += buy.commission
                    buyQuantity += buy.quantity
                    if(buyQuantity > posQuantity) {
                        val price: Double = buy.payment / buy.quantity
                        buySum += price * posQuantity
                        break
                    }
                    buySum += buy.payment
                    if(buyQuantity == posQuantity)
                        break
                }*/
                profit = pos.expectedYield.value.toDouble() // + commission
                dividends = 0.0
                tax = 0.0
                profitWithoutTax = -commission//pos.averagePositionPrice?.value?.toDouble() ?: 0.0

                // dividends

            }
            list.add(row)
        }

        rowList.value = list
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