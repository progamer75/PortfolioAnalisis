package com.tvsoft.portfolioanalysis

import android.util.Log
import kotlinx.coroutines.delay
import ru.tinkoff.piapi.contract.v1.*
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.models.Portfolio
import java.math.BigDecimal

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



    fun getStocks(): List<Share> = api.instrumentsService.allSharesSync
    fun getBonds(): List<Bond> = api.instrumentsService.allBondsSync
    fun getEtfs(): List<Etf> = api.instrumentsService.allEtfsSync
    fun getCurrencies(): List<Currency> = api.instrumentsService.allCurrenciesSync
}