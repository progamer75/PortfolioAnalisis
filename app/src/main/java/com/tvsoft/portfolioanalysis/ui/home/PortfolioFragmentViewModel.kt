package com.tvsoft.portfolioanalysis.ui.home

import android.app.Application
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.*
import com.tvsoft.portfolioanalysis.*
import com.tvsoft.portfolioanalysis.Utils.Companion.moneyConvert
import com.tvsoft.portfolioanalysis.Utils.Companion.moneyFormat
import com.tvsoft.portfolioanalysis.Utils.Companion.percentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.tinkoff.invest.openapi.model.rest.InstrumentType
import ru.tinkoff.invest.openapi.model.rest.MarketInstrumentList
import java.time.LocalDate
import java.time.OffsetDateTime

private const val TAG = "PortfolioFragmentViewModel"

data class PortfolioRow(
    val figi: String,
    val name: String,
    val currency: CurrenciesDB,
    var balance: Double = 0.0,
    var profit: Double = 0.0,
    var dividends: Double = 0.0,
    var tax: Double = 0.0,
    var earliestDate: LocalDate = LocalDate.now(), // самая ранняя дата покупки
    var averageRate: Double = 0.0, // средний курс на даты покупок
    var profitWithoutTax: Double = 0.0 // профит с дивидендами за вычетом налогов и комиссий
)

data class PortfolioStat(
    var balance: Double = 0.0, // текущая стоимость портфеля
    var profitOpen: Double = 0.0, // профит по открытым позициям с дивидендами за вычетом налогов и комиссий
    var profitOpenIrr: Double = 0.0,
)

class PortfolioFragmentViewModel(private val portfolioNum: Int, application: Application) :
    AndroidViewModel(application) {
    private val tinkoffDao = TinkoffDB.getDatabase(application).tinkoffDao
    private val tinkoffApi = TinkoffAPI.getInstance()
    val rowList = MutableLiveData<List<PortfolioRow>>()
    var profitMap = mutableMapOf<CurrenciesDB, Double>()
    var period: Periods = Periods.Year
    private var _lastUSDRate: Double = 1.0

    val lastUSDRate = MutableLiveData<String>()
    val portfolioBalanceRub = MutableLiveData<String>()
    val portfolioBalanceUsd = MutableLiveData<String>()

    val portfolioProfitRub = MutableLiveData<String>()
    val portfolioProfitUsd = MutableLiveData<String>()
    val portfolioProfitIrrRub = MutableLiveData<String>()
    val portfolioProfitIrrUsd = MutableLiveData<String>()

    val portfolioClosedProfitRub = MutableLiveData<String>()
    val portfolioClosedProfitUsd = MutableLiveData<String>()
    val portfolioClosedProfitIrrRub = MutableLiveData<String>()
    val portfolioClosedProfitIrrUsd = MutableLiveData<String>()

    val portfolioTotalProfitRub = MutableLiveData<String>()
    val portfolioTotalProfitUsd = MutableLiveData<String>()
    val portfolioTotalProfitIrrRub = MutableLiveData<String>()
    val portfolioTotalProfitIrrUsd = MutableLiveData<String>()

    private fun getPortfolioProfit(): String {
        var str = ""
/*        CurrenciesDB.values().forEach {
            val sum: Double = profitMap[it]!!
            if(kotlin.math.abs(sum) > 0.0001) {
                if(str != "")
                    str = "$str, "
                str += "$sum${it.symbol()}"
            }
        }
        Log.i(TAG, str)*/

        return "Прибыль = $str"
    }

    init{
        zeroProfitMap()
    }

    private fun zeroProfitMap() {
        CurrenciesDB.values().forEach {
            profitMap[it] = 0.0
        }
    }

    fun onPeriodSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        val newPeriod = Periods.values()[pos]
        if(period != newPeriod) {
            period = newPeriod
            onRefreshActivesList()
        }
    }

    fun onRefreshActivesList() {
        viewModelScope.launch {
            refreshActivesList()
        }
    }

    private suspend fun refreshActivesList() {
        zeroProfitMap()

        var _portfolioBalanceRub: Double = 0.0
        var _portfolioBalanceUsd: Double = 0.0

        var _portfolioProfitRub: Double = 0.0
        var _portfolioProfitUsd: Double = 0.0
        var _portfolioProfitIrrRub: Double = 0.0
        var _portfolioProfitIrrUsd: Double = 0.0

        var _portfolioClosedProfitRub: Double = 0.0
        var _portfolioClosedProfitUsd: Double = 0.0
        var _portfolioClosedProfitIrrRub: Double = 0.0
        var _portfolioClosedProfitIrrUsd: Double = 0.0

        var _portfolioTotalProfitRub: Double = 0.0
        var _portfolioTotalProfitUsd: Double = 0.0
        var _portfolioTotalProfitIrrRub: Double = 0.0
        var _portfolioTotalProfitIrrUsd: Double = 0.0

        _lastUSDRate =
            withContext(Dispatchers.IO) {
                ExchangeRateAPI().getLastRate(CurrenciesDB.USD)
            }
        lastUSDRate.value = "ЦБ: " + String.format("%.2f", _lastUSDRate) + " руб/$"

        val portfolioPositions = tinkoffApi.portfolios[portfolioNum].positions
        val list = mutableListOf<PortfolioRow>()
        for(pos in portfolioPositions) {
            val posQuantity = pos.balance.toInt() //pos.lots * instr.lot
            if(pos.instrumentType == InstrumentType.CURRENCY) {
                continue
            }

            val instr = tinkoffDao.getMarketInstrument(pos.figi) ?:
                throw IllegalArgumentException("Не найден инструмент ${pos.figi}")

            val lastRate: Double =
                withContext(Dispatchers.IO) {
                    ExchangeRateAPI().getLastRate(instr.currency)
                }

            var earliestOffsetDate = OffsetDateTime.now()
            val row = PortfolioRow(pos.figi, pos.name, instr.currency).apply{
                var buyQuantity: Int = 0
                var buySumRub: Double = 0.0
                val buys = tinkoffDao.getOperations(portfolioNum, pos.figi)
                var commission: Long = 0
                var buySum: Double = 0.0
                for(buy in buys) {
                    val kurs = tinkoffDao.getRate(buy.currency, buy.date.toLocalDate()) ?: 1.0 // TODO если курс не найден надо подгрузить
                    commission += buy.commission
                    buyQuantity += buy.quantity

                    earliestOffsetDate = buy.date

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
                    Utils.round2(buySumRub / buySum)

                profit = pos.expectedYield.value.toDouble() // + commission
                profitMap[instr.currency] = profitMap[instr.currency]!! + profit

                val commissionRub: Double = -commission.toDouble() / 100.0

                earliestDate = earliestOffsetDate.toLocalDate()
                dividends = (tinkoffDao.getDividends(portfolioNum, pos.figi, earliestOffsetDate).sumOf +
                        tinkoffDao.getDividendsTax(portfolioNum, pos.figi, earliestOffsetDate).sumOf).toDouble() / 100.0

                val price = tinkoffApi.getPriceByFigi(instr.figi)
                balance = price * posQuantity
                val sellSumRub = balance * lastRate
                if(lastRate > 0.000001) {
                    tax = Utils.round2(if(sellSumRub > buySumRub) (sellSumRub - buySumRub) * 0.13 else 0.0) // в рублях
                    profitWithoutTax =
                        Utils.round2(profit + dividends - (commissionRub + tax) / lastRate)  //pos.averagePositionPrice?.value?.toDouble() ?: 0.0
                }

                _portfolioBalanceRub += sellSumRub
                _portfolioBalanceUsd += moneyConvert(balance, instr.currency, lastRate, CurrenciesDB.USD, _lastUSDRate)

                _portfolioProfitRub += profit * lastRate
                _portfolioProfitUsd += moneyConvert(profit, instr.currency, lastRate, CurrenciesDB.USD, _lastUSDRate)
            }
            list.add(row)
        }

        val curList = tinkoffApi.api.portfolioContext.getPortfolioCurrencies(tinkoffApi.userAccounts[portfolioNum].brokerAccountId).get().currencies
        for(cur in curList) {
            val sum = cur.balance.toDouble()
            val currency = CurrenciesDB.valueOf(cur.currency.value)
            val lastRate: Double =
                withContext(Dispatchers.IO) {
                    ExchangeRateAPI().getLastRate(currency)
                }
            when(currency) {
                CurrenciesDB.RUB -> _portfolioBalanceRub += sum
                CurrenciesDB.USD -> _portfolioBalanceUsd += sum
                else -> {
                    _portfolioBalanceRub += sum * lastRate
                    _portfolioBalanceUsd += sum * lastRate / _lastUSDRate
                }
            }

            val row = PortfolioRow("", currency.itName(), currency).apply{
                balance = sum
            }
            list.add(row)
        }

        rowList.value = list

        portfolioBalanceRub.value = moneyFormat(_portfolioBalanceRub, CurrenciesDB.RUB)
        portfolioBalanceUsd.value = moneyFormat(_portfolioBalanceUsd, CurrenciesDB.USD)

        portfolioProfitRub.value = moneyFormat(_portfolioProfitRub, CurrenciesDB.RUB)
        portfolioProfitUsd.value = moneyFormat(_portfolioProfitUsd, CurrenciesDB.USD)
        portfolioProfitIrrRub.value = percentFormat(_portfolioProfitIrrRub)
        portfolioProfitIrrUsd.value = percentFormat(_portfolioProfitIrrUsd)

        portfolioClosedProfitRub.value = moneyFormat(_portfolioClosedProfitRub, CurrenciesDB.RUB)
        portfolioClosedProfitUsd.value = moneyFormat(_portfolioClosedProfitUsd, CurrenciesDB.USD)
        portfolioClosedProfitIrrRub.value = percentFormat(_portfolioClosedProfitIrrRub)
        portfolioClosedProfitIrrUsd.value = percentFormat(_portfolioClosedProfitIrrUsd)

        portfolioTotalProfitRub.value = moneyFormat(_portfolioTotalProfitRub, CurrenciesDB.RUB)
        portfolioTotalProfitUsd.value = moneyFormat(_portfolioTotalProfitUsd, CurrenciesDB.USD)
        portfolioTotalProfitIrrRub.value = percentFormat(_portfolioTotalProfitIrrRub)
        portfolioTotalProfitIrrUsd.value = percentFormat(_portfolioTotalProfitIrrUsd)
    }

    //TODO В заголовок добавить текущий курс ЦБ
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