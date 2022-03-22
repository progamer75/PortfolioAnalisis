package com.tvsoft.portfolioanalysis

import android.util.Log
import com.tvsoft.portfolioanalisis.TinkoffTokens
import ru.tinkoff.piapi.contract.v1.*
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.models.Portfolio
import java.math.BigDecimal

object TinkoffAPI {
    private val TAG = "TinkoffAPI"
    lateinit var api: InvestApi//OpenApi
    var portfolios: MutableList<Portfolio> = mutableListOf()
    var userAccounts = listOf<Account>()

    fun init(): Boolean {
        try {
            //api = OkHttpOpenApi(TinkoffTokens().token, isSandbox)
            api = InvestApi.create(TinkoffTokens().token)
        } catch(ex: Exception) {
            Log.e(TAG, "Ошибка подключения к Tinkoff InvestApi. Возможно неверно указан токен или нет связи.")
        }

        //val userAccounts: List<UserAccount> = listOf()
        try {
            userAccounts = api.userService.accountsSync
        //api.userContext.accounts.get().accounts
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
        if(userAccounts[pNum].type == AccountType.ACCOUNT_TYPE_TINKOFF)
             return "Брокерский ${pNum+1} - ${userAccounts[pNum].name}"

        return "ИИС"
    }

    fun mapUnitsAndNanos(value: Quotation): BigDecimal {
        return if (value.units == 0L && value.nano == 0) {
            BigDecimal.ZERO
        } else BigDecimal.valueOf(value.units).add(BigDecimal.valueOf(value.nano.toLong(), 9))
    }

    fun getPriceByFigi(figi: String): Double {
        val priceList = api.marketDataService.getLastPricesSync(listOf(figi))
            //api.marketContext.getMarketOrderbook(figi, 1).get()
        val price = priceList.first()?.price
        return if(price != null)
                mapUnitsAndNanos(price).toDouble()
            else 0.0
    }

    fun getStocks(): List<Share> = api.instrumentsService.allSharesSync
    fun getBonds(): List<Bond> = api.instrumentsService.allBondsSync
    fun getEtfs(): List<Etf> = api.instrumentsService.allEtfsSync
}