package com.tvsoft.portfolioanalisis

import android.util.Log
import ru.tinkoff.invest.openapi.OpenApi
import ru.tinkoff.invest.openapi.model.rest.Portfolio
import ru.tinkoff.invest.openapi.model.rest.SandboxRegisterRequest
import ru.tinkoff.invest.openapi.model.rest.UserAccount
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApi

class TinkoffAPI(val token: String) {
    val TAG = "TinkoffAPI"
    private val isSandbox: Boolean = false
    private lateinit var tinkoff_api: OpenApi
    var portfolios: MutableList<Portfolio> = mutableListOf()

    val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("TAG")

    fun init() {
        try {
            tinkoff_api = OkHttpOpenApi(token, isSandbox)
        } catch(ex: Exception) {
            Log.e(TAG, "Ошибка OkHttpOpenApi")
            logger.error("Ошибка OkHttpOpenApi")
        }

        if (tinkoff_api.isSandboxMode) {
            tinkoff_api.sandboxContext.performRegistration(SandboxRegisterRequest()).join()
        }

        val userAccounts: List<UserAccount> = tinkoff_api.userContext.accounts.get().accounts
        for(a in userAccounts) {
            val portfolio: Portfolio = tinkoff_api.portfolioContext.getPortfolio(a.brokerAccountId).join()
            //Log.v(TAG, "$portfolio")
            portfolios.add(portfolio)
        }
    }

}