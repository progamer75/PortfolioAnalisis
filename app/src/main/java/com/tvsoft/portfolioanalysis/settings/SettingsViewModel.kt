package com.tvsoft.portfolioanalysis.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.tvsoft.portfolioanalysis.TinkoffAPI

class SettingsViewModel(val tinkoffAPI: TinkoffAPI, application: Application) :
    AndroidViewModel(application) {
}