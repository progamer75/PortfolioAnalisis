package com.tvsoft.portfolioanalisis

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.tvsoft.portfolioanalisis.databinding.ActivityMainBinding
import ru.tinkoff.invest.openapi.OpenApi
import ru.tinkoff.invest.openapi.model.rest.BrokerAccountType
import ru.tinkoff.invest.openapi.model.rest.Portfolio
import ru.tinkoff.invest.openapi.model.rest.SandboxRegisterRequest
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApi

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val TAG = "Main"
    val token = "t.qgrgDsNOJaT3DKPvLTkBBL8-rIR4ENxYZ9RmHcFVxshrsOUBmveSOGcYcsab_FrtgQJDJPDFjEONb8tU_JY6Lg"
    //trade: "t.qgrgDsNOJaT3DKPvLTkBBL8-rIR4ENxYZ9RmHcFVxshrsOUBmveSOGcYcsab_FrtgQJDJPDFjEONb8tU_JY6Lg"
    //sandbox: "t.GKbQQCVRH0z-mEm_l49C6PoLixDlL1HTjADa962Ulgz6abbTuDr_H7CvuLlSeCyc2vS46TGrZwj_GmEejmL3gg"
    lateinit var tinkoff_api: OpenApi
    val isSandbox: Boolean = false
    lateinit var portfolio: Portfolio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        initTinkoff()
    }

    private fun initTinkoff() {
        println("qwerty1")

        tinkoff_api = OkHttpOpenApi(token, isSandbox)
        if (tinkoff_api.isSandboxMode) {
            tinkoff_api.sandboxContext.performRegistration(SandboxRegisterRequest()).join()
        }
        println("qwerty2")
        portfolio = tinkoff_api.portfolioContext.getPortfolio(BrokerAccountType.TINKOFF.value).join()
        println("qwerty3")
        for (pos in portfolio.positions) {
            Log.i(TAG,pos.name + " - " + pos.balance)
        }
    }
}
