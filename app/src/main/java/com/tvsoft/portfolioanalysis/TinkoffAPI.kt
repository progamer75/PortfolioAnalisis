package com.tvsoft.portfolioanalysis

import android.util.Log
import ru.tinkoff.invest.openapi.OpenApi
import ru.tinkoff.invest.openapi.model.rest.*
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApi

class TinkoffAPI() {
    private val TAG = "TinkoffAPI"
    private val isSandbox: Boolean = false
    lateinit var api: OpenApi
    var portfolios: MutableList<Portfolio> = mutableListOf()
    var userAccounts: List<UserAccount> = listOf()

    //private val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("TAG")

    companion object {
        // Singleton prevents multiple instances of API opening at the
        // same time.
        @Volatile
        private var INSTANCE: TinkoffAPI? = null

        fun getInstance(): TinkoffAPI {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the API
            return INSTANCE ?: synchronized(this) {
                val instance = TinkoffAPI()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    fun init(): Boolean {
        try {
            api = OkHttpOpenApi(TinkoffTokens().token, isSandbox)
        } catch(ex: Exception) {
            Log.e(TAG, "Ошибка OkHttpOpenApi")
            //logger.error("Ошибка OkHttpOpenApi")
        }

        if (api.isSandboxMode) {
            api.sandboxContext.performRegistration(SandboxRegisterRequest()).join()
        }

        //val userAccounts: List<UserAccount> = listOf()
        try {
            userAccounts = api.userContext.accounts.get().accounts
        } catch(ex: Exception) {
            return false
        }

        for(a in userAccounts) {
            val portfolio: Portfolio = api.portfolioContext.getPortfolio(a.brokerAccountId).join()
            //Log.v(TAG, "$portfolio")
            portfolios.add(portfolio)
        }

        return true
    }

    fun getPortfolioName(pNum: Int): String {
        if(userAccounts[pNum].brokerAccountType == BrokerAccountType.TINKOFF)
             return "Брокерский $pNum+1"

        return "ИИС"
    }

    // в центах/копейках
    fun getPortfolioSum(pNum: Int): Long {
        var sum: Long = 0
        for(pos in portfolios[pNum].positions) {
            sum =+ (pos.balance.scaleByPowerOfTen(2)).toLong()
        }

        return sum
    }

    fun getPriceByFigi(figi: String): Double {
        val orderbook = api.marketContext.getMarketOrderbook(figi, 1).get()
        return orderbook.get().lastPrice.toDouble()
    }

    fun getStocks(): List<MarketInstrument>? = api.marketContext.marketStocks.get().instruments
    fun getBonds(): List<MarketInstrument>? = api.marketContext.marketBonds.get().instruments
    fun getEtfs(): List<MarketInstrument>? = api.marketContext.marketEtfs.get().instruments
}