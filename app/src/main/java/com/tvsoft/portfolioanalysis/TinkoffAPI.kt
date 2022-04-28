package com.tvsoft.portfolioanalysis

import android.util.Log
import kotlinx.coroutines.delay
import ru.tinkoff.piapi.contract.v1.*
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.models.Portfolio
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

object TinkoffAPI {
    private val TAG = "TinkoffAPI"
    lateinit var api: InvestApi
    val portfolios = mutableListOf<Portfolio>()
    var userAccounts= mutableListOf<Account>()

    fun init(token: String): Boolean {
        try {
            api = InvestApi.create(token)//TinkoffTokens().token)
        } catch(ex: Exception) {
            Log.i(TAG, "${ex.message} / ${ex.toString()}")
            Log.e(TAG, "Ошибка подключения к Tinkoff InvestApi. Возможно неверно указан токен или нет связи.")
            return false
        }

        try {
            userAccounts = api.userService.accountsSync
        } catch(ex: Exception) {
            return false
        }

        for(a in userAccounts) {
            val portfolio = api.operationsService.getPortfolioSync(a.id)
                //api.portfolioContext.getPortfolio(a.brokerAccountId).join()
            //Log.v(TAG, "$portfolio")
            portfolios.add(portfolio)
        }

        return true
    }

    fun getPortfolioName(pNum: Int): String {
        //if(userAccounts[pNum].type == AccountType.ACCOUNT_TYPE_TINKOFF)
             return userAccounts[pNum].name

        //return "ИИС"
    }

    private fun getAccountId(portfolioNum: Int): String {
        return TinkoffAPI.userAccounts[portfolioNum].id
    }

    fun getPortfolio(portfolioNum: Int): Portfolio {
        return api.operationsService.getPortfolioSync(getAccountId(portfolioNum))
    }

    fun getMoneyPositions(portfolioNum: Int): Map<CurrenciesDB, Double> {
        val list = api.operationsService.getPositions(getAccountId(portfolioNum)).get().money
        val map = mutableMapOf<CurrenciesDB, Double>()
        for(pos in list) {
            map[getCurrenciesDB(pos.currency.toString())!!] = pos.value.toDouble()
        }

        return map
    }

    private fun mapUnitsAndNanos(value: Quotation): BigDecimal {
        return if (value.units == 0L && value.nano == 0) {
            BigDecimal.ZERO
        } else BigDecimal.valueOf(value.units).add(BigDecimal.valueOf(value.nano.toLong(), 9))
    }

    suspend fun getPriceByFigi(figi: String): Double {
        // TODO Для увеличения быстродействия попробовать запрашивать сразу прайсы на все figi
        var res = 0.0
        while(res < 0.001) {
            val priceList = api.marketDataService.getLastPricesSync(listOf(figi))
                //api.marketContext.getMarketOrderbook(figi, 1).get()
            val price = priceList.first()?.price
            res = if(price != null)
                    mapUnitsAndNanos(price).toDouble()
                else 0.0
            if(res < 0.001)
                delay(10)
        }

        return res
    }

    suspend fun loadInstruments(tinkoffDao: TinkoffDao) {
        val stocks = getStocks()
        val bonds = getBonds()
        val etfs = getEtfs()
        val currencies = getCurrencies()

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

    suspend fun loadAllData(tinkoffDao: TinkoffDao) {
        tinkoffDao.deleteAllPortfolio()
        TinkoffDB.portfolioList.clear() //TODO не красиво все это TinkoffDB и portfolioList
        var id: Int = 0
        for(account in userAccounts) {
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
    }

    fun getStocks(): List<Share> = api.instrumentsService.allSharesSync
    fun getBonds(): List<Bond> = api.instrumentsService.allBondsSync
    fun getEtfs(): List<Etf> = api.instrumentsService.allEtfsSync
    fun getCurrencies(): List<Currency> = api.instrumentsService.allCurrenciesSync
}