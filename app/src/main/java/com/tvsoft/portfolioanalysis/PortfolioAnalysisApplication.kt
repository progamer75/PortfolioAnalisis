package com.tvsoft.portfolioanalysis

import android.app.Application
import android.content.Context
import android.content.res.Configuration

class PortfolioAnalysisApplication: Application() {
    companion object {
        var context: Context? = null
    }
    //val tinkoff_db: TinkoffDB by lazy { TinkoffDB.getDatabase(this, CoroutineScope(SupervisorJob())) }

    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
/*    override fun onCreate() {
        super.onCreate()
        startKoin{
            androidLogger()
            androidContext(this@PortfolioAnalysisApplication)
            //modules(listOf())
        }
    }*/

    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    override fun onConfigurationChanged ( newConfig : Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    override fun onLowMemory() {
        super.onLowMemory()
    }

    private var mAppExecutors: AppExecutors? = null

    override fun onCreate() {
        super.onCreate()
        mAppExecutors = AppExecutors()
        context = this
    }

/*    fun getDatabase(): TinkoffDB? {
        return TinkoffDB.getInstance(this, mAppExecutors)
    }

    fun getRepository(): DataRepository? {
        return DataRepository.getInstance(getDatabase())
    }*/
}