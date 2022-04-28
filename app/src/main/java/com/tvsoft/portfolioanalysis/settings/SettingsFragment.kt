package com.tvsoft.portfolioanalysis.settings

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
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
                        Toast.makeText(pref.context, "Подключение успешно.", Toast.LENGTH_SHORT).show()

                        var ok = false
                        val alertDialog: AlertDialog? = activity?.let {
                            val builder = AlertDialog.Builder(it)
                            builder.apply {
                                setPositiveButton(R.string.ok,
                                    DialogInterface.OnClickListener { dialog, id ->
                                        val navController = requireActivity()
                                            .findNavController(R.id.nav_host_fragment_activity_main)
                                        val bundle = bundleOf("loadAll" to true)
                                        navController.navigate(
                                            R.id.action_settings_fragment_to_loadAllFragment, bundle
                                        )
                                    })
                                setNegativeButton(R.string.cancel,
                                    DialogInterface.OnClickListener { dialog, id ->
                                    })
                                setTitle("Загрузить данные?")
                            }
                            // Set other dialog properties

                            // Create the AlertDialog
                            builder.create()
                        }
                        alertDialog?.show()

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