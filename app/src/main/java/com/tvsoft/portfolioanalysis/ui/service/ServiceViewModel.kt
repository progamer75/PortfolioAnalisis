package com.tvsoft.portfolioanalysis.ui.service

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.tvsoft.portfolioanalysis.*
import kotlinx.coroutines.launch
import ru.tinkoff.invest.openapi.model.rest.OperationStatus
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ServiceViewModel(private val tinkoffDao: TinkoffDao,
                       application: Application) :
    AndroidViewModel(application) {

    private val TAG = "ServiceViewModel"

    private val tinkoffAPI = TinkoffAPI.getInstance()
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
        val api = tinkoffAPI.api
        for(account in tinkoffAPI.userAccounts) {

        }
    }

    fun onLoadInstruments() {
        viewModelScope.launch {
            loadInstruments()
        }
    }

    private suspend fun loadInstruments() {
        val stocks = tinkoffAPI.getStocks()
        val bonds = tinkoffAPI.getBonds()
        val etfs = tinkoffAPI.getEtfs()

        val miList = mutableListOf<MarketInstrumentDB>()
        stocks?.let {
            for(stock in stocks) {
                miList.add(MarketInstrumentDB(stock))
            }
        }
        bonds?.let {
            for(bond in bonds) {
                miList.add(MarketInstrumentDB(bond))
            }
        }
        etfs?.let {
            for(etf in etfs) {
                miList.add(MarketInstrumentDB(etf))
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

    fun onLoadAllData() {
        loadAllDataDone.value = false
        viewModelScope.launch {
            loadInstruments()
            loadAllData()
        }
    }

    private suspend fun loadAllData() {
        val api = tinkoffAPI.api

        tinkoffDao.deleteAllPortfolio()
        TinkoffDB.portfolioList.clear()
        var id: Int = 0
        for(account in tinkoffAPI.userAccounts) {
            // сначала загружаем портфели
            id++
            val p = PortfolioDB(id, account.brokerAccountId, "")
            tinkoffDao.insertPortfolio(p)
            TinkoffDB.portfolioList.add(p)

            // операции загружаем по годам, если за год ничего нет, прерываем загрузку
            var localDateTime = LocalDateTime.from(LocalDateTime.now())
            while (true) {
                val timeFrom = OffsetDateTime.of(localDateTime.minusYears(1), ZoneOffset.UTC)
                val timeTo = OffsetDateTime.of(localDateTime, ZoneOffset.UTC)
                Log.i(TAG, "$timeFrom / $timeTo")
                localDateTime = localDateTime.minusYears(1)


                val operations = api.operationsContext.getOperations(timeFrom, timeTo, null, account.brokerAccountId).get().operations
                if (operations.isEmpty())
                    break

                for(oper in operations) {
                    if(oper.status != OperationStatus.DONE)
                        continue
                    if(tinkoffDao.findOperationById(oper.id).isNotEmpty()) // операция уже есть
                        continue
                    //Log.i(TAG, "$oper")
                    val operItem = OperationDB(id, oper)
                    tinkoffDao.insertOperation(operItem)

                    Log.i(TAG, "${LocalDateTime.ofEpochSecond(operItem.date, 0, ZoneOffset.UTC)} / ${operItem.payment}")
                }
            }
            loadAllDataDone.value = true
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            tinkoffDao.deleteAllOperation()
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