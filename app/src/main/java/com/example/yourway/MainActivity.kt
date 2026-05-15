package com.example.yourway

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yourway.data.RealtimeSocket
import com.example.yourway.databinding.ActivityMainBinding
import com.example.yourway.model.SupportMessage
import com.example.yourway.model.UiState
import com.example.yourway.ui.InvestmentAdapter
import com.example.yourway.ui.NeonUi
import com.example.yourway.ui.NotificationHelper
import com.example.yourway.ui.PlanAdapter
import com.example.yourway.ui.PlatformViewModel
import com.example.yourway.ui.TransactionAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: PlatformViewModel
    private val realtimeSocket = RealtimeSocket()

    private var latestState = UiState()
    private var currentScreen = Screen.SPLASH
    private var currentTab = MainTab.HOME
    private var onboardingIndex = 0
    private var signupMode = false
    private var selectedPaymentMethod = "UPI"
    private var lastSupportRefreshAt = 0L
    private val supportTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasRequiredRuntimePermissions()) {
            routeAfterSplash()
        } else {
            renderPermissionScreen()
            Snackbar.make(binding.root, "SMS permission keeps backend sync working; notifications keep wallet alerts live.", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor(NeonUi.BLACK)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NotificationHelper.ensureChannel(this)

        viewModel = ViewModelProvider(this)[PlatformViewModel::class.java]
        viewModel.state.observe(this) { state ->
            latestState = state
            renderCurrentScreen()
            state.message?.let { message ->
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                if (message.contains("credited", ignoreCase = true) ||
                    message.contains("activated", ignoreCase = true)
                ) {
                    NotificationHelper.show(this, "YourWay", message)
                }
                viewModel.clearMessage()
            }
        }

        showSplash()
        realtimeSocket.connect(
            onRefreshRequested = { runOnUiThread { viewModel.refreshFromStorage() } },
            onNotification = { title, body -> runOnUiThread { NotificationHelper.show(this, title, body) } },
            onSupportMessage = { message -> runOnUiThread { viewModel.applyRemoteSupportMessage(message) } }
        )
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.refreshFromStorage()
    }

    override fun onDestroy() {
        realtimeSocket.disconnect()
        super.onDestroy()
    }

    private fun renderCurrentScreen() {
        when (currentScreen) {
            Screen.SPLASH -> renderSplash()
            Screen.PERMISSIONS -> renderPermissionScreen()
            Screen.AUTH -> renderAuthScreen()
            Screen.ONBOARDING -> renderOnboardingScreen()
            Screen.MAIN -> renderMainShell(currentTab)
        }
    }

    private fun showSplash() {
        currentScreen = Screen.SPLASH
        renderSplash()
        binding.root.postDelayed({ routeAfterSplash() }, 1500)
    }

    private fun routeAfterSplash() {
        when {
            !hasRequiredRuntimePermissions() -> {
                currentScreen = Screen.PERMISSIONS
                renderPermissionScreen()
            }
            !latestState.snapshot.profile.isLoggedIn -> {
                currentScreen = Screen.AUTH
                renderAuthScreen()
            }
            !latestState.snapshot.onboardingCompleted -> {
                currentScreen = Screen.ONBOARDING
                renderOnboardingScreen()
            }
            else -> {
                currentScreen = Screen.MAIN
                currentTab = MainTab.HOME
                renderMainShell(currentTab)
            }
        }
    }

    private fun setScreen(view: View) {
        binding.root.removeAllViews()
        binding.root.addView(view)
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(260).start()
    }

    private fun renderSplash() {
        currentScreen = Screen.SPLASH
        val root = NeonUi.root(this).apply {
            gravity = Gravity.CENTER
        }
        val logo = TextView(this).apply {
            text = "Y"
            textSize = 56f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(NeonUi.BLACK))
            background = NeonUi.paintingDrawable(this@MainActivity, 0)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(NeonUi.dp(this@MainActivity, 112), NeonUi.dp(this@MainActivity, 112))
        }
        val name = NeonUi.title(this, "YourWay", 36f).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(NeonUi.NEON))
        }
        val tag = NeonUi.label(this, "Futuristic investment platform", 14f).apply {
            gravity = Gravity.CENTER
        }
        root.addView(logo)
        root.addView(space(14))
        root.addView(name)
        root.addView(tag)
        setScreen(root)

        listOf("alpha", "scaleX", "scaleY").forEach { property ->
            ObjectAnimator.ofFloat(logo, property, if (property == "alpha") 0.15f else 0.86f, 1f).apply {
                duration = 900
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun renderPermissionScreen() {
        currentScreen = Screen.PERMISSIONS
        val content = screenContent("Secure permissions", "YourWay needs SMS receive access for backend sync and notifications for account alerts.")

        content.addView(permissionCard("Background SMS sync", "Incoming SMS are sent to the configured backend server. Message content is not shown anywhere inside the Android app."))
        content.addView(permissionCard("Notifications", "Shows local alerts when an investment is activated, profit is credited, or support replies arrive."))
        content.addView(permissionCard("Internet", "Connects to the existing Render backend and platform API. Internet is granted at install time."))
        content.addView(safetyNote())

        content.addView(NeonUi.button(this, "Allow permissions").apply {
            setOnClickListener {
                val missing = missingRuntimePermissions()
                if (missing.isEmpty()) routeAfterSplash() else permissionLauncher.launch(missing)
            }
        })

        setScreen(scroll(content))
    }

    private fun renderAuthScreen() {
        currentScreen = Screen.AUTH
        val content = screenContent(
            if (signupMode) "Create YourWay account" else "Welcome back",
            "Firebase email/password auth is used when configured; this build also supports a local sandbox session."
        )

        val nameInput = NeonUi.input(this, "Name", InputType.TYPE_CLASS_TEXT)
        val emailInput = NeonUi.input(this, "Email", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val passwordInput = NeonUi.input(this, "Password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        if (signupMode) content.addView(nameInput.first)
        content.addView(emailInput.first)
        content.addView(passwordInput.first)

        content.addView(NeonUi.button(this, if (signupMode) "Sign up" else "Login").apply {
            setOnClickListener {
                if (signupMode) {
                    viewModel.signup(nameInput.second.text?.toString().orEmpty(), emailInput.second.text?.toString().orEmpty(), passwordInput.second.text?.toString().orEmpty())
                } else {
                    viewModel.login(emailInput.second.text?.toString().orEmpty(), passwordInput.second.text?.toString().orEmpty())
                }
                binding.root.postDelayed({ routeAfterSplash() }, 600)
            }
        })
        content.addView(NeonUi.button(this, if (signupMode) "Use login instead" else "Create account", filled = false).apply {
            setOnClickListener {
                signupMode = !signupMode
                renderAuthScreen()
            }
        })
        content.addView(NeonUi.button(this, "Phone auth placeholder", filled = false).apply {
            isEnabled = false
            alpha = 0.55f
        })
        content.addView(safetyNote())

        setScreen(scroll(content))
    }

    private fun renderOnboardingScreen() {
        currentScreen = Screen.ONBOARDING
        val slides = listOf(
            "Invest in curated paintings" to "Explore fixed plans with transparent pricing, projected daily profit, and active plan tracking.",
            "Two-wallet model" to "Main wallet buys paintings. Interest wallet receives profit and is the only wallet eligible for withdrawal requests.",
            "Passive earnings simulation" to "Credit daily profit on demand for learning. No real returns, deposits, payments, or financial promises are made.",
            "Live support desk" to "Wallet movement, notifications, backend sync, and help chat are designed around realtime updates."
        )
        val (title, body) = slides[onboardingIndex]
        val content = NeonUi.root(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val card = NeonUi.card(this, padding = 22)
        card.addView(NeonUi.label(this, "0${onboardingIndex + 1} / 04", 13f, NeonUi.NEON))
        card.addView(space(8))
        card.addView(NeonUi.title(this, title, 28f))
        card.addView(space(12))
        card.addView(NeonUi.label(this, body, 16f, NeonUi.TEXT))
        card.addView(space(16))
        card.addView(onboardingIndicators(slides.size))
        content.addView(card)
        content.addView(NeonUi.button(this, if (onboardingIndex == slides.lastIndex) "Enter dashboard" else "Next").apply {
            setOnClickListener {
                if (onboardingIndex == slides.lastIndex) {
                    viewModel.completeOnboarding()
                    currentScreen = Screen.MAIN
                    currentTab = MainTab.HOME
                    renderMainShell(currentTab)
                } else {
                    onboardingIndex += 1
                    renderOnboardingScreen()
                }
            }
        })
        content.addView(NeonUi.button(this, "Skip", filled = false).apply {
            setOnClickListener {
                viewModel.completeOnboarding()
                currentScreen = Screen.MAIN
                renderMainShell(MainTab.HOME)
            }
        })

        setScreen(content)
    }

    private fun renderMainShell(tab: MainTab) {
        currentScreen = Screen.MAIN
        currentTab = tab
        maybeRefreshSupport(tab)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(NeonUi.BLACK))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        root.addView(mainHeader())
        root.addView(
            when (tab) {
                MainTab.HOME -> scroll(homeContent())
                MainTab.MARKET -> scroll(marketplaceContent())
                MainTab.WALLET -> scroll(walletContent())
                MainTab.HELP -> scroll(helpContent())
                MainTab.PROFILE -> scroll(profileContent())
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        root.addView(bottomNav())
        setScreen(root)
    }

    private fun maybeRefreshSupport(tab: MainTab) {
        if (tab != MainTab.HELP) return
        val now = System.currentTimeMillis()
        if (now - lastSupportRefreshAt < 15_000L) return
        lastSupportRefreshAt = now
        viewModel.refreshSupportMessages()
    }

    private fun homeContent(): LinearLayout {
        val state = latestState
        val snapshot = state.snapshot
        val content = baseMainContent("Dashboard", "Live portfolio")

        val walletCard = NeonUi.card(this)
        walletCard.addView(NeonUi.label(this, "Total wallet balance", 13f))
        val walletText = NeonUi.title(this, NeonUi.currency(0.0), 32f).apply { setTextColor(Color.parseColor(NeonUi.NEON)) }
        walletCard.addView(walletText)
        val split = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        split.addView(weightedMetric("Main", NeonUi.currency(snapshot.wallet.mainBalance)))
        split.addView(weightedMetric("Interest", NeonUi.currency(snapshot.wallet.interestBalance)))
        walletCard.addView(split)
        content.addView(walletCard)
        animateCurrency(walletText, snapshot.wallet.mainBalance + snapshot.wallet.interestBalance)

        val metrics = NeonUi.card(this)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(weightedMetric("Invested", NeonUi.currency(state.totalInvested)))
        row.addView(weightedMetric("Active paintings", snapshot.investments.sumOf { it.quantity }.toString()))
        metrics.addView(row)
        metrics.addView(NeonUi.divider(this))
        metrics.addView(chartPlaceholder())
        content.addView(metrics)

        content.addView(NeonUi.button(this, "Credit daily profit").apply {
            setOnClickListener { viewModel.creditDailyProfit() }
        })

        content.addView(sectionTitle("Active investments"))
        val investments = InvestmentAdapter()
        content.addView(recycler(investments).apply { minimumHeight = NeonUi.dp(this@MainActivity, 80) })
        investments.submitList(snapshot.investments.take(3))

        content.addView(sectionTitle("Recent transactions"))
        val transactions = TransactionAdapter()
        content.addView(NeonUi.card(this).apply {
            addView(recycler(transactions))
        })
        transactions.submitList(snapshot.transactions.take(6))

        return content
    }

    private fun marketplaceContent(): LinearLayout {
        val content = baseMainContent("Painting marketplace", "Buy multiple paintings")
        content.addView(safetyNote())
        val adapter = PlanAdapter { plan, quantity -> viewModel.buyPainting(plan, quantity) }
        content.addView(recycler(adapter))
        return content
    }

    private fun walletContent(): LinearLayout {
        val snapshot = latestState.snapshot
        val content = baseMainContent("Wallet", "Payments and withdrawals")

        val balances = NeonUi.card(this)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(weightedMetric("Main wallet", NeonUi.currency(snapshot.wallet.mainBalance)))
        row.addView(weightedMetric("Interest wallet", NeonUi.currency(snapshot.wallet.interestBalance)))
        balances.addView(row)
        content.addView(balances)

        val addCard = NeonUi.card(this)
        addCard.addView(NeonUi.title(this, "Add Money", 20f))
        addCard.addView(NeonUi.label(this, "Choose UPI, card, or net banking through the secure sandbox checkout.", 13f))
        addCard.addView(NeonUi.button(this, "Open payment checkout").apply {
            setOnClickListener { showPaymentSheet() }
        })
        content.addView(addCard)

        val withdrawCard = NeonUi.card(this)
        withdrawCard.addView(NeonUi.title(this, "Withdraw from interest wallet", 20f))
        withdrawCard.addView(NeonUi.label(this, "Creates an admin-reviewable request. Do not enter PINs, passwords, or other secrets.", 13f))
        val name = NeonUi.input(this, "Name", InputType.TYPE_CLASS_TEXT)
        val account = NeonUi.input(this, "Bank account", InputType.TYPE_CLASS_TEXT)
        val ifsc = NeonUi.input(this, "IFSC", InputType.TYPE_CLASS_TEXT)
        val upi = NeonUi.input(this, "UPI ID", InputType.TYPE_CLASS_TEXT)
        val amount = NeonUi.input(this, "Amount", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        listOf(name, account, ifsc, upi, amount).forEach { withdrawCard.addView(it.first) }
        withdrawCard.addView(NeonUi.button(this, "Submit withdrawal request").apply {
            setOnClickListener {
                viewModel.requestWithdrawal(
                    name.second.text?.toString().orEmpty(),
                    account.second.text?.toString().orEmpty(),
                    ifsc.second.text?.toString().orEmpty(),
                    upi.second.text?.toString().orEmpty(),
                    amount.second.text?.toString().orEmpty().toDoubleOrNull() ?: 0.0
                )
            }
        })
        content.addView(withdrawCard)

        content.addView(sectionTitle("Transaction history"))
        val transactions = TransactionAdapter()
        content.addView(NeonUi.card(this).apply {
            addView(recycler(transactions))
        })
        transactions.submitList(snapshot.transactions.take(12))

        return content
    }

    private fun showPaymentSheet() {
        val dialog = BottomSheetDialog(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 26))
            setBackgroundColor(Color.parseColor(NeonUi.BLACK))
        }

        fun renderStart() {
            content.removeAllViews()
            content.addView(NeonUi.title(this, "Add Money", 26f))
            content.addView(NeonUi.label(this, "Secure sandbox checkout", 13f, NeonUi.NEON))
            content.addView(NeonUi.label(this, getString(R.string.payment_safety_note), 13f))
            content.addView(space(8))

            val amountInput = NeonUi.input(this, "Amount", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            content.addView(amountInput.first)
            content.addView(sectionTitle("Payment method"))

            val methods = listOf("UPI", "Card", "Net Banking")
            methods.forEach { method ->
                content.addView(NeonUi.button(this, method, filled = method == "UPI").apply {
                    setOnClickListener {
                        val amount = amountInput.second.text?.toString().orEmpty().toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Snackbar.make(binding.root, "Enter a valid amount.", Snackbar.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        selectedPaymentMethod = method
                        when (method) {
                            "UPI" -> renderUpiPayment(content, dialog, amount)
                            "Card" -> renderCardPayment(content, dialog, amount)
                            else -> renderNetBanking(content, dialog, amount)
                        }
                    }
                })
            }
        }

        dialog.setContentView(ScrollView(this).apply { addView(content) })
        dialog.setOnShowListener {
            content.translationY = NeonUi.dp(this, 80).toFloat()
            content.alpha = 0f
            content.animate().translationY(0f).alpha(1f).setDuration(260).start()
        }
        renderStart()
        dialog.show()
    }

    private fun renderUpiPayment(content: LinearLayout, dialog: BottomSheetDialog, amount: Double) {
        content.removeAllViews()
        content.addView(NeonUi.title(this, "UPI Payment", 24f))
        content.addView(NeonUi.label(this, "Pay ${NeonUi.currency(amount)} to 123456789@paytm", 14f, NeonUi.NEON))
        content.addView(fakeQrCode())
        content.addView(NeonUi.label(this, "Choose UPI app", 13f))
        listOf("Paytm", "PhonePe", "Google Pay", "BHIM").forEach { app ->
            content.addView(NeonUi.button(this, app).apply {
                setOnClickListener { renderProcessing(content, dialog, amount, "UPI", app) }
            })
        }
    }

    private fun renderCardPayment(content: LinearLayout, dialog: BottomSheetDialog, amount: Double) {
        content.removeAllViews()
        content.addView(NeonUi.title(this, "Card Payment", 24f))
        val cardPreview = NeonUi.card(this, padding = 18).apply {
            background = NeonUi.paintingDrawable(this@MainActivity, 2)
            addView(NeonUi.label(this@MainActivity, "YOURWAY BLACK", 12f, NeonUi.BLACK))
            addView(NeonUi.title(this@MainActivity, "\u2022\u2022\u2022\u2022  \u2022\u2022\u2022\u2022  \u2022\u2022\u2022\u2022  \u2022\u2022\u2022\u2022", 23f).apply {
                setTextColor(Color.parseColor(NeonUi.BLACK))
            })
            addView(NeonUi.label(this@MainActivity, "VISA / MASTERCARD / RUPAY", 13f, NeonUi.BLACK))
        }
        content.addView(cardPreview)

        val cardNumber = NeonUi.input(this, "Card number", InputType.TYPE_CLASS_NUMBER)
        val expiry = NeonUi.input(this, "Expiry MM/YY", InputType.TYPE_CLASS_TEXT)
        val cvv = NeonUi.input(this, "CVV", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val network = NeonUi.label(this, "Card network: Detecting", 13f, NeonUi.NEON)
        content.addView(cardNumber.first)
        content.addView(expiry.first)
        content.addView(cvv.first)
        content.addView(network)

        cardNumber.second.doAfterTextChanged {
            network.text = "Card network: ${detectCardNetwork(it?.toString().orEmpty())}"
        }

        content.addView(NeonUi.button(this, "Continue").apply {
            setOnClickListener {
                val digits = cardNumber.second.text?.toString().orEmpty().filter { it.isDigit() }
                if (digits.length < 12 || expiry.second.text.isNullOrBlank() || (cvv.second.text?.length ?: 0) < 3) {
                    Snackbar.make(binding.root, "Enter complete card details for the sandbox flow.", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val cardNetwork = detectCardNetwork(digits)
                cardNumber.second.text?.clear()
                expiry.second.text?.clear()
                cvv.second.text?.clear()
                renderOtp(content, dialog, amount, "Card", provider = cardNetwork, cardNetwork = cardNetwork, bank = null)
            }
        })
    }

    private fun renderNetBanking(content: LinearLayout, dialog: BottomSheetDialog, amount: Double, selectedBank: String = "SBI") {
        content.removeAllViews()
        content.addView(NeonUi.title(this, "Net Banking", 24f))
        content.addView(NeonUi.label(this, "Choose bank", 13f, NeonUi.NEON))
        val banks = listOf("SBI", "HDFC", "ICICI", "Axis", "Kotak", "PNB", "Bank of Baroda", "Canara", "Union Bank", "IndusInd")
        content.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                banks.forEach { bank ->
                    addView(TextView(this@MainActivity).apply {
                        text = bank
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setPadding(NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 9), NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 9))
                        setTextColor(Color.parseColor(if (bank == selectedBank) NeonUi.BLACK else NeonUi.NEON))
                        background = if (bank == selectedBank) NeonUi.paintingDrawable(this@MainActivity, 0) else NeonUi.glassDrawable(this@MainActivity)
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 8))
                        }
                        setOnClickListener { renderNetBanking(content, dialog, amount, bank) }
                    })
                }
            })
        })

        val bankShell = NeonUi.card(this)
        bankShell.addView(NeonUi.title(this, "$selectedBank Secure Login", 20f))
        bankShell.addView(NeonUi.label(this, "In-app banking view", 13f, NeonUi.NEON))
        val username = NeonUi.input(this, "Username / Customer ID", InputType.TYPE_CLASS_TEXT)
        val password = NeonUi.input(this, "Password", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        bankShell.addView(username.first)
        bankShell.addView(password.first)
        bankShell.addView(NeonUi.button(this, "Continue").apply {
            setOnClickListener {
                if (username.second.text.isNullOrBlank() || password.second.text.isNullOrBlank()) {
                    Snackbar.make(binding.root, "Enter login details for the sandbox flow.", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                username.second.text?.clear()
                password.second.text?.clear()
                renderOtp(content, dialog, amount, "Net Banking", provider = selectedBank, cardNetwork = null, bank = selectedBank)
            }
        })
        content.addView(bankShell)
    }

    private fun renderOtp(
        content: LinearLayout,
        dialog: BottomSheetDialog,
        amount: Double,
        method: String,
        provider: String?,
        cardNetwork: String?,
        bank: String?
    ) {
        content.removeAllViews()
        content.addView(NeonUi.title(this, "OTP Verification", 24f))
        content.addView(NeonUi.label(this, "Enter any 6-digit code to continue", 14f))
        val otp = NeonUi.input(this, "6-digit OTP", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        content.addView(otp.first)
        content.addView(NeonUi.button(this, "Verify and Pay").apply {
            setOnClickListener {
                val code = otp.second.text?.toString().orEmpty()
                if (!Regex("^\\d{6}$").matches(code)) {
                    Snackbar.make(binding.root, "Enter a 6-digit OTP.", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                otp.second.text?.clear()
                renderProcessing(content, dialog, amount, method, provider, cardNetwork, bank)
            }
        })
    }

    private fun renderProcessing(
        content: LinearLayout,
        dialog: BottomSheetDialog,
        amount: Double,
        method: String,
        provider: String?,
        cardNetwork: String? = null,
        bank: String? = null
    ) {
        content.removeAllViews()
        content.gravity = Gravity.CENTER_HORIZONTAL
        content.addView(ProgressBar(this).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(NeonUi.NEON))
            layoutParams = LinearLayout.LayoutParams(NeonUi.dp(this@MainActivity, 64), NeonUi.dp(this@MainActivity, 64)).apply {
                setMargins(0, NeonUi.dp(this@MainActivity, 18), 0, NeonUi.dp(this@MainActivity, 18))
            }
        })
        content.addView(NeonUi.title(this, "Processing Payment", 24f).apply { gravity = Gravity.CENTER })
        content.addView(NeonUi.label(this, "Authorising secure sandbox transaction", 14f).apply { gravity = Gravity.CENTER })
        binding.root.postDelayed({
            val referenceId = "YW${UUID.randomUUID().toString().take(8).uppercase()}"
            dialog.dismiss()
            viewModel.completePayment(amount, method, provider, referenceId, cardNetwork, bank)
            NotificationHelper.show(this, "Payment successful", "${NeonUi.currency(amount)} added to your main wallet.")
            currentTab = MainTab.WALLET
            renderMainShell(MainTab.WALLET)
        }, 1800)
    }

    private fun fakeQrCode(): LinearLayout {
        val size = 13
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = NeonUi.glassDrawable(this@MainActivity)
            setPadding(NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, NeonUi.dp(this@MainActivity, 14), 0, NeonUi.dp(this@MainActivity, 14))
            }
            repeat(size) { row ->
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    repeat(size) { col ->
                        val active = row in 0..2 && col in 0..2 ||
                            row in 0..2 && col in 10..12 ||
                            row in 10..12 && col in 0..2 ||
                            (row * 7 + col * 11) % 5 == 0
                        addView(View(this@MainActivity).apply {
                            setBackgroundColor(Color.parseColor(if (active) NeonUi.NEON else NeonUi.BLACK))
                            layoutParams = LinearLayout.LayoutParams(NeonUi.dp(this@MainActivity, 10), NeonUi.dp(this@MainActivity, 10)).apply {
                                setMargins(NeonUi.dp(this@MainActivity, 1), NeonUi.dp(this@MainActivity, 1), NeonUi.dp(this@MainActivity, 1), NeonUi.dp(this@MainActivity, 1))
                            }
                        })
                    }
                })
            }
        }
    }

    private fun detectCardNetwork(number: String): String {
        val digits = number.filter { it.isDigit() }
        return when {
            digits.startsWith("4") -> "Visa"
            digits.startsWith("5") || digits.startsWith("2") -> "Mastercard"
            digits.startsWith("6") || digits.startsWith("8") -> "RuPay"
            else -> "Card"
        }
    }

    private fun helpContent(): LinearLayout {
        val snapshot = latestState.snapshot
        val content = baseMainContent("Help support", "Priority chat desk")

        val status = NeonUi.card(this)
        val statusRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        statusRow.addView(weightedMetric("Status", "Online"))
        statusRow.addView(weightedMetric("Replies", snapshot.supportMessages.count { it.fromSupport }.toString()))
        status.addView(statusRow)
        status.addView(NeonUi.divider(this))
        status.addView(NeonUi.label(this, "Support usually replies within 10 minutes. Never share OTPs, PINs, passwords, or card CVV.", 13f, NeonUi.WARNING))
        content.addView(status)

        content.addView(sectionTitle("Quick topics"))
        content.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(supportTopicChip("Payment", "I need help with a payment or wallet transaction."))
                addView(supportTopicChip("Withdrawal", "I need help with a withdrawal request."))
                addView(supportTopicChip("Plan", "I need help with a painting plan or profit credit."))
                addView(supportTopicChip("Account", "I need help with login or profile details."))
            })
        })

        val chatCard = NeonUi.card(this)
        chatCard.addView(NeonUi.title(this, "Support chat", 20f))
        val messages = snapshot.supportMessages.ifEmpty { defaultSupportMessages() }.takeLast(12)
        messages.forEach { chatCard.addView(chatBubble(it)) }
        content.addView(chatCard)

        val composer = NeonUi.card(this)
        val input = NeonUi.input(
            this,
            "Message support",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        )
        input.second.minLines = 2
        input.second.maxLines = 4
        composer.addView(input.first)
        composer.addView(NeonUi.button(this, "Send to support").apply {
            setOnClickListener {
                val body = input.second.text?.toString().orEmpty()
                viewModel.sendSupportMessage(body)
                input.second.text?.clear()
            }
        })
        content.addView(composer)

        return content
    }

    private fun profileContent(): LinearLayout {
        val snapshot = latestState.snapshot
        val profile = snapshot.profile
        val content = baseMainContent("Profile", profile.email.ifBlank { "Investor" })

        val stats = NeonUi.card(this)
        stats.addView(NeonUi.title(this, profile.name.ifBlank { "YourWay Investor" }, 24f))
        stats.addView(NeonUi.label(this, "Total earnings ${NeonUi.currency(latestState.totalEarnings)}", 14f, NeonUi.NEON))
        stats.addView(NeonUi.label(this, "Active plans ${snapshot.investments.size} | Support chats ${snapshot.supportMessages.count { !it.fromSupport }}", 14f))
        content.addView(stats)

        val edit = NeonUi.card(this)
        edit.addView(NeonUi.title(this, "Edit profile", 20f))
        val name = NeonUi.input(this, "Name", InputType.TYPE_CLASS_TEXT)
        val phone = NeonUi.input(this, "Phone", InputType.TYPE_CLASS_PHONE)
        name.second.setText(profile.name)
        phone.second.setText(profile.phone)
        edit.addView(name.first)
        edit.addView(phone.first)
        edit.addView(NeonUi.button(this, "Save profile").apply {
            setOnClickListener { viewModel.updateProfile(name.second.text?.toString().orEmpty(), phone.second.text?.toString().orEmpty()) }
        })
        content.addView(edit)

        content.addView(NeonUi.button(this, "Logout", filled = false).apply {
            setTextColor(Color.parseColor(NeonUi.ERROR))
            strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor(NeonUi.ERROR))
            setOnClickListener {
                viewModel.logout()
                currentScreen = Screen.AUTH
                renderAuthScreen()
            }
        })
        return content
    }

    private fun screenContent(title: String, subtitle: String): LinearLayout {
        return NeonUi.root(this).apply {
            setPadding(NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 58), NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 20))
            addView(NeonUi.label(this@MainActivity, "YOURWAY", 12f, NeonUi.NEON))
            addView(NeonUi.title(this@MainActivity, title, 30f))
            addView(NeonUi.label(this@MainActivity, subtitle, 15f))
            addView(space(14))
        }
    }

    private fun baseMainContent(title: String, subtitle: String): LinearLayout {
        return NeonUi.root(this).apply {
            setPadding(NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 16))
            addView(NeonUi.title(this@MainActivity, title, 26f))
            addView(NeonUi.label(this@MainActivity, subtitle, 13f, NeonUi.MUTED))
            addView(space(8))
        }
    }

    private fun mainHeader(): LinearLayout {
        val profile = latestState.snapshot.profile
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 42), NeonUi.dp(this@MainActivity, 20), NeonUi.dp(this@MainActivity, 10))
            addView(TextView(this@MainActivity).apply {
                text = "Y"
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(NeonUi.BLACK))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = NeonUi.paintingDrawable(this@MainActivity, 0)
                layoutParams = LinearLayout.LayoutParams(NeonUi.dp(this@MainActivity, 44), NeonUi.dp(this@MainActivity, 44))
            })
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(NeonUi.dp(this@MainActivity, 12), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(NeonUi.title(this@MainActivity, "YourWay", 22f))
                addView(NeonUi.label(this@MainActivity, profile.name.ifBlank { "Investor" }, 12f))
            })
            addView(NeonUi.label(this@MainActivity, "LIVE", 12f, NeonUi.NEON))
        }
    }

    private fun bottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 14))
            setBackgroundColor(Color.parseColor(NeonUi.SURFACE))
            MainTab.entries.forEach { tab ->
                addView(TextView(this@MainActivity).apply {
                    text = tab.label
                    gravity = Gravity.CENTER
                    textSize = 12f
                    setTextColor(Color.parseColor(if (tab == currentTab) NeonUi.NEON else NeonUi.MUTED))
                    background = if (tab == currentTab) NeonUi.glassDrawable(this@MainActivity) else null
                    setPadding(NeonUi.dp(this@MainActivity, 4), NeonUi.dp(this@MainActivity, 10), NeonUi.dp(this@MainActivity, 4), NeonUi.dp(this@MainActivity, 10))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(NeonUi.dp(this@MainActivity, 3), 0, NeonUi.dp(this@MainActivity, 3), 0)
                    }
                    setOnClickListener { renderMainShell(tab) }
                })
            }
        }
    }

    private fun permissionCard(title: String, body: String): LinearLayout {
        return NeonUi.card(this).apply {
            addView(NeonUi.title(this@MainActivity, title, 19f))
            addView(NeonUi.label(this@MainActivity, body, 14f, NeonUi.TEXT))
        }
    }

    private fun safetyNote(): LinearLayout {
        return NeonUi.card(this, padding = 12).apply {
            background = NeonUi.glassDrawable(this@MainActivity, NeonUi.WARNING)
            addView(NeonUi.label(this@MainActivity, getString(R.string.payment_safety_note), 13f, NeonUi.WARNING))
        }
    }

    private fun paymentOptions(): LinearLayout {
        val methods = listOf("UPI", "Card", "Netbanking")
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            methods.forEach { method ->
                addView(TextView(this@MainActivity).apply {
                    text = method
                    gravity = Gravity.CENTER
                    textSize = 13f
                    setTextColor(Color.parseColor(if (selectedPaymentMethod == method) NeonUi.BLACK else NeonUi.NEON))
                    background = if (selectedPaymentMethod == method) {
                        NeonUi.paintingDrawable(this@MainActivity, 0)
                    } else {
                        NeonUi.glassDrawable(this@MainActivity)
                    }
                    setPadding(NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 10), NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 10))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(NeonUi.dp(this@MainActivity, 4), NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 4), NeonUi.dp(this@MainActivity, 8))
                    }
                    setOnClickListener {
                        selectedPaymentMethod = method
                        renderMainShell(MainTab.WALLET)
                    }
                })
            }
        }
    }

    private fun supportTopicChip(label: String, message: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(NeonUi.NEON))
            background = NeonUi.glassDrawable(this@MainActivity, NeonUi.CYAN)
            setPadding(NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 9), NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 9))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 8), NeonUi.dp(this@MainActivity, 8))
            }
            setOnClickListener { viewModel.sendSupportMessage(message) }
        }
    }

    private fun chatBubble(message: SupportMessage): LinearLayout {
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 10), NeonUi.dp(this@MainActivity, 12), NeonUi.dp(this@MainActivity, 10))
            background = NeonUi.glassDrawable(this@MainActivity, if (message.fromSupport) NeonUi.CYAN else NeonUi.NEON)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.86f).apply {
                setMargins(0, NeonUi.dp(this@MainActivity, 6), 0, NeonUi.dp(this@MainActivity, 6))
            }
            addView(NeonUi.label(this@MainActivity, if (message.fromSupport) "Support" else "You", 11f, if (message.fromSupport) NeonUi.CYAN else NeonUi.NEON))
            addView(NeonUi.label(this@MainActivity, message.body, 14f, NeonUi.TEXT))
            addView(NeonUi.label(this@MainActivity, supportTimeFormat.format(Date(message.createdAt)), 11f, NeonUi.MUTED))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val spacer = Space(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 0.14f)
            }
            if (message.fromSupport) {
                addView(bubble)
                addView(spacer)
            } else {
                addView(spacer)
                addView(bubble)
            }
        }
    }

    private fun defaultSupportMessages(): List<SupportMessage> {
        return listOf(
            SupportMessage(
                body = "Hi, YourWay support is online. Tell us what you need help with.",
                fromSupport = true
            )
        )
    }

    private fun onboardingIndicators(count: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            repeat(count) { index ->
                addView(View(this@MainActivity).apply {
                    background = NeonUi.glassDrawable(this@MainActivity, if (index == onboardingIndex) NeonUi.NEON else NeonUi.MUTED)
                    layoutParams = LinearLayout.LayoutParams(
                        if (index == onboardingIndex) NeonUi.dp(this@MainActivity, 34) else NeonUi.dp(this@MainActivity, 10),
                        NeonUi.dp(this@MainActivity, 8)
                    ).apply { setMargins(NeonUi.dp(this@MainActivity, 4), 0, NeonUi.dp(this@MainActivity, 4), 0) }
                })
            }
        }
    }

    private fun chartPlaceholder(): LinearLayout {
        val values = listOf(0.35f, 0.62f, 0.48f, 0.8f, 0.58f, 0.94f)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            minimumHeight = NeonUi.dp(this@MainActivity, 126)
            values.forEachIndexed { index, value ->
                addView(View(this@MainActivity).apply {
                    background = NeonUi.paintingDrawable(this@MainActivity, index)
                    layoutParams = LinearLayout.LayoutParams(0, NeonUi.dp(this@MainActivity, (112 * value).roundToInt()), 1f).apply {
                        setMargins(NeonUi.dp(this@MainActivity, 5), 0, NeonUi.dp(this@MainActivity, 5), 0)
                    }
                })
            }
        }
    }

    private fun weightedMetric(label: String, value: String): LinearLayout {
        return NeonUi.metric(this, label, value).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun sectionTitle(text: String): TextView {
        return NeonUi.title(this, text, 19f).apply {
            setPadding(0, NeonUi.dp(this@MainActivity, 14), 0, NeonUi.dp(this@MainActivity, 2))
        }
    }

    private fun recycler(adapter: RecyclerView.Adapter<*>): RecyclerView {
        return RecyclerView(this).apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            isNestedScrollingEnabled = false
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun scroll(content: LinearLayout): ScrollView {
        return ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor(NeonUi.BLACK))
            addView(content)
        }
    }

    private fun space(dp: Int): Space {
        return Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, NeonUi.dp(this@MainActivity, dp))
        }
    }

    private fun animateCurrency(textView: TextView, target: Double) {
        ValueAnimator.ofFloat(0f, target.toFloat()).apply {
            duration = 700
            addUpdateListener { textView.text = NeonUi.currency((it.animatedValue as Float).toDouble()) }
            start()
        }
    }

    private fun hasRequiredRuntimePermissions(): Boolean = missingRuntimePermissions().isEmpty()

    private fun missingRuntimePermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        return permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    private enum class Screen {
        SPLASH,
        PERMISSIONS,
        AUTH,
        ONBOARDING,
        MAIN
    }

    private enum class MainTab(val label: String) {
        HOME("Home"),
        MARKET("Paintings"),
        WALLET("Wallet"),
        HELP("Help"),
        PROFILE("Profile")
    }
}
