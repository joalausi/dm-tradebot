package com.example.dmarketalert.view

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dmarketalert.R
import com.example.dmarketalert.model.AppSettings
import com.example.dmarketalert.viewModel.SettingsViewModel

class SettingsFragment : Fragment() {

    private lateinit var viewModel: SettingsViewModel

    // === SEARCH ===
    private lateinit var searchField: SearchView

    // === GENERAL SETTINGS ===
    private lateinit var spinnerLanguage: Spinner
    private lateinit var tvLanguage: TextView

    private lateinit var spinnerCurrency: Spinner
    private lateinit var tvCurrency: TextView

    private lateinit var spinnerTheme: Spinner
    private lateinit var tvTheme: TextView

    private lateinit var cardResetSettings: CardView

    // === NOTIFICATION SETTINGS ===
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var tvNotifications: TextView

    private lateinit var switchOutbid: SwitchCompat
    private lateinit var tvOutbid: TextView

    private lateinit var switchApi: SwitchCompat
    private lateinit var tvApi: TextView

    private lateinit var spinnerNotificationMode: Spinner
    private lateinit var tvNotificationMode: TextView

    private lateinit var spinnerNotificationDelay: Spinner
    private lateinit var tvNotificationDelay: TextView

    // === APPLICATION OPTIONS ===
    private lateinit var cardUpdateTargets: CardView
    private lateinit var cardClearNotifications: CardView
    private lateinit var cardClearHistory: CardView

    private lateinit var spinnerNotificationLimit: Spinner
    private lateinit var tvNotificationLimit: TextView

    private lateinit var spinnerHistoryLimit: Spinner
    private lateinit var tvHistoryLimit: TextView

    private lateinit var cardRemoveStatistics: CardView

    // === ABOUT APP ===
    private lateinit var tvLastUpdate: TextView
    private lateinit var cardGitHub: CardView
    private lateinit var cardDisclaimer: CardView

    // === OTHER ===
    private lateinit var cardTechSupport: CardView
    private lateinit var cardLicense: CardView
    private lateinit var cardTermsOfUseAndPrivacyPolice: CardView

    private var isUpdatingUI = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        initViews(view)
        setupSearchView()
        setupSpinners()
        setupListeners()
        observeViewModel()

        // Load statistic
        val userId = getCurrentUserId()
        if (userId.isNotEmpty()) {
            viewModel.loadStatistics(userId)
        }

        return view
    }

    private fun initViews(view: View) {
        // Search
        searchField = view.findViewById(R.id.Search_view)

        // General settings
        spinnerLanguage = view.findViewById(R.id.spinner_language)
        tvLanguage = view.findViewById(R.id.textView_language)

        spinnerCurrency = view.findViewById(R.id.spinner_currency)
        tvCurrency = view.findViewById(R.id.textView_currency)

        spinnerTheme = view.findViewById(R.id.spinner_theme)
        tvTheme = view.findViewById(R.id.textView_theme)

        cardResetSettings = view.findViewById(R.id.CardView_reset_settings)

        // Notification settings
        switchNotifications = view.findViewById(R.id.switch_turn_notifications)
        tvNotifications = view.findViewById(R.id.textView_turn_notification)

        switchOutbid = view.findViewById(R.id.switch_turn_outbid)
        tvOutbid = view.findViewById(R.id.textView_turn_outbid)

        switchApi = view.findViewById(R.id.switch_turn_API_errors)
        tvApi = view.findViewById(R.id.textView_API_errors)

        spinnerNotificationMode = view.findViewById(R.id.spinner_notification_mode)
        tvNotificationMode = view.findViewById(R.id.textView_notification_mode)

        spinnerNotificationDelay = view.findViewById(R.id.spinner_notification_mode2)
        tvNotificationDelay = view.findViewById(R.id.textView_notification_delay)

        // Application options
        cardUpdateTargets = view.findViewById(R.id.CardView_targets_update)
        cardClearNotifications = view.findViewById(R.id.CardView_notification_clear2)
        cardClearHistory = view.findViewById(R.id.CardView_history_clear)

        spinnerNotificationLimit = view.findViewById(R.id.spinner_notifications_limit)
        tvNotificationLimit = view.findViewById(R.id.textView_limit_notifications)

        spinnerHistoryLimit = view.findViewById(R.id.spinner_histury_limit)
        tvHistoryLimit = view.findViewById(R.id.textView_limit_history)

        cardRemoveStatistics = view.findViewById(R.id.CardView_clear_all_statitistic)

        // About app
        tvLastUpdate = view.findViewById(R.id.textView_LastUpdate)
        cardGitHub = view.findViewById(R.id.CardView_gitHub)
        cardDisclaimer = view.findViewById(R.id.CardView_disclamer)

        //Other
        cardTechSupport = view.findViewById(R.id.CardView_tech_support)
        cardLicense = view.findViewById(R.id.CardView_license)
        cardTermsOfUseAndPrivacyPolice = view.findViewById(R.id.CardView_rules)
    }

    private fun setupSearchView() {
        searchField.queryHint = "Search settings..."

        searchField.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // TODO: Realization of settings in the future
                Log.d("SettingsFragment", "Search query: $newText")
                return true
            }
        })
    }

    private fun setupSpinners() {
        // Language spinner
        val languages = arrayOf("English", "Українська", "Русский")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = languageAdapter

        // Currency spinner
        val currencies = arrayOf("USD$", "UAH₴", "EUR€")
        val currencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = currencyAdapter

        // Theme spinner
        val themes = arrayOf("System", "Light", "Dark")
        val themeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, themes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTheme.adapter = themeAdapter

        // Notification mode spinner
        val notificationModes = arrayOf("Sound", "Vibration", "Silent")
        val notificationModeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, notificationModes)
        notificationModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificationMode.adapter = notificationModeAdapter

        // Notification delay spinner
        val delays = arrayOf("0 hours", "1 hour", "5 hours", "10 hours", "1 day", "3 days", "7 days")
        val delayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, delays)
        delayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificationDelay.adapter = delayAdapter

        // Limits spinners
        val limits = arrayOf("0", "10", "25", "50", "100")

        val notificationLimitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, limits)
        notificationLimitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNotificationLimit.adapter = notificationLimitAdapter

        val historyLimitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, limits)
        historyLimitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHistoryLimit.adapter = historyLimitAdapter
    }

    private fun setupListeners() {
        val userId = getCurrentUserId()

        // === GENERAL SETTINGS ===

        // Language spinner
        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI) return

                val languageCode = when (position) {
                    0 -> AppSettings.LANGUAGE_ENGLISH
                    1 -> AppSettings.LANGUAGE_UKRAINIAN
                    2 -> AppSettings.LANGUAGE_RUSSIAN
                    else -> AppSettings.LANGUAGE_ENGLISH
                }

                viewModel.updateLanguage(languageCode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Currency spinner
        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI) return

                val currencyCode = when (position) {
                    0 -> AppSettings.CURRENCY_USD
                    1 -> AppSettings.CURRENCY_UAH
                    2 -> AppSettings.CURRENCY_EUR
                    else -> AppSettings.CURRENCY_USD
                }

                viewModel.updateCurrency(currencyCode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Theme spinner
        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI) return

                val themeCode = when (position) {
                    0 -> AppSettings.THEME_SYSTEM
                    1 -> AppSettings.THEME_LIGHT
                    2 -> AppSettings.THEME_DARK
                    else -> AppSettings.THEME_SYSTEM
                }

                viewModel.updateTheme(themeCode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Reset settings card
        cardResetSettings.setOnClickListener {
            showResetDialog()
        }

        // === NOTIFICATION SETTINGS ===

        // Main notifications switch
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingUI) return@setOnCheckedChangeListener
            viewModel.updateNotificationsEnabled(isChecked)
        }

        // Outbid notifications switch
        switchOutbid.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingUI) return@setOnCheckedChangeListener
            viewModel.updateOutbidNotifications(isChecked)
        }

        // API errors switch
        switchApi.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingUI) return@setOnCheckedChangeListener
            viewModel.updateApiErrorNotifications(isChecked)
        }

        // Notification mode spinner
        spinnerNotificationMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI) return

                val mode = when (position) {
                    0 -> AppSettings.NOTIFICATION_SOUND
                    1 -> AppSettings.NOTIFICATION_VIBRATION
                    2 -> AppSettings.NOTIFICATION_SILENT
                    else -> AppSettings.NOTIFICATION_SOUND
                }

                viewModel.updateNotificationMode(mode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Notification delay spinner
        spinnerNotificationDelay.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI) return

                val delay = when (position) {
                    0 -> AppSettings.DELAY_0_HOURS
                    1 -> AppSettings.DELAY_1_HOUR
                    2 -> AppSettings.DELAY_5_HOURS
                    3 -> AppSettings.DELAY_10_HOURS
                    4 -> AppSettings.DELAY_1_DAY
                    5 -> AppSettings.DELAY_3_DAYS
                    6 -> AppSettings.DELAY_7_DAYS
                    else -> AppSettings.DELAY_0_HOURS
                }

                viewModel.updateNotificationDelay(delay)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // === APPLICATION OPTIONS ===

        // Update targets card
        cardUpdateTargets.setOnClickListener {
            // TODO: Update target on main screen
            Toast.makeText(context, "Targets will be updated", Toast.LENGTH_SHORT).show()
        }

        // Clear notifications card
        cardClearNotifications.setOnClickListener {
            showClearNotificationsDialog()
        }

        // Clear history card
        cardClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }

        // Open GitHub URL
        cardGitHub.setOnClickListener {
            openGitHub("https://github.com/joalausi/dm-tradebot")
        }

        // Open disclaimer
        cardDisclaimer.setOnClickListener {
            opedDisclaimer()
        }

        // Open support URL
        cardTechSupport.setOnClickListener {
            support("https://mail.google.com/mail/u/0/#inbox?compose=DmwnWtDpKkndvKVhKGbDfstxvdsPhRgsSjNZQpzxrpSTCpqpNkSnPxrFvBwlzNLnjsfRxNLgkvPq")
        }

        // License
        cardLicense.setOnClickListener {
            license()
        }

        // Rules
        cardTermsOfUseAndPrivacyPolice.setOnClickListener {
            rules()
        }

        // Notification limit spinner
        spinnerNotificationLimit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI) return

                val limit = when (position) {
                    0 -> 0
                    1 -> 10
                    2 -> 25
                    3 -> 50
                    4 -> 100
                    else -> 50
                }

                viewModel.updateNotificationLimit(userId, limit)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // History limit spinner
        spinnerHistoryLimit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI) return

                val limit = when (position) {
                    0 -> 0
                    1 -> 10
                    2 -> 25
                    3 -> 50
                    4 -> 100
                    else -> 100
                }

                viewModel.updateHistoryLimit(userId, limit)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Remove all statistics card
        cardRemoveStatistics.setOnClickListener {
            showRemoveStatisticsDialog()
        }
    }

    private fun observeViewModel() {
        // Observation on settings
        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            updateUI(settings)
        }

        // Observation on operation status
        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is SettingsViewModel.OperationStatus.Loading -> {
                    // TODO: Show ProgressBar
                }
                is SettingsViewModel.OperationStatus.Success -> {
                    Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetOperationStatus()
                }
                is SettingsViewModel.OperationStatus.Error -> {
                    Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                    viewModel.resetOperationStatus()
                }
                else -> {
                    // Idle
                }
            }
        }
    }

    private fun updateUI(settings: AppSettings) {
        isUpdatingUI = true

        // Update language
        tvLanguage.text = settings.getLanguageDisplay()
        spinnerLanguage.setSelection(getLanguagePosition(settings.language))

        // Update currency
        tvCurrency.text = settings.getCurrencyDisplay()
        spinnerCurrency.setSelection(getCurrencyPosition(settings.currency))

        // Update theme
        tvTheme.text = settings.getThemeDisplay()
        spinnerTheme.setSelection(getThemePosition(settings.theme))

        // Update notification switches
        switchNotifications.isChecked = settings.notificationEnabled
        switchOutbid.isChecked = settings.outBidNotification
        switchApi.isChecked = settings.apiNotification

        // Update notification mode
        tvNotificationMode.text = settings.getNotificationModeDisplay()
        spinnerNotificationMode.setSelection(getNotificationModePosition(settings.notificationMode))

        // Update notification delay
        tvNotificationDelay.text = settings.getDelayText()
        spinnerNotificationDelay.setSelection(getDelayPosition(settings.notificationDelay))

        // Update limits
        tvNotificationLimit.text = settings.limitOfNotification.toString()
        spinnerNotificationLimit.setSelection(getLimitPosition(settings.limitOfNotification))

        tvHistoryLimit.text = settings.limitOfHistory.toString()
        spinnerHistoryLimit.setSelection(getLimitPosition(settings.limitOfHistory))

        // Update last update time
        updateLastUpdateText(settings.lastUpdate)

        isUpdatingUI = false
    }

    private fun updateLastUpdateText(timestamp: Long) {
        val date = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        tvLastUpdate.text = date
    }

    // === HELPER FUNCTIONS ===

    private fun getLanguagePosition(language: String): Int {
        return when (language) {
            AppSettings.LANGUAGE_ENGLISH -> 0
            AppSettings.LANGUAGE_UKRAINIAN -> 1
            AppSettings.LANGUAGE_RUSSIAN -> 2
            else -> 0
        }
    }

    private fun getCurrencyPosition(currency: String): Int {
        return when (currency) {
            AppSettings.CURRENCY_USD -> 0
            AppSettings.CURRENCY_UAH -> 1
            AppSettings.CURRENCY_EUR -> 2
            else -> 0
        }
    }

    private fun getThemePosition(theme: String): Int {
        return when (theme) {
            AppSettings.THEME_SYSTEM -> 0
            AppSettings.THEME_LIGHT -> 1
            AppSettings.THEME_DARK -> 2
            else -> 0
        }
    }

    private fun getNotificationModePosition(mode: String): Int {
        return when (mode) {
            AppSettings.NOTIFICATION_SOUND -> 0
            AppSettings.NOTIFICATION_VIBRATION -> 1
            AppSettings.NOTIFICATION_SILENT -> 2
            else -> 0
        }
    }

    private fun getDelayPosition(delay: Int): Int {
        return when (delay) {
            AppSettings.DELAY_0_HOURS -> 0
            AppSettings.DELAY_1_HOUR -> 1
            AppSettings.DELAY_5_HOURS -> 2
            AppSettings.DELAY_10_HOURS -> 3
            AppSettings.DELAY_1_DAY -> 4
            AppSettings.DELAY_3_DAYS -> 5
            AppSettings.DELAY_7_DAYS -> 6
            else -> 0
        }
    }

    private fun getLimitPosition(limit: Int): Int {
        return when (limit) {
            0 -> 0
            10 -> 1
            25 -> 2
            50 -> 3
            100 -> 4
            else -> 1
        }
    }

    private fun openGitHub(url: String){
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun opedDisclaimer(){
        startActivity(Intent(requireContext(), DisclaimerActivity::class.java))
    }

    private fun support(urlSupport: String){
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlSupport)))
    }

    private fun license(){
        startActivity(Intent(requireContext(), License::class.java))
    }

    private fun rules(){
        startActivity(Intent(requireContext(), Terms_of_use_and_privacy_police::class.java))
    }

    // === DIALOGS ===

    private fun showResetDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.reset_settings, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogLayout)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogLayout.findViewById<Button>(R.id.button_cancel4)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogLayout.findViewById<Button>(R.id.button_apply4)?.setOnClickListener {
            viewModel.resetSettings()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showClearNotificationsDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.clear_notifications, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogLayout)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogLayout.findViewById<Button>(R.id.button_cancel5)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogLayout.findViewById<Button>(R.id.button_apply5)?.setOnClickListener {
            val userId = getCurrentUserId()
            viewModel.clearNotifications(userId)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showClearHistoryDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.clear_history, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogLayout)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogLayout.findViewById<Button>(R.id.button_cancel6)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogLayout.findViewById<Button>(R.id.button_apply6)?.setOnClickListener {
            val userId = getCurrentUserId()
            viewModel.clearHistory(userId)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showRemoveStatisticsDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.remove_all_targets_statistic, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogLayout)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogLayout.findViewById<Button>(R.id.button_cancel7)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogLayout.findViewById<Button>(R.id.button_apply7)?.setOnClickListener {
            val userId = getCurrentUserId()
            viewModel.removeAllStatistics(userId)
            dialog.dismiss()
        }
        dialog.show()
    }

    /**
     * Get ID of current user
     */
    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", "") ?: ""
    }
}