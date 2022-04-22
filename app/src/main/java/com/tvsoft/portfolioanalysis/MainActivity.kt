package com.tvsoft.portfolioanalysis

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tvsoft.portfolioanalysis.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val TAG = "Main"
    //private val viewModel: PAViewModel by viewModels<PAViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
/*        if(!TinkoffAPI.init(TinkoffTokens().token)) {
            Toast.makeText(this, "Ошибка связи!", Toast.LENGTH_SHORT).show()
        }*/
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
/*
        navController.addOnDestinationChangedListener { nc: NavController, nd: NavDestination, args: Bundle? ->
            if(nd.id == R.id.settings_fragment) {

            }
        }
*/
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_service
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

/*        supportFragmentManager
            .beginTransaction()
            .replace(R.id.navigation_home, HomeFragment())
            .commit()*/

        supportActionBar?.hide()
        //supportActionBar?.setDisplayShowTitleEnabled(false)
        //supportActionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
        //supportActionBar?.setCustomView(R.layout.toolbar_title)
        //textView: TextView = supportActionBar?.customView.findViewById(R.id.toolbar_title);
        //textView.setText("My Custom Title");

        //TODO проверить заведен ли токен, если нет, то открыть SettingsActivity
        //Если заведен, поробовать подключиться, если не получится, тоже открыть SettingsActivity

        //val layoutDataStore = SettingsDataStore(applicationContext.dataStore)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this )
        val token = sharedPreferences.getString("token", "")
        if(token == "") {
            navController.navigate(R.id.settings_fragment)
        } else
            if(!TinkoffAPI.init(token!!))
            {
                Toast.makeText(this, "Не удалось подключиться к Тинкофф Инвестициям!", Toast.LENGTH_LONG).show()
            }

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
    }
}
