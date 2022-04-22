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
import com.tvsoft.portfolioanalysis.businesslogic.BusinessLogic
import kotlinx.coroutines.launch
import ru.tinkoff.piapi.contract.v1.OperationType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
//TODO Добавить отчет по покупкам/продажам валюты
private const val TAG = "PortfolioFragmentViewModel"

data class PortfolioRow(
    val figi: String,
    val name: String,
    val currency: CurrenciesDB,
    var balance: Double = 0.0,
    var quantity: Int = 0,
    var profit: Double = 0.0,
    var dividends: Double = 0.0,
    var dividendsClosed: Double = 0.0,
    var tax: Double = 0.0,
    var earliestDate: LocalDate = LocalDate.now(), // самая ранняя дата покупки
    var averageRate: Double = 0.0, // средний курс на даты покупок
    var profitWithoutTax: Double = 0.0, // профит с дивидендами за вычетом налогов и комиссий
    var profitWithoutTaxClosed: Double = 0.0,

    var closed: Boolean = false
)

data class PortfolioStat(
    var balance: Double = 0.0, // текущая стоимость портфеля
    var profitOpen: Double = 0.0, // профит по открытым позициям с дивидендами за вычетом налогов и комиссий
    var profitOpenIrr: Double = 0.0
)

class PortfolioFragmentViewModel(private val portfolioNum: Int, application: Application) :
    AndroidViewModel(application) {
    private val tinkoffDao = TinkoffDB.getDatabase(application).tinkoffDao
    private val calcData = BusinessLogic(tinkoffDao, portfolioNum)
    val rowList = MutableLiveData<List<PortfolioRow>>()
    var profitMap = mutableMapOf<CurrenciesDB, Double>()
    var period: Periods = Periods.Year
    private var _lastUSDRate = 1.0

    val lastUSDRate = MutableLiveData<String>()
    val portfolioBalanceRub = MutableLiveData<String>()
    val portfolioBalanceUsd = MutableLiveData<String>()

    val portfolioOpenRub = MutableLiveData<String>()
    val portfolioOpenUsd = MutableLiveData<String>()
    val portfolioOpenIrrRub = MutableLiveData<String>()
    val portfolioOpenIrrUsd = MutableLiveData<String>()

    val portfolioClosedProfitRub = MutableLiveData<String>()
    val portfolioClosedProfitUsd = MutableLiveData<String>()
    val portfolioClosedProfitIrrRub = MutableLiveData<String>()
    val portfolioClosedProfitIrrUsd = MutableLiveData<String>()

    val portfolioTotalProfitRub = MutableLiveData<String>()
    val portfolioTotalProfitUsd = MutableLiveData<String>()
    val portfolioTotalIrrRub = MutableLiveData<String>()
    val portfolioTotalIrrUsd = MutableLiveData<String>()

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

        var _portfolioBalanceRub = 0.0
        var _portfolioBalanceUsd = 0.0 // Баланс на сейчас с учетом кэша

        var _portfolioOpenRub = 0.0
        var _portfolioOpenUsd = 0.0
        var _portfolioOpenIrrRub = 0.0
        var _portfolioOpenIrrUsd = 0.0

        var _portfolioClosedProfitRub = 0.0
        var _portfolioClosedProfitUsd = 0.0
        var _portfolioClosedProfitIrrRub = 0.0
        var _portfolioClosedProfitIrrUsd = 0.0

        var _portfolioTotalProfitRub = 0.0
        var _portfolioTotalProfitUsd = 0.0
        var _portfolioTotalIrrRub = 0.0
        var _portfolioTotalIrrUsd = 0.0

        // по закрытым сделкам:
        val daysAgo: Long =
            when(period) {
                Periods.AllTime -> 0
                Periods.Year -> 365
                Periods.Quarter -> 90
                Periods.Month -> 30
            }
        val firstDate:LocalDateTime = if(daysAgo == 0L)
            LocalDateTime.of(2010, 1, 1, 0, 0, 0, 0)
        else
            LocalDateTime.now().minusDays(daysAgo)
        val lastDate = LocalDateTime.now()
        val firstDateOffset = OffsetDateTime.of(firstDate, ZoneOffset.UTC)
        val lastDateOffset = OffsetDateTime.of(lastDate, ZoneOffset.UTC)

        _lastUSDRate = ExchangeRateAPI.getLastRate(CurrenciesDB.USD)
        lastUSDRate.value = "ЦБ: " + String.format("%.2f", _lastUSDRate) + " руб/$"

        val portfolioPositions = TinkoffAPI.portfolios[portfolioNum].positions
        val list = mutableListOf<PortfolioRow>()
        for(pos in portfolioPositions) {
            if(getInstrumentTypeDB(pos.instrumentType) == InstrumentTypeDB.Currency) {
                continue
            }

            val posQuantity = pos.quantity.toInt()
            val instr = tinkoffDao.getMarketInstrument(pos.figi) ?:
                throw IllegalArgumentException("Не найден инструмент ${pos.figi}")

            val lastRate: Double = ExchangeRateAPI.getLastRate(instr.currency)
            if(lastRate < 0.000001)
                throw Exception("Нулевой курс ${instr.currency}")

            var earliestOffsetDate = OffsetDateTime.now()
            val name = tinkoffDao.getMarketInstrument(pos.figi)?.name ?: ""
            val row = PortfolioRow(pos.figi, name, instr.currency).apply{
                var buyQuantity: Int = 0
                var buySumRub = 0.0
                val buys = tinkoffDao.getOperations(portfolioNum, pos.figi,
                    listOf(OperationType.OPERATION_TYPE_BUY,
                    OperationType.OPERATION_TYPE_BUY_CARD,
                    OperationType.OPERATION_TYPE_INPUT_SECURITIES))
                var commission = 0.0
                var buySum = 0.0
                for(buy in buys) {
                    val kurs = ExchangeRateAPI.getRate(buy.currency, buy.date.toLocalDate())
                    //commission += buy.commission
                    buyQuantity += buy.quantity

                    earliestOffsetDate = buy.date

                    if(buyQuantity > posQuantity) {
                        val price: Double = buy.payment / buy.quantity
                        buySumRub += price * posQuantity * kurs
                        buySum += price * posQuantity
                        break
                    }
                    buySumRub += buy.payment * kurs
                    buySum += buy.payment
                    if(buyQuantity == posQuantity)
                        break
                }
                quantity = posQuantity
                buySumRub = -buySumRub
                buySum = -buySum
                averageRate = if(instr.currency == CurrenciesDB.RUB)
                    1.0
                else {
                    if(buySum > 0.000001)
                        Utils.round2(buySumRub / buySum)
                    else {
                        1.0
                    }
                }

                profit = pos.expectedYield.toDouble() // + commission
                profitMap[instr.currency] = profitMap[instr.currency]!! + profit

                val commissionRub: Double = -commission

                earliestDate = earliestOffsetDate.toLocalDate()
                dividends = tinkoffDao.getDividends(portfolioNum, pos.figi, earliestOffsetDate).sumOf +
                        tinkoffDao.getDividendsTax(portfolioNum, pos.figi, earliestOffsetDate).sumOf

                val price = TinkoffAPI.getPriceByFigi(instr.figi)
                balance = price * posQuantity
                val balanceRub = balance * lastRate
                if(balanceRub < 0.01)
                    Log.i(TAG, "$name: $price / $figi")

                tax = Utils.round2(if(balanceRub > buySumRub) (balanceRub - buySumRub) * 0.13 else 0.0) // в рублях
                profitWithoutTax =
                    Utils.round2(profit + dividends - (commissionRub + tax) / lastRate)  //pos.averagePositionPrice?.value?.toDouble() ?: 0.0

                val closedList = calcData.dealList.filter { it.figi == instr.figi &&
                        it.date > firstDateOffset &&
                        it.date < lastDateOffset }
                val profitClosed = closedList.sumOf { it.profit }
                val sumDivClosed = tinkoffDao.getDividends(portfolioNum, instr.figi, firstDateOffset, earliestOffsetDate).sumOf -
                        tinkoffDao.getDividendsTax(portfolioNum, instr.figi, firstDateOffset, earliestOffsetDate).sumOf
                dividendsClosed = sumDivClosed
                val taxClosed = closedList.sumOf { it.tax / it.rate } // в рублях
                profitWithoutTaxClosed = Utils.round2(profitClosed + sumDivClosed - taxClosed / lastRate)

                _portfolioBalanceRub += balanceRub
                _portfolioBalanceUsd += moneyConvert(balance, instr.currency, lastRate, CurrenciesDB.USD, _lastUSDRate)

                _portfolioOpenRub += profit * lastRate
                _portfolioOpenUsd += moneyConvert(profit, instr.currency, lastRate, CurrenciesDB.USD, _lastUSDRate)

                _portfolioClosedProfitRub += profitWithoutTaxClosed * lastRate // по текущему курсу
                _portfolioClosedProfitUsd += moneyConvert(profitWithoutTaxClosed, instr.currency, lastRate, CurrenciesDB.USD, _lastUSDRate)
            }

            list.add(row)
        }
        list.sortBy { it.name }

        calcData.calcDeals()

        // Добавим еще закрытые сделки, которых сейчас нету в портфеле
        val closedList = calcData.dealList.filter {
                it.date > firstDateOffset &&
                it.date < lastDateOffset }
        val groupList = closedList.groupBy { it.figi }
        for(group in groupList) {
            val figi = group.key
            if(list.find {figi == it.figi } != null)
                continue

            val instr = tinkoffDao.getMarketInstrument(figi) ?:
                throw IllegalArgumentException("Не найден инструмент $figi")

            val lastRate: Double = ExchangeRateAPI.getLastRate(instr.currency)

            val row = PortfolioRow(figi, instr.name, instr.currency).apply {
                closed = true

                var profitClosed = 0.0
                var profitClosedRub = 0.0
                var profitClosedUsd = 0.0
                var taxClosed = 0.0
                var taxClosedRub = 0.0
                var taxClosedUsd = 0.0

                val dealList = group.value
                for(deal in dealList) {
                    profitClosed += deal.profit
                    profitClosedRub += deal.profit * deal.rate
                    taxClosed += deal.tax / deal.rate
                    taxClosedRub += deal.tax
                    val dealDate = deal.date.toLocalDate()
                    val rateOnDate = ExchangeRateAPI.getRate(instr.currency, dealDate)
                    val usdOnDate = ExchangeRateAPI.getRate(CurrenciesDB.USD, dealDate)
                    profitClosedUsd += moneyConvert(deal.profit.toDouble(), instr.currency, rateOnDate, CurrenciesDB.USD, usdOnDate)
                    taxClosedUsd += deal.tax / usdOnDate
                }

/*                val sumDivClosed: Long = tinkoffDao.getDividends(
                    portfolioNum,
                    instr.figi,
                    firstDateOffset,
                    OffsetDateTime.now()
                ).sumOf -
                        tinkoffDao.getDividendsTax(
                            portfolioNum,
                            instr.figi,
                            firstDateOffset,
                            OffsetDateTime.now()
                        ).sumOf*/

                val divList = tinkoffDao.getDividendsAndTax(portfolioNum, instr.figi,
                    firstDateOffset, OffsetDateTime.now())
                var sumDivClosed = 0.0
                var sumDivClosedRub = 0.0
                var sumDivClosedUsd = 0.0
                for(div in divList) {
                    val rate = ExchangeRateAPI.getRate(div.currency, div.date.toLocalDate())
                    val rateUsd = ExchangeRateAPI.getRate(CurrenciesDB.USD, div.date.toLocalDate())
/*                    if(div.operationType == OperationType.OPERATION_TYPE_DIVIDEND_TAX ||
                        div.operationType == OperationType.OPERATION_TYPE_DIVIDEND_TAX_PROGRESSIVE ||
                        div.operationType == OperationType.OPERATION_TYPE_BOND_TAX ||
                        div.operationType == OperationType.OPERATION_TYPE_BOND_TAX_PROGRESSIVE
                            ) { // Здесь налоги в рублях*/
                    if(div.currency == CurrenciesDB.RUB) {
                        sumDivClosedRub += div.payment
                        sumDivClosed += div.payment
                        sumDivClosedUsd += div.payment / rateUsd
                    } else {
                        sumDivClosed += div.payment
                        sumDivClosedRub += div.payment * rate
                        sumDivClosedUsd += moneyConvert(div.payment.toDouble(), div.currency, rate, CurrenciesDB.USD, rateUsd)
                    }
                }
                dividendsClosed = sumDivClosed

                profitWithoutTaxClosed =
                    Utils.round2(profitClosed + sumDivClosed - taxClosed)
                profitClosedRub =
                    Utils.round2(profitClosedRub + sumDivClosedRub - taxClosedRub)
                profitClosedUsd =
                    Utils.round2(profitClosedUsd + sumDivClosedUsd - taxClosedUsd)
                //balance = profitClosedRub
                tax = taxClosedRub

                _portfolioClosedProfitRub += profitClosedRub
                _portfolioClosedProfitUsd += profitClosedUsd
            }

            list.add(row)
        }

        val moneyPos = TinkoffAPI.getMoneyPositions(portfolioNum)
        for(pos in moneyPos) {
            val sum = pos.value
            val currency = pos.key

            val lastRate: Double = ExchangeRateAPI.getLastRate(currency)
            when (currency) {
                CurrenciesDB.RUB -> {
                    _portfolioBalanceRub += sum
                    _portfolioBalanceUsd += sum / _lastUSDRate
                }
                CurrenciesDB.USD -> {
                    _portfolioBalanceUsd += sum
                    _portfolioBalanceRub += sum * _lastUSDRate
                }
                else -> {
                    _portfolioBalanceRub += sum * lastRate
                    _portfolioBalanceUsd += sum * lastRate / _lastUSDRate
                }
            }

            val row = PortfolioRow("", currency.itName(), currency).apply {
                balance = sum
            }
            list.add(row)
        }

        calcData.calcIrr(_portfolioBalanceRub, _portfolioBalanceUsd)

        _portfolioTotalProfitRub = _portfolioOpenRub + _portfolioClosedProfitRub
        _portfolioTotalProfitUsd = _portfolioOpenUsd + _portfolioClosedProfitUsd

        _portfolioTotalIrrRub = calcData.rubIrr
        _portfolioTotalIrrUsd = calcData.usdIrr

        rowList.value = list

        portfolioBalanceRub.value = moneyFormat(_portfolioBalanceRub, CurrenciesDB.RUB)
        portfolioBalanceUsd.value = moneyFormat(_portfolioBalanceUsd, CurrenciesDB.USD)

        portfolioOpenRub.value = moneyFormat(_portfolioOpenRub, CurrenciesDB.RUB)
        portfolioOpenUsd.value = moneyFormat(_portfolioOpenUsd, CurrenciesDB.USD)
        portfolioOpenIrrRub.value = percentFormat(_portfolioOpenIrrRub)
        portfolioOpenIrrUsd.value = percentFormat(_portfolioOpenIrrUsd)

        portfolioClosedProfitRub.value = moneyFormat(_portfolioClosedProfitRub, CurrenciesDB.RUB)
        portfolioClosedProfitUsd.value = moneyFormat(_portfolioClosedProfitUsd, CurrenciesDB.USD)
        portfolioClosedProfitIrrRub.value = percentFormat(_portfolioClosedProfitIrrRub)
        portfolioClosedProfitIrrUsd.value = percentFormat(_portfolioClosedProfitIrrUsd)

        portfolioTotalProfitRub.value = moneyFormat(_portfolioTotalProfitRub, CurrenciesDB.RUB)
        portfolioTotalProfitUsd.value = moneyFormat(_portfolioTotalProfitUsd, CurrenciesDB.USD)
        portfolioTotalIrrRub.value = percentFormat(_portfolioTotalIrrRub)
        portfolioTotalIrrUsd.value = percentFormat(_portfolioTotalIrrUsd)
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