package com.tvsoft.portfolioanalysis.settings

import android.os.Bundle
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tvsoft.portfolioanalysis.R
import com.tvsoft.portfolioanalysis.TinkoffAPI

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        super.onDisplayPreferenceDialog(preference)
        when (preference.key) {
            "token" -> {
                preference.setOnPreferenceChangeListener { pref, newValue ->
                    if(TinkoffAPI.init(newValue as String)) {
                        Toast.makeText(pref.context, "Подключение успешно. Загружаю данные...", Toast.LENGTH_SHORT).show()
                        val navController = this.requireActivity().findNavController(R.id.nav_host_fragment_activity_main)
                        navController.navigate(R.id.action_settings_fragment_to_navigation_service)
                        true
                    }
                    else {
                        Toast.makeText(pref.context, "Не удалось подключиться к Тинкофф Инвестициям!", Toast.LENGTH_LONG).show()
                        false
                    }
                }
            }
        }
    }
}