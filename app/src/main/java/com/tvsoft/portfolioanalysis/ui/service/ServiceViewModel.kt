package com.tvsoft.portfolioanalysis.ui.service

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.tvsoft.portfolioanalysis.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.tinkoff.piapi.contract.v1.OperationState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

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
            loadInstruments()
        }
    }

    private suspend fun loadInstruments() {
        val stocks = TinkoffAPI.getStocks()
        val bonds = TinkoffAPI.getBonds()
        val etfs = TinkoffAPI.getEtfs()
        val currencies = TinkoffAPI.getCurrencies()

        val miList = mutableListOf<MarketInstrumentDB>()
        stocks.let {
            for(stock in stocks) {
                miList.add(MarketInstrumentDB(stock))
            }
        }
        bonds.let {
            for(bond in bonds) {
                miList.add(MarketInstrumentDB(bond))
            }
        }
        etfs.let {
            for(etf in etfs) {
                miList.add(MarketInstrumentDB(etf))
            }
        }
        currencies.let {
            for(cur in currencies) {
                miList.add(MarketInstrumentDB(cur))
            }
        }
        tinkoffDao.loadAllMarketInstrument(miList)
    }

    fun showText() {
        viewModelScope.launch {
            val allData = tinkoffDao.getAllMarketInstrument()
            _text.value = allData.size.toString()
        }
    }

    fun onLoadAllData() { // TODO Добавить прогрессбар для отображения прогресса загрузки начальных данных
        loadAllDataDone.value = false
        viewModelScope.launch {
            tinkoffDao.deleteAllOperation()
            tinkoffDao.deleteAllRates()
            loadInstruments()
            loadAllData()
            loadExchangeRates()
        }
    }

    private suspend fun loadAllData() {
        val api = TinkoffAPI.api

        tinkoffDao.deleteAllPortfolio()
        TinkoffDB.portfolioList.clear() //TODO не красиво все это TinkoffDB и portfolioList
        var id: Int = 0
        for(account in TinkoffAPI.userAccounts) {
            // сначала загружаем портфели
            val p = PortfolioDB(id, account.id, "")
            tinkoffDao.insertPortfolio(p)
            TinkoffDB.portfolioList.add(p)

            // операции загружаем по годам, если за год ничего нет, прерываем загрузку
            var localDateTime = LocalDateTime.from(LocalDateTime.now())
            while (true) {
                val timeFrom = OffsetDateTime.of(localDateTime.minusYears(1), ZoneOffset.UTC)
                val timeTo = OffsetDateTime.of(localDateTime, ZoneOffset.UTC)
                Log.i(TAG, "$timeFrom / $timeTo")
                localDateTime = localDateTime.minusYears(1)


                val fromInstant = timeFrom.toInstant()
                val toInstant = timeTo.toInstant()
                val operations = api.operationsService.getAllOperationsSync(account.id, fromInstant, toInstant)
                val a = api.operationsService.getAllOperationsSync(account.id, fromInstant, toInstant)
                if (operations.isEmpty())
                    break

                for(oper in operations) {
                    if(Utils.ts2LocalDate(oper.date) < LocalDate.of(2019, 7, 25) &&
                        Utils.ts2LocalDate(oper.date) > LocalDate.of(2019, 7, 8))
                        Log.i(TAG, "$oper")
                    if(oper.state != OperationState.OPERATION_STATE_EXECUTED)
                        continue
                    if(tinkoffDao.findOperationById(oper.id).isNotEmpty()) {// операция уже есть
                        Log.i(TAG, "$oper")
                        continue
                    }

                    val operItem = OperationDB(id, oper)
                    tinkoffDao.insertOperation(operItem)
                    //Log.i(TAG, oper.toString())
                }
            }
            id++
        }
        loadAllDataDone.value = true
    }

    fun deleteAll() {
        viewModelScope.launch {
            tinkoffDao.deleteAllOperation()
        }
    }

    fun onLoadExchangeRates() {
        viewModelScope.launch {
            loadExchangeRates()
        }
    }

    private suspend fun loadExchangeRates() {
        val rateList = tinkoffDao.getAllRates()

        rateList.forEach {
            Log.i(TAG, "${it.currency} / ${it.date} / ${it.rate}")
        }

        withContext(Dispatchers.IO) {
            enumValues<CurrenciesDB>().forEach {
                var date = LocalDate.of(2018, 3, 1)
                val rateList =
                    ExchangeRateAPI.loadRateFromRes( it, date, LocalDate.now())
                var prevRate = 0.0
                rateList.forEach {
//                    Log.i(TAG, "${it.currency} / ${it.date} / ${it.rate}")
                    while(date <= it.date) {
                        if(date == it.date) {
                            prevRate = it.rate
                        }
                        tinkoffDao.insertRate(ExchangeRateDB(it.currency, date, prevRate))
                        //Log.i(TAG, "${it.currency} / $date / ${prevRate} / ${it.date}")

                        date = date.plusDays(1)
                    }
                }
            }
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