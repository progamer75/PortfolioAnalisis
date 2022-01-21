package com.tvsoft.portfolioanalysis

import androidx.lifecycle.ViewModel

class PAViewModel: ViewModel() {
    lateinit var tinkoff_api: TinkoffAPI
    var sss: Int = 0

    fun initTinkoff(): Boolean {
        tinkoff_api = TinkoffAPI.getInstance()
        return tinkoff_api.init()
    }
}