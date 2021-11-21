package com.tvsoft.portfolioanalisis

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.tvsoft.portfolioanalisis.databinding.ActivityMainBinding
import ru.tinkoff.invest.openapi.OpenApi
import ru.tinkoff.invest.openapi.model.rest.BrokerAccountType
import ru.tinkoff.invest.openapi.model.rest.Portfolio
import ru.tinkoff.invest.openapi.model.rest.SandboxRegisterRequest
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApi

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val TAG = "Main"
    lateinit var tinkoff_api: TinkoffAPI
    lateinit var tinkoff_db: TinkoffDB

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
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_service
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

/*        navView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.Service -> { true}
                else -> false
            }
        }*/

        tinkoff_api = TinkoffAPI(TinkoffTokens().token)
        tinkoff_api.init()

        initTinkoff()
    }

    private fun initTinkoff() {
        tinkoff_db = Room.databaseBuilder(applicationContext, TinkoffDB::class.java, "tinkoff_db").build()
        Log.v(TAG, "tinkoffDB - Ok")
    }
}
