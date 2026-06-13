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
    private data class SettingItem(
        val view: View,
        val keywords: String
    )
    private val allSettingItems = mutableListOf<SettingItem>()

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
        setupSearchItems()
        observeViewModel()
        setupListeners()

        val userId = getCurrentUserId()

        android.util.Log.d("SETTINGS_DEBUG", "userId = '$userId'")

        if (userId.isNotEmpty()) {
            viewModel.loadStatistics(userId)
        } else {
            android.util.Log.e("SETTINGS_DEBUG", "userId порожній! Firestore не викликається")
        }

        return view
    }

    private fun initViews(view: View) {

        searchField = view.findViewById(R.id.Search_view)
        spinnerLanguage = view.findViewById(R.id.spinner_language)
        tvLanguage = view.findViewById(R.id.textView_language)
        spinnerCurrency = view.findViewById(R.id.spinner_currency)
        tvCurrency = view.findViewById(R.id.textView_currency)
        spinnerTheme = view.findViewById(R.id.spinner_theme)
        tvTheme = view.findViewById(R.id.textView_theme)
        cardResetSettings = view.findViewById(R.id.CardView_reset_settings)

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

        cardUpdateTargets = view.findViewById(R.id.CardView_targets_update)
        cardClearNotifications = view.findViewById(R.id.CardView_notification_clear2)
        cardClearHistory = view.findViewById(R.id.CardView_history_clear)
        spinnerNotificationLimit = view.findViewById(R.id.spinner_notifications_limit)
        tvNotificationLimit = view.findViewById(R.id.textView_limit_notifications)
        spinnerHistoryLimit = view.findViewById(R.id.spinner_histury_limit)
        tvHistoryLimit = view.findViewById(R.id.textView_limit_history)
        cardRemoveStatistics = view.findViewById(R.id.CardView_clear_all_statitistic)

        tvLastUpdate = view.findViewById(R.id.textView_LastUpdate)
        cardGitHub = view.findViewById(R.id.CardView_gitHub)
        cardDisclaimer = view.findViewById(R.id.CardView_disclamer)
        cardTechSupport = view.findViewById(R.id.CardView_tech_support)
        cardLicense = view.findViewById(R.id.CardView_license)
        cardTermsOfUseAndPrivacyPolice = view.findViewById(R.id.CardView_rules)
    }

    private fun setupSearchItems() {

        val languageCard = view?.findViewById<CardView>(R.id.CardView_language)
        val currencyCard = view?.findViewById<CardView>(R.id.CardView_currency)
        val themeCard = view?.findViewById<CardView>(R.id.CardView_theme)
        val notificationsCard = view?.findViewById<CardView>(R.id.CardView_turnOff_On)
        val outbidCard = view?.findViewById<CardView>(R.id.CardView_notification_outbid)
        val apiCard = view?.findViewById<CardView>(R.id.CardView_notification_API)
        val modeCard = view?.findViewById<CardView>(R.id.CardView_notification_mode)
        val delayCard = view?.findViewById<CardView>(R.id.CardView_notification_clear)
        val notifLimitCard = view?.findViewById<CardView>(R.id.CardView_notification_limit)
        val historyLimitCard = view?.findViewById<CardView>(R.id.CardView_history_limit)

        languageCard?.let {
            allSettingItems.add(SettingItem(it, "language мова язык english ukrainian"))
        }
        currencyCard?.let {
            allSettingItems.add(SettingItem(it, "currency валюта usd uah eur dollar hryvnia"))
        }
        themeCard?.let {
            allSettingItems.add(SettingItem(it, "theme тема dark light system темна світла"))
        }
        notificationsCard?.let {
            allSettingItems.add(SettingItem(it, "notifications сповіщення enable disable увімкнути"))
        }
        outbidCard?.let {
            allSettingItems.add(SettingItem(it, "outbid перебита ставка auction аукціон"))
        }
        apiCard?.let {
            allSettingItems.add(SettingItem(it, "api errors помилки error"))
        }
        modeCard?.let {
            allSettingItems.add(SettingItem(it, "sound vibration silent звук вібрація тихий mode"))
        }
        delayCard?.let {
            allSettingItems.add(SettingItem(it, "delay затримка hours години day день"))
        }
        cardClearNotifications.let {
            allSettingItems.add(SettingItem(it, "clear notifications очистити сповіщення видалити"))
        }
        cardClearHistory.let {
            allSettingItems.add(SettingItem(it, "clear history очистити історію"))
        }
        notifLimitCard?.let {
            allSettingItems.add(SettingItem(it, "limit notifications ліміт сповіщень кількість"))
        }
        historyLimitCard?.let {
            allSettingItems.add(SettingItem(it, "limit history ліміт історії"))
        }
        cardResetSettings.let {
            allSettingItems.add(SettingItem(it, "reset settings скинути налаштування defaults"))
        }
        cardGitHub.let {
            allSettingItems.add(SettingItem(it, "github source code код репозиторій"))
        }
    }

    private fun setupSearchView() {
        searchField.queryHint = "Search settings..."

        searchField.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSettings(newText ?: "")
                return true
            }
        })
    }

    private fun filterSettings(query: String) {
        if (query.isEmpty()) {
            allSettingItems.forEach { it.view.visibility = View.VISIBLE }
            return
        }

        val lowerQuery = query.lowercase()

        allSettingItems.forEach { item ->
            val matches = item.keywords.lowercase().contains(lowerQuery)
            item.view.visibility = if (matches) View.VISIBLE else View.GONE
        }
    }

    private fun setupSpinners() {

        val languages = arrayOf("English", "Українська", "Русский")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerLanguage.adapter = it
        }

        val currencies = arrayOf("USD$", "UAH₴", "EUR€")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCurrency.adapter = it
        }

        val themes = arrayOf("System", "Light", "Dark")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, themes).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTheme.adapter = it
        }

        val notificationModes = arrayOf("Sound", "Vibration", "Silent")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, notificationModes).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerNotificationMode.adapter = it
        }

        val delays = arrayOf("0 hours", "1 hour", "5 hours", "10 hours", "1 day", "3 days", "7 days")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, delays).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerNotificationDelay.adapter = it
        }

        val limits = arrayOf("0", "10", "25", "50", "100")
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, limits).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerNotificationLimit.adapter = it
        }
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, limits).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerHistoryLimit.adapter = it
        }
    }

    private fun setupListeners() {
        val userId = getCurrentUserId()

        // --- Language ---
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

        // --- Currency ---
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

        // --- Theme ---
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

        cardResetSettings.setOnClickListener { showResetDialog() }

        // --- Notifications ---
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingUI) return@setOnCheckedChangeListener
            viewModel.updateNotificationsEnabled(isChecked)
        }

        switchOutbid.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingUI) return@setOnCheckedChangeListener
            viewModel.updateOutbidNotifications(isChecked)
        }

        switchApi.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingUI) return@setOnCheckedChangeListener
            viewModel.updateApiErrorNotifications(isChecked)
        }

        // --- Mode of notifications ---
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

        // --- Delay of notifications ---
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

        // --- Action buttons ---
        cardUpdateTargets.setOnClickListener {
            Toast.makeText(context, "Targets will be updated", Toast.LENGTH_SHORT).show()
        }

        cardClearNotifications.setOnClickListener { showClearNotificationsDialog() }
        cardClearHistory.setOnClickListener { showClearHistoryDialog() }
        cardGitHub.setOnClickListener { openUrl("https://github.com/joalausi/dm-tradebot") }
        cardDisclaimer.setOnClickListener {
            startActivity(Intent(requireContext(), DisclaimerActivity::class.java))
        }
        cardTechSupport.setOnClickListener {
            openUrl("https://mail.google.com/mail/u/0/#inbox?compose=DmwnWtDpKkndvKVhKGbDfstxvdsPhRgsSjNZQpzxrpSTCpqpNkSnPxrFvBwlzNLnjsfRxNLgkvPq")
        }
        cardLicense.setOnClickListener {
            startActivity(Intent(requireContext(), License::class.java))
        }
        cardTermsOfUseAndPrivacyPolice.setOnClickListener {
            startActivity(Intent(requireContext(), Terms_of_use_and_privacy_police::class.java))
        }

        // --- Limits ---
        spinnerNotificationLimit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI || userId.isEmpty()) return
                val limit = when (position) { 0->0; 1->10; 2->25; 3->50; 4->100; else->50 }
                viewModel.updateNotificationLimit(userId, limit)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerHistoryLimit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingUI || userId.isEmpty()) return
                val limit = when (position) { 0->0; 1->10; 2->25; 3->50; 4->100; else->100 }
                viewModel.updateHistoryLimit(userId, limit)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        cardRemoveStatistics.setOnClickListener { showRemoveStatisticsDialog() }
    }

    private fun observeViewModel() {

        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            updateUI(settings)
        }

        viewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is SettingsViewModel.OperationStatus.Loading -> {
                }
                is SettingsViewModel.OperationStatus.Success -> {
                    Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetOperationStatus()
                }
                is SettingsViewModel.OperationStatus.Error -> {
                    Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                    viewModel.resetOperationStatus()
                }
                else -> { }
            }
        }

        viewModel.restartActivity.observe(viewLifecycleOwner) { shouldRestart ->
            if (shouldRestart == true) {
                viewModel.resetRestartFlag()
                (activity as? BaseActivity)?.restartForLanguageChange()
            }
        }
    }

    private fun updateUI(settings: AppSettings) {
        isUpdatingUI = true

        tvLanguage.text = settings.getLanguageDisplay()
        spinnerLanguage.setSelection(getLanguagePosition(settings.language))

        tvCurrency.text = settings.getCurrencyDisplay()
        spinnerCurrency.setSelection(getCurrencyPosition(settings.currency))

        tvTheme.text = settings.getThemeDisplay()
        spinnerTheme.setSelection(getThemePosition(settings.theme))

        switchNotifications.isChecked = settings.notificationEnabled
        switchOutbid.isChecked = settings.outBidNotification
        switchApi.isChecked = settings.apiNotification

        tvNotificationMode.text = settings.getNotificationModeDisplay()
        spinnerNotificationMode.setSelection(getNotificationModePosition(settings.notificationMode))

        tvNotificationDelay.text = settings.getDelayText()
        spinnerNotificationDelay.setSelection(getDelayPosition(settings.notificationDelay))

        tvNotificationLimit.text = settings.limitOfNotification.toString()
        spinnerNotificationLimit.setSelection(getLimitPosition(settings.limitOfNotification))

        tvHistoryLimit.text = settings.limitOfHistory.toString()
        spinnerHistoryLimit.setSelection(getLimitPosition(settings.limitOfHistory))

        updateLastUpdateText(settings.lastUpdate)

        isUpdatingUI = false
    }

    private fun updateLastUpdateText(timestamp: Long) {
        val date = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        tvLastUpdate.text = date
    }

    // ===== HELPER FUNCTIONS =====

    private fun getLanguagePosition(language: String) = when (language) {
        AppSettings.LANGUAGE_ENGLISH -> 0
        AppSettings.LANGUAGE_UKRAINIAN -> 1
        AppSettings.LANGUAGE_RUSSIAN -> 2
        else -> 0
    }

    private fun getCurrencyPosition(currency: String) = when (currency) {
        AppSettings.CURRENCY_USD -> 0
        AppSettings.CURRENCY_UAH -> 1
        AppSettings.CURRENCY_EUR -> 2
        else -> 0
    }

    private fun getThemePosition(theme: String) = when (theme) {
        AppSettings.THEME_SYSTEM -> 0
        AppSettings.THEME_LIGHT -> 1
        AppSettings.THEME_DARK -> 2
        else -> 0
    }

    private fun getNotificationModePosition(mode: String) = when (mode) {
        AppSettings.NOTIFICATION_SOUND -> 0
        AppSettings.NOTIFICATION_VIBRATION -> 1
        AppSettings.NOTIFICATION_SILENT -> 2
        else -> 0
    }

    private fun getDelayPosition(delay: Int) = when (delay) {
        AppSettings.DELAY_0_HOURS -> 0
        AppSettings.DELAY_1_HOUR -> 1
        AppSettings.DELAY_5_HOURS -> 2
        AppSettings.DELAY_10_HOURS -> 3
        AppSettings.DELAY_1_DAY -> 4
        AppSettings.DELAY_3_DAYS -> 5
        AppSettings.DELAY_7_DAYS -> 6
        else -> 0
    }

    private fun getLimitPosition(limit: Int) = when (limit) {
        0 -> 0; 10 -> 1; 25 -> 2; 50 -> 3; 100 -> 4; else -> 1
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    // ===== DIALOGS =====

    private fun showResetDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.reset_settings, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogLayout).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogLayout.findViewById<Button>(R.id.button_cancel4)?.setOnClickListener { dialog.dismiss() }
        dialogLayout.findViewById<Button>(R.id.button_apply4)?.setOnClickListener {
            viewModel.resetSettings()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showClearNotificationsDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.clear_notifications, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogLayout).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogLayout.findViewById<Button>(R.id.button_cancel5)?.setOnClickListener { dialog.dismiss() }
        dialogLayout.findViewById<Button>(R.id.button_apply5)?.setOnClickListener {
            viewModel.clearNotifications(getCurrentUserId())
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showClearHistoryDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.clear_history, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogLayout).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogLayout.findViewById<Button>(R.id.button_cancel6)?.setOnClickListener { dialog.dismiss() }
        dialogLayout.findViewById<Button>(R.id.button_apply6)?.setOnClickListener {
            viewModel.clearHistory(getCurrentUserId())
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showRemoveStatisticsDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.remove_all_targets_statistic, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogLayout).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogLayout.findViewById<Button>(R.id.button_cancel7)?.setOnClickListener { dialog.dismiss() }
        dialogLayout.findViewById<Button>(R.id.button_apply7)?.setOnClickListener {
            viewModel.removeAllStatistics(getCurrentUserId())
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun getCurrentUserId(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("nickname", "") ?: ""
    }
}