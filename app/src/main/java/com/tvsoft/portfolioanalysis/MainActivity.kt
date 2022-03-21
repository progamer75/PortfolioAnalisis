package com.tvsoft.portfolioanalysis

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tvsoft.portfolioanalysis.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val TAG = "Main"
    //private val viewModel: PAViewModel by viewModels<PAViewModel>()
    //val tinkoff_db: TinkoffDB by lazy { TinkoffDB.getDatabase(this, CoroutineScope(SupervisorJob()))}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!TinkoffAPI.init()) {
            Toast.makeText(this, "Ошибка связи!", Toast.LENGTH_SHORT).show()
        }

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

        supportActionBar?.hide()
        //supportActionBar?.setDisplayShowTitleEnabled(false)
        //supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        //supportActionBar?.setCustomView(R.layout.toolbar_title)
        //textView: TextView = supportActionBar?.customView.findViewById(R.id.toolbar_title);
        //textView.setText("My Custom Title");

        //TODO проверить заведен ли токен, если нет, то открыть SettingsActivity
//        val portfolioListActivity = Intent(this, SettingsActivity::class.java)
//        startActivity(portfolioListActivity)


    /*        navView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_service -> { true}
                else -> false
            }
        }*/
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //outState.putString(KEY, "")
    }

/*    fun getViewModel2(): PAViewModel {
        return viewModel
    }*/
}
