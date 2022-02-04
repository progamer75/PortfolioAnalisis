package com.tvsoft.portfolioanalysis.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.tvsoft.portfolioanalysis.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.tinkoff.invest.openapi.model.rest.InstrumentType
import java.time.LocalDate
import java.time.OffsetDateTime

private const val TAG = "PortfolioFragmentViewModel"

data class PortfolioRow(
    var name: String,
    var profit: Double = 0.0,
    var dividends: Double = 0.0,
    var tax: Double = 0.0,
    var earliestDate: LocalDate = LocalDate.now(),
    var averageRate: Double = 0.0, // средний курс на даты покупок
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
            var lastRate: Double = 0.0
            withContext(Dispatchers.IO) {
                lastRate = ExchangeRateAPI().getLastRate(instr.currency)
            }
            var earliestOffsetDate = OffsetDateTime.now()
            val posQuantity = pos.balance.toInt() //pos.lots * instr.lot
            val row = PortfolioRow(pos.name).apply{
                var buyQuantity: Int = 0
                var buySumRub: Double = 0.0
                val buys = tinkoffDao.getOperations(portfolioNum, pos.figi)
                var commission: Long = 0
                var buySum: Double = 0.0
                for(buy in buys) {
                    val kurs = tinkoffDao.getRate(buy.currency, buy.date.toLocalDate())
                    commission += buy.commission
                    buyQuantity += buy.quantity

                    earliestOffsetDate = buy.date
                    //Log.i(TAG, "${instr.name} / $kurs / ${buy.payment} / ${buy.quantity}")

                    if(buyQuantity > posQuantity) {
                        val price: Double = buy.payment.toDouble() / buy.quantity
                        buySumRub += price * posQuantity * kurs
                        buySum += price * posQuantity
                        break
                    }
                    buySumRub += buy.payment * kurs
                    buySum += buy.payment
                    if(buyQuantity == posQuantity)
                        break
                }
                // TODO добавить доходность
                buySumRub = -buySumRub / 100
                buySum = -buySum / 100
                averageRate = if(instr.currency == CurrenciesDB.RUB)
                    1.0
                else
                    round2(buySumRub / buySum)

                profit = pos.expectedYield.value.toDouble() // + commission
                val commissionRub: Double = -commission.toDouble() / 100.0

                earliestDate = earliestOffsetDate.toLocalDate()
                dividends = (tinkoffDao.getDividends(portfolioNum, pos.figi, earliestOffsetDate).sumOf +
                        tinkoffDao.getDividendsTax(portfolioNum, pos.figi, earliestOffsetDate).sumOf).toDouble() / 100.0

                val price = tinkoffApi.getPriceByFigi(instr.figi)
                if(lastRate > 0.000001) {
                    val sellSumRub = posQuantity * price * lastRate
                    //Log.i(TAG, "$price / $lastRate / $sellSumRub / $buySumRub")
                    tax = round2(if(sellSumRub > buySumRub) (sellSumRub - buySumRub) * 0.13 else 0.0) // в рублях
                    profitWithoutTax =
                        round2(profit + dividends - (commissionRub + tax) / lastRate)  //pos.averagePositionPrice?.value?.toDouble() ?: 0.0
                }
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