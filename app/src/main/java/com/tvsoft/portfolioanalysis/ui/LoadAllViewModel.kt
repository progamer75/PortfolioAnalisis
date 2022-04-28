package com.tvsoft.portfolioanalysis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tvsoft.portfolioanalysis.*
import ru.tinkoff.piapi.contract.v1.OperationState
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LoadAllViewModel (private val tinkoffDao: TinkoffDao,
    application: Application) : AndroidViewModel(application) {

    var loadAllDataTitle = MutableLiveData<String?>()
    var loadAllDataProgress = MutableLiveData<Int?>()
    var loadAllDataDone = MutableLiveData<Boolean?>()

    suspend fun loadAllData() {
        loadAllDataTitle.value = "Загрузка инструментов..."
        if(tinkoffDao.getNumMarketInstrument() < 100)
            TinkoffAPI.loadInstruments(tinkoffDao)

        loadAllDataTitle.value = "Загрузка курсов валют..."
        tinkoffDao.deleteAllRates()
        ExchangeRateAPI.loadExchangeRates()

        tinkoffDao.deleteAllOperation()
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
                loadAllDataTitle.value = "Загрузка операций c " +
                        "${timeFrom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} по " +
                        timeTo.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                localDateTime = localDateTime.minusYears(1)

                val fromInstant = timeFrom.toInstant()
                val toInstant = timeTo.toInstant()
                val operations = TinkoffAPI.api.operationsService.getAllOperationsSync(account.id, fromInstant, toInstant)
                if (operations.isEmpty())
                    break

                for(oper in operations) {
                    if(oper.state != OperationState.OPERATION_STATE_EXECUTED)
                        continue
/*                    if(tinkoffDao.findOperationById(oper.id).isNotEmpty()) {// операция уже есть
                        continue
                    }*/

                    val operItem = OperationDB(id, oper)
                    tinkoffDao.insertOperation(operItem)

                    val proc: Double = ChronoUnit.DAYS.between(
                        Utils.ts2OffsetDateTime(oper.date).toLocalDate(),
                        timeTo.toLocalDate()) * 100.0 / 365.0
                    loadAllDataProgress.value = proc.toInt()
                }
            }
            id++
        }

        //loadAllDataTitle.value = "Загрузка завершена"
        loadAllDataDone.value = true
    }
}

class LoadAllViewModelFactory(
    private val tinkoffDao: TinkoffDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoadAllViewModel::class.java)) {
            return LoadAllViewModel(tinkoffDao, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}