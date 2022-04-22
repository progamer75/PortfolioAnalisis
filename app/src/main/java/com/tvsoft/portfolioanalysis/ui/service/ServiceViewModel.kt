package com.tvsoft.portfolioanalysis.ui.service

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import androidx.navigation.findNavController
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
                if(getCurrenciesDB(stock.currency) != null)
                    miList.add(MarketInstrumentDB(stock))
                else
                    Log.i(TAG, "$stock")
            }
        }
        bonds.let {
            for(bond in bonds) {
                if(getCurrenciesDB(bond.currency) != null)
                    miList.add(MarketInstrumentDB(bond))
                else
                    Log.i(TAG, "$bond")
            }
        }
        etfs.let {
            for(etf in etfs) {
                if(getCurrenciesDB(etf.currency) != null)
                    miList.add(MarketInstrumentDB(etf))
                else
                    Log.i(TAG, "$etf")
            }
        }
        currencies.let {
            for(cur in currencies) {
                if(getCurrenciesDB(cur.currency) != null)
                    miList.add(MarketInstrumentDB(cur))
                else
                    Log.i(TAG, "$cur")
            }
        }
        tinkoffDao.loadAllMarketInstrument(miList)
    }

    fun onSettings(view: View) {
        val navController = view.findNavController().navigate(R.id.action_navigation_service_to_settings_fragment)
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
                localDateTime = localDateTime.minusYears(1)


                val fromInstant = timeFrom.toInstant()
                val toInstant = timeTo.toInstant()
                val operations = api.operationsService.getAllOperationsSync(account.id, fromInstant, toInstant)
                val a = api.operationsService.getAllOperationsSync(account.id, fromInstant, toInstant)
                if (operations.isEmpty())
                    break

                for(oper in operations) {
                    if(oper.state != OperationState.OPERATION_STATE_EXECUTED)
                        continue
                    if(tinkoffDao.findOperationById(oper.id).isNotEmpty()) {// операция уже есть
                        continue
                    }

                    val operItem = OperationDB(id, oper)
                    tinkoffDao.insertOperation(operItem)
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
        withContext(Dispatchers.IO) {
            tinkoffDao.deleteAllRates()
            enumValues<CurrenciesDB>().forEach {
                var date = LocalDate.of(2018, 3, 1)
                val rateList =
                    ExchangeRateAPI.loadRateFromRes( it, date, LocalDate.now())
                var prevRate = 0.0
                rateList.forEach {
                    while(date <= it.date) {
                        if(date == it.date) {
                            prevRate = it.rate
                        }
                        tinkoffDao.insertRate(ExchangeRateDB(it.currency, date, prevRate))
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