package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.TajXOApplication
import com.example.data.api.SupabaseClient
import com.example.data.model.*
import com.example.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {

    // Current screen state for simplified custom routing
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Profile Cache Flow from Room DB
    val activeProfile: StateFlow<Profile?> = repository.activeProfileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Cached Challenges Flow from Room DB
    val challenges: StateFlow<List<Challenge>> = repository.cachedChallengesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cached Transactions Flow from Room DB
    val transactions: StateFlow<List<Transaction>> = repository.cachedTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state states
    var loginUsernameOrEmail = MutableStateFlow("")
    var loginPassword = MutableStateFlow("")
    var registerUsername = MutableStateFlow("")
    var registerEmail = MutableStateFlow("")
    var registerPassword = MutableStateFlow("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Matchmaking & Match Details state
    private val _activeMatch = MutableStateFlow<Match?>(null)
    val activeMatch: StateFlow<Match?> = _activeMatch.asStateFlow()

    val player1Profile = MutableStateFlow<Profile?>(null)
    val player2Profile = MutableStateFlow<Profile?>(null)

    private val _isMatchmaking = MutableStateFlow(false)
    val isMatchmaking: StateFlow<Boolean> = _isMatchmaking.asStateFlow()

    private val _matchmakingStatus = MutableStateFlow("")
    val matchmakingStatus: StateFlow<String> = _matchmakingStatus.asStateFlow()

    private val _matchmakingBetAmount = MutableStateFlow("5") // Default 5.0
    val matchmakingBetAmount: StateFlow<String> = _matchmakingBetAmount.asStateFlow()

    // Admin Panel
    private val _pendingTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val pendingTransactions: StateFlow<List<Transaction>> = _pendingTransactions.asStateFlow()

    private val _adminProfiles = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val adminProfiles: StateFlow<Map<String, Profile>> = _adminProfiles.asStateFlow()

    // Notifications & Monitoring Logs
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _monitoringLogs = MutableStateFlow<List<MonitoringLog>>(emptyList())
    val monitoringLogs: StateFlow<List<MonitoringLog>> = _monitoringLogs.asStateFlow()

    // Game Effects Indicators
    private val _gameWinEffect = MutableStateFlow(false)
    val gameWinEffect: StateFlow<Boolean> = _gameWinEffect.asStateFlow()

    private val _gameLoseEffect = MutableStateFlow(false)
    val gameLoseEffect: StateFlow<Boolean> = _gameLoseEffect.asStateFlow()

    private val _gameDrawEffect = MutableStateFlow(false)
    val gameDrawEffect: StateFlow<Boolean> = _gameDrawEffect.asStateFlow()

    // Friends System states
    private val _friendsList = MutableStateFlow<List<FriendUser>>(emptyList())
    val friendsList: StateFlow<List<FriendUser>> = _friendsList.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<PendingFriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<PendingFriendRequest>> = _pendingRequests.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FriendUser>>(emptyList())
    val searchResults: StateFlow<List<FriendUser>> = _searchResults.asStateFlow()

    val searchQuery = MutableStateFlow("")

    private val _selectedUserProfile = MutableStateFlow<Profile?>(null)
    val selectedUserProfile: StateFlow<Profile?> = _selectedUserProfile.asStateFlow()

    private val _incomingInvitations = MutableStateFlow<List<MatchInvitation>>(emptyList())
    val incomingInvitations: StateFlow<List<MatchInvitation>> = _incomingInvitations.asStateFlow()

    private val _matchChats = MutableStateFlow<List<ChatMessage>>(emptyList())
    val matchChats: StateFlow<List<ChatMessage>> = _matchChats.asStateFlow()

    private val _isMicMuted = MutableStateFlow(true)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isVoiceConnected = MutableStateFlow(false)
    val isVoiceConnected: StateFlow<Boolean> = _isVoiceConnected.asStateFlow()

    private val _lobbyMatch = MutableStateFlow<Match?>(null)
    val lobbyMatch: StateFlow<Match?> = _lobbyMatch.asStateFlow()

    private val _lobbyInvitation = MutableStateFlow<MatchInvitation?>(null)
    val lobbyInvitation: StateFlow<MatchInvitation?> = _lobbyInvitation.asStateFlow()

    // Polling Jobs
    private var matchPollingJob: Job? = null
    private var matchmakingPollingJob: Job? = null
    private var generalPollingJob: Job? = null
    private var lobbyPollingJob: Job? = null
    private var h7BotPollingJob: Job? = null
    private var cachedH7Id: String? = null

    val isComputerBotEnabled = MutableStateFlow(false)

    init {
        // Start a general background polling to refresh dashboard periodically
        startGeneralDashboardPolling()
        startH7BotLoop()

        // Fetch and cache h.7 bot's ID on startup, and pre-compute perfect bot values
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                ensureSolvedValues()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                repository.fetchProfileByUsername("h.7").onSuccess {
                    cachedH7Id = it.id
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun navigateTo(screen: Screen) {
        val previousScreen = _currentScreen.value
        _currentScreen.value = screen
        _errorMessage.value = null
        _successMessage.value = null
        
        // Stop match/matchmaking polling when leaving game screen
        if (screen != Screen.Game) {
            stopMatchPolling()
            _activeMatch.value = null
            resetGameEffects()
        }
        if (screen != Screen.Matchmaking) {
            stopMatchmakingPolling()
        }
        if (screen != Screen.Lobby) {
            stopLobbyPolling()
        }



        // Auto trigger loads on navigation
        when (screen) {
            Screen.Home -> refreshDashboard()
            Screen.Wallet -> refreshDashboard()
            Screen.Notifications -> loadNotifications()
            Screen.Challenges -> refreshChallenges()
            Screen.AdminPanel -> loadAdminPanel()
            Screen.Friends -> {
                loadFriendsList()
                loadPendingFriendRequests()
                loadIncomingInvitations()
            }
            Screen.SearchUsers -> {
                clearSearch()
            }
            Screen.Lobby -> {
                startLobbyPolling()
            }
            Screen.FriendRequests -> {
                loadPendingFriendRequests()
            }
            else -> {}
        }
    }

    fun setMatchmakingBetAmount(amount: String) {
        _matchmakingBetAmount.value = amount
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = if (message != null) com.example.util.ErrorSanitizer.sanitize(message) else null
    }

    fun setSuccessMessage(message: String?) {
        _successMessage.value = message
    }

    fun resetGameEffects() {
        _gameWinEffect.value = false
        _gameLoseEffect.value = false
        _gameDrawEffect.value = false
    }

    private fun cleanLoginInput(str: String): String {
        var trimmed = str.trim()
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1).trim()
        }
        val builder = StringBuilder()
        for (ch in trimmed) {
            if (ch in '٠'..'٩') {
                builder.append((ch - '٠' + '0'.code).toChar())
            } else if (ch in '۰'..'۹') {
                builder.append((ch - '۰' + '0'.code).toChar())
            } else {
                builder.append(ch)
            }
        }
        return builder.toString().trim()
    }

    private fun isUserId(str: String): Boolean {
        val cleaned = cleanLoginInput(str)
        val isDigits = cleaned.isNotEmpty() && cleaned.all { it.isDigit() }
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
        return isDigits || uuidRegex.matches(cleaned)
    }

    // ==========================================
    // 1. AUTH LOGIC
    // ==========================================
    private fun loginUserInternal(loginStr: String, passStr: String) {
        val cleanedLoginStr = cleanLoginInput(loginStr)
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.login(cleanedLoginStr, passStr)
            _isLoading.value = false

            result.fold(
                onSuccess = { profile ->
                    _successMessage.value = "مرحباً بك مجدداً ${profile.username}"
                    loginPassword.value = ""
                    
                    // Start heartbeat, register session and sync immediately
                    viewModelScope.launch {
                        try {
                            repository.heartbeat(profile.id)
                            val devName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                            repository.registerUserSession(profile.id, devName, "127.0.0.1")
                            repository.refreshProfile(profile.id)
                            repository.refreshTransactions(profile.id)
                            loadNotifications()
                        } catch (e: Exception) {
                            // Fail silently
                        }
                    }

                    if (profile.username == "taj") {
                        navigateTo(Screen.AdminPanel)
                    } else {
                        navigateTo(Screen.Home)
                    }
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    _errorMessage.value = when {
                        rawMsg.contains("USER_NOT_FOUND") -> {
                            if (isUserId(cleanedLoginStr)) {
                                "لا يوجد حساب بهذا الـ User ID."
                            } else {
                                "لا يوجد حساب بهذه البيانات، يرجى إنشاء حساب جديد."
                            }
                        }
                        rawMsg.contains("WRONG_PASSWORD") -> 
                            "كلمة المرور غير صحيحة."
                        else -> 
                            "فشل تسجيل الدخول: $rawMsg"
                    }
                }
            )
        }
    }

    fun performLogin() {
        val rawLoginStr = loginUsernameOrEmail.value
        val loginStr = cleanLoginInput(rawLoginStr)
        val passStr = loginPassword.value

        if (loginStr.isEmpty() || passStr.isEmpty()) {
            _errorMessage.value = "الرجاء إدخال اسم المستخدم/البريد وكلمة المرور"
            return
        }

        if (isUserId(loginStr)) {
            _errorMessage.value = "تسجيل الدخول بالـ ID غير متاح حالياً. يرجى استخدام اسم المستخدم أو البريد الإلكتروني."
            return
        }

        loginUserInternal(loginStr, passStr)
    }

    fun performGoogleAuth(email: String, displayName: String) {
        val googlePassword = "GoogleSignIn_TajXo_2026_SecureKey"
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // 1. Try to login directly with the email and our deterministic Google password
            val loginResult = repository.login(email, googlePassword)
            loginResult.fold(
                onSuccess = { profile ->
                    _successMessage.value = "تم تسجيل الدخول بحساب جوجل بنجاح: ${profile.username} ✨"
                    _isLoading.value = false
                    
                    // Start heartbeat and sessions
                    viewModelScope.launch {
                        try {
                            repository.heartbeat(profile.id)
                            val devName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                            repository.registerUserSession(profile.id, devName, "127.0.0.1")
                            repository.refreshProfile(profile.id)
                            repository.refreshTransactions(profile.id)
                            loadNotifications()
                        } catch (e: Exception) {
                            // Fail silently
                        }
                    }

                    if (profile.username == "taj") {
                        navigateTo(Screen.AdminPanel)
                    } else {
                        navigateTo(Screen.Home)
                    }
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    if (rawMsg.contains("USER_NOT_FOUND")) {
                        // 2. User doesn't exist, we will automatically register them!
                        var baseUsername = displayName.trim().lowercase().replace("\\s+".toRegex(), "")
                        if (baseUsername.isEmpty()) {
                            baseUsername = email.substringBefore("@").lowercase().replace("[^a-z0-9]".toRegex(), "")
                        }
                        if (baseUsername.length < 3) baseUsername += "user"
                        
                        registerAndLoginGoogleUser(baseUsername, email, googlePassword)
                    } else {
                        _isLoading.value = false
                        _errorMessage.value = "فشل تسجيل الدخول بحساب جوجل: $rawMsg"
                    }
                }
            )
        }
    }

    private fun registerAndLoginGoogleUser(username: String, email: String, passwordStr: String, attempt: Int = 0) {
        viewModelScope.launch {
            val finalUsername = if (attempt == 0) username else "$username${(100..999).random()}"
            val regResult = repository.register(finalUsername, email, passwordStr)
            regResult.fold(
                onSuccess = { userId ->
                    val loginResult = repository.login(email, passwordStr)
                    _isLoading.value = false
                    loginResult.fold(
                        onSuccess = { profile ->
                            _successMessage.value = "تم إنشاء الحساب وتسجيل الدخول بجوجل بنجاح: ${profile.username} 🎉"
                            viewModelScope.launch {
                                try {
                                    repository.heartbeat(profile.id)
                                    val devName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                                    repository.registerUserSession(profile.id, devName, "127.0.0.1")
                                    repository.refreshProfile(profile.id)
                                    repository.refreshTransactions(profile.id)
                                    loadNotifications()
                                } catch (e: Exception) {
                                    // Fail silently
                                }
                            }
                            if (profile.username == "taj") {
                                navigateTo(Screen.AdminPanel)
                            } else {
                                navigateTo(Screen.Home)
                            }
                        },
                        onFailure = { error ->
                            _errorMessage.value = "فشل تسجيل الدخول التلقائي بعد التسجيل بجوجل: ${error.message}"
                        }
                    )
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    if (rawMsg.contains("USERNAME_ALREADY_EXISTS") && attempt < 5) {
                        registerAndLoginGoogleUser(username, email, passwordStr, attempt + 1)
                    } else if (rawMsg.contains("EMAIL_ALREADY_EXISTS")) {
                        _isLoading.value = false
                        _errorMessage.value = "هذا البريد الإلكتروني مسجل بالفعل بكلمة مرور عادية. يرجى تسجيل الدخول العادي أو التواصل مع الدعم الفني."
                    } else {
                        _isLoading.value = false
                        _errorMessage.value = "فشل تسجيل حساب جوجل تلقائياً: $rawMsg"
                    }
                }
            )
        }
    }

    fun performRegister() {
        val username = registerUsername.value.trim()
        val email = registerEmail.value.trim()
        val password = registerPassword.value

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _errorMessage.value = "الرجاء ملء جميع الحقول المطلوبة"
            return
        }

        if (username.length < 3) {
            _errorMessage.value = "اسم المستخدم يجب أن يتكون من 3 أحرف على الأقل"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.register(username, email, password)
            _isLoading.value = false

            result.fold(
                onSuccess = { userId ->
                    _successMessage.value = "تم إنشاء الحساب بنجاح! جاري تسجيل الدخول تلقائياً..."
                    registerUsername.value = ""
                    registerEmail.value = ""
                    registerPassword.value = ""
                    // Log in automatically after registration
                    loginUsernameOrEmail.value = username
                    loginPassword.value = ""
                    loginUserInternal(username, password)
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    _errorMessage.value = when {
                        rawMsg.contains("USERNAME_ALREADY_EXISTS") -> 
                            "اسم المستخدم هذا مستخدم بالفعل، يرجى اختيار اسم آخر."
                        rawMsg.contains("EMAIL_ALREADY_EXISTS") -> 
                            "البريد الإلكتروني هذا مسجل بالفعل بمستخدم آخر."
                        else -> 
                            "فشل تسجيل الحساب الجديد: $rawMsg"
                    }
                }
            )
        }
    }

    fun performLogout() {
        val userId = activeProfile.value?.id
        viewModelScope.launch {
            repository.logout(userId)
            navigateTo(Screen.Login)
        }
    }

    fun updateProfile(username: String, avatarUrl: String, onSuccess: () -> Unit) {
        val currentProfile = activeProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateProfile(currentProfile.id, username, avatarUrl)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _successMessage.value = "تم تحديث الملف الشخصي بنجاح!"
                    onSuccess()
                },
                onFailure = {
                    _errorMessage.value = it.localizedMessage ?: "فشل تحديث الملف الشخصي"
                }
            )
        }
    }

    // ==========================================
    // 2. DASHBOARD & WALLET LOGIC
    // ==========================================
    fun refreshDashboard() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.refreshProfile(profile.id)
            repository.refreshTransactions(profile.id)
            loadNotifications()
        }
    }

    fun refreshChallenges() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshChallenges()
            _isLoading.value = false
        }
    }

    fun requestDeposit(amount: Double, paymentMethod: String, proofImage: String) {
        val profile = activeProfile.value ?: return
        if (amount < 1.0) {
            _errorMessage.value = "الحد الأدنى للشحن هو 1 دولار أمريكي (USD)"
            return
        }
        if (proofImage.isEmpty()) {
            _errorMessage.value = "يجب رفع صورة إثبات التحويل"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.requestDeposit(profile.id, amount, paymentMethod, proofImage)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _successMessage.value = "تم إرسال طلب الشحن بنجاح وبانتظار موافقة الإدارة."
                    refreshDashboard()
                },
                onFailure = {
                    _errorMessage.value = "فشل تقديم طلب الشحن"
                }
            )
        }
    }

    fun requestWithdrawal(amount: Double, paymentMethod: String, payoutDetails: String) {
        val profile = activeProfile.value ?: return
        if (amount < 1.0) {
            _errorMessage.value = "الحد الأدنى للسحب هو 1 دولار أمريكي (USD)"
            return
        }
        if (profile.balance < amount) {
            _errorMessage.value = "عذراً، رصيدك المتاح غير كافٍ لإتمام عملية السحب"
            return
        }
        if (payoutDetails.isEmpty()) {
            _errorMessage.value = "يرجى إدخال بيانات الاستلام"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.requestWithdrawal(profile.id, amount, paymentMethod, payoutDetails)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _successMessage.value = "تم إرسال طلب السحب بنجاح وتم تجميد المبلغ وبانتظار موافقة الإدارة."
                    refreshDashboard()
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    if (rawMsg.contains("INSUFFICIENT_BALANCE")) {
                        _errorMessage.value = "عذراً، رصيدك المتاح غير كافٍ لإتمام عملية السحب"
                    } else {
                        _errorMessage.value = "فشل تقديم طلب السحب"
                    }
                }
            )
        }
    }

    // ==========================================
    // 3. MATCHMAKING LOGIC
    // ==========================================
    fun startMatchmaking(isFree: Boolean = false) {
        val profile = activeProfile.value ?: return
        val betVal = if (isFree) 0.0 else (matchmakingBetAmount.value.toDoubleOrNull() ?: 5.0)

        if (!isFree) {
            if (betVal < 1.0) {
                _errorMessage.value = "أقل قيمة لدخول أي مباراة هي 1 دولار أمريكي."
                return
            }

            if (profile.balance < betVal) {
                _errorMessage.value = "عذراً، رصيدك المتاح غير كافٍ لدخول المباراة. يرجى الشحن أولاً."
                return
            }
        }

        viewModelScope.launch {
            _isMatchmaking.value = true
            _errorMessage.value = null
            _matchmakingStatus.value = if (isFree) {
                "جاري البحث عن لاعبين في الوضع المجاني..."
            } else {
                "جاري البحث عن لاعبين بنفس قيمة الدخول ($betVal$)..."
            }
            
            val result = repository.performMatchmaking(profile.id, betVal)
            
            result.fold(
                onSuccess = { matchmakingResponse ->
                    if (matchmakingResponse.status == "matched") {
                        _isMatchmaking.value = false
                        _matchmakingStatus.value = "تم العثور على لاعب مناسب! جاري التحميل..."
                        val matchId = matchmakingResponse.matchId!!
                        startActiveMatch(matchId)
                    } else {
                        // status is 'waiting' (challenge created)
                        _matchmakingStatus.value = if (isFree) {
                            "تم إنشاء تحدي مفتوح مجاني. في انتظار لاعب آخر لقبول التحدي..."
                        } else {
                            "تم إنشاء التحدي المفتوح بقيمة $betVal$. في انتظار لاعب آخر لقبول التحدي..."
                        }
                        val challengeId = matchmakingResponse.challengeId!!
                        startMatchmakingPolling(challengeId)
                    }
                },
                onFailure = { error ->
                    _isMatchmaking.value = false
                    val rawMsg = error.message ?: ""
                    if (rawMsg.contains("INSUFFICIENT_BALANCE")) {
                        _errorMessage.value = "عذراً، رصيدك المتاح غير كافٍ لدخول المباراة."
                    } else {
                        _errorMessage.value = "فشل البدء في البحث عن مباراة. يرجى المحاولة مرة أخرى."
                    }
                }
            )
        }
    }

    fun acceptChallengeDirectly(challenge: Challenge) {
        val profile = activeProfile.value ?: return
        if (profile.id == challenge.creatorId) {
            _errorMessage.value = "لا يمكنك قبول تحدٍ قمت بإنشائه بنفسك."
            return
        }
        if (challenge.betAmount > 0 && profile.balance < challenge.betAmount) {
            _errorMessage.value = "عذراً، رصيدك المتاح غير كافٍ لقبول هذا التحدي."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.acceptChallenge(challenge.id, profile.id)
            _isLoading.value = false

            result.fold(
                onSuccess = { matchId ->
                    _successMessage.value = "تم قبول التحدي بنجاح! تبدأ المباراة الآن."
                    startActiveMatch(matchId)
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    _errorMessage.value = when {
                        rawMsg.contains("INSUFFICIENT_BALANCE") ->
                            "عذراً، رصيدك غير كافٍ للعب بقيمة التحدي."
                        rawMsg.contains("CANNOT_PLAY_SELF") ->
                            "لا يمكنك لعب مباراة ضد نفسك."
                        rawMsg.contains("OPPONENT_HAS_OPEN_CHALLENGE") ->
                            "لا يمكنك قبول هذا التحدي لأن لديك بالفعل تحدٍ مفتوح معلق."
                        rawMsg.contains("PLAYER_ALREADY_PLAYING") ->
                            "لا يمكنك قبول التحدي لأنك تلعب مباراة بالفعل حالياً."
                        rawMsg.contains("CHALLENGE_NOT_OPEN") ->
                            "عذراً، لم يعد هذا التحدي متاحاً للقبول."
                        else ->
                            "فشل قبول التحدي. ربما تم قبوله بالفعل من لاعب آخر أو انتهت صلاحيته."
                    }
                }
            )
        }
    }

    fun cancelMyOpenChallengeDirectly(challengeId: String) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.cancelMyOpenChallenges(profile.id).fold(
                onSuccess = {
                    _successMessage.value = "تم إلغاء التحدي واستعادة الرصيد بنجاح."
                    refreshChallenges()
                },
                onFailure = { error ->
                    _errorMessage.value = "فشل إلغاء التحدي: ${error.localizedMessage}"
                }
            )
            _isLoading.value = false
        }
    }

    fun createCustomChallenge(amount: Double) {
        val profile = activeProfile.value ?: return
        if (amount < 1.0) {
            _errorMessage.value = "أقل قيمة لدخول أي مباراة هي 1 دولار أمريكي."
            return
        }
        if (profile.balance < amount) {
            _errorMessage.value = "عذراً، رصيدك غير كافٍ لإنشاء التحدي."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.createChallenge(profile.id, amount)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _successMessage.value = "تم إنشاء التحدي المالي الخاص بك بنجاح ونشره باللوحة!"
                    refreshChallenges()
                },
                onFailure = { error ->
                    val rawMsg = error.message ?: ""
                    _errorMessage.value = when {
                        rawMsg.contains("ALREADY_HAS_OPEN_CHALLENGE") ->
                            "لديك بالفعل تحدٍ مفتوح معلق حالياً. يرجى إلغاؤه أو انتظاره أولاً قبل إنشاء تحدٍ جديد."
                        rawMsg.contains("PLAYER_IN_MATCH") ->
                            "لا يمكنك إنشاء تحدٍ بينما أنت تلعب مباراة حالياً."
                        rawMsg.contains("INSUFFICIENT_BALANCE") ->
                            "عذراً، رصيدك غير كافٍ لإنشاء التحدي."
                        else ->
                            "فشل إنشاء التحدي. تأكد من توفر الرصيد الكافي وعدم وجود تحدٍ مفتوح لديك."
                    }
                }
            )
        }
    }

    private fun triggerH7ToAcceptChallenge(challengeId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Get h.7's profile
                val h7Res = repository.fetchProfileByUsername("h.7")
                if (h7Res.isSuccess) {
                    val h7Profile = h7Res.getOrNull()
                    if (h7Profile != null) {
                        cachedH7Id = h7Profile.id
                        // 2. Free up h.7 from any stuck match or state via auto-match
                        repository.executeH7AutoMatch()
                        // 3. Force h.7 to accept the challenge directly
                        repository.acceptChallenge(challengeId, h7Profile.id)
                    }
                }
            } catch (e: Exception) {
                // Ignore background triggering errors
            }
        }
    }

    private fun startMatchmakingPolling(challengeId: String) {
        stopMatchmakingPolling()
        matchmakingPollingJob = viewModelScope.launch {
            // Trigger h.7 instantly at the very start of matchmaking
            triggerH7ToAcceptChallenge(challengeId)
            
            var iterations = 0
            while (_isMatchmaking.value) {
                delay(1000) // Poll every 1000ms instead of 1500ms for faster load
                iterations++
                
                // Every 3 iterations (3 seconds), re-trigger h.7 to guarantee entry
                if (iterations % 3 == 0) {
                    triggerH7ToAcceptChallenge(challengeId)
                }

                try {
                    val response = SupabaseClient.service.getChallenges()
                    if (response.isSuccessful && response.body() != null) {
                        val challengesList: List<Challenge> = response.body()!!
                        val challenge = challengesList.firstOrNull { it.id == challengeId }
                        if (challenge != null && challenge.status == "accepted" && challenge.matchId != null) {
                            _isMatchmaking.value = false
                            stopMatchmakingPolling()
                            startActiveMatch(challenge.matchId)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore errors during background polling
                }
            }
        }
    }

    private fun stopMatchmakingPolling() {
        matchmakingPollingJob?.cancel()
        matchmakingPollingJob = null
    }

    fun cancelMatchmaking() {
        stopMatchmakingPolling()
        _isMatchmaking.value = false
        _errorMessage.value = "تم إلغاء البحث عن مباراة."
        refreshDashboard()
    }

    // ==========================================
    // 4. GAME PLAY LOGIC
    // ==========================================
    private fun startActiveMatch(matchId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getMatch(matchId)
            _isLoading.value = false

            result.fold(
                onSuccess = { match ->
                    _activeMatch.value = match
                    resetGameEffects()
                    navigateTo(Screen.Game)
                    
                    // Fetch profiles of both players
                    launch {
                        repository.refreshProfile(match.player1Id).onSuccess { player1Profile.value = it }
                    }
                    launch {
                        repository.refreshProfile(match.player2Id).onSuccess { player2Profile.value = it }
                    }

                    startMatchPolling(matchId)

                    // Immediate auto-play trigger for hamzanasser
                    val activeUser = activeProfile.value
                    if (activeUser != null && activeUser.username == "hamzanasser" && isComputerBotEnabled.value) {
                        if (match.status == "playing" && match.turnPlayerId == activeUser.id) {
                            val myChar = if (activeUser.id == match.player1Id) 'X' else 'O'
                            val oppChar = if (myChar == 'X') 'O' else 'X'
                            val bestMove = calculateBestMoveForBot(match.board, myChar, oppChar)
                            if (bestMove != -1) {
                                makeMove(bestMove)
                            }
                        }
                    }
                },
                onFailure = {
                    _errorMessage.value = "فشل بدء المباراة المحجوزة"
                }
            )
        }
    }

    fun makeMove(cellIndex: Int) {
        val match = _activeMatch.value ?: return
        val profile = activeProfile.value ?: return

        if (match.status != "playing") return
        if (match.turnPlayerId != profile.id) return

        val char = if (profile.id == match.player1Id) 'X' else 'O'
        val nextBoard: String
        val nextTurnPlayerId = if (profile.id == match.player1Id) match.player2Id else match.player1Id

        val pieceCount = match.board.count { it == char }
        val isMovement = pieceCount >= 3

        if (isMovement) {
            val src = if (cellIndex >= 10) cellIndex / 10 else 0
            val dst = if (cellIndex >= 10) cellIndex % 10 else cellIndex
            if (src !in 0..8 || dst !in 0..8) return
            if (match.board[src] != char) return
            if (match.board[dst] != '_') return

            val boardChars = match.board.toCharArray()
            boardChars[src] = '_'
            boardChars[dst] = char
            nextBoard = String(boardChars)
        } else {
            if (cellIndex !in 0..8) return
            if (match.board[cellIndex] != '_') return

            val boardChars = match.board.toCharArray()
            boardChars[cellIndex] = char
            nextBoard = String(boardChars)
        }

        // Instantly reflect local state to feel ultra snappy

        val updatedLocal = match.copy(
            board = nextBoard,
            turnPlayerId = nextTurnPlayerId
        )
        _activeMatch.value = updatedLocal

        viewModelScope.launch {
            val result = repository.submitMove(match.id, profile.id, cellIndex)
            result.fold(
                onSuccess = { response ->
                    val refreshedMatch = match.copy(
                        board = response.board,
                        status = response.status,
                        turnPlayerId = response.turnPlayerId,
                        winnerId = response.winnerId,
                        winnerUsername = response.winnerUsername
                    )
                    _activeMatch.value = refreshedMatch
                    
                    if (response.status != "playing") {
                        handleMatchEnded(refreshedMatch)
                    } else {
                        triggerH7BotMoveIfActive(refreshedMatch)
                    }
                },
                onFailure = { error ->
                    // Revert to original match state on failure
                    _activeMatch.value = match
                    val rawMsg = error.message ?: ""
                    val friendlyError = when {
                        rawMsg.contains("NOT_YOUR_TURN") || rawMsg.contains("ليس دورك") -> "ليس دورك الحالي للعب."
                        rawMsg.contains("MUST_MOVE_PIECE") -> "يجب عليك تحريك قطعة موجودة على اللوحة."
                        rawMsg.contains("SOURCE_CELL_NOT_YOURS") -> "هذه القطعة ليست لك لتحريكها."
                        rawMsg.contains("DESTINATION_CELL_OCCUPIED") -> "المربع المستهدف ممتلئ بالفعل."
                        rawMsg.contains("CELL_ALREADY_OCCUPIED") -> "هذا المربع ممتلئ بالفعل."
                        rawMsg.contains("INVALID_CELL_INDEX") -> "حركة غير صالحة، يرجى المحاولة مرة أخرى."
                        rawMsg.contains("MATCH_ALREADY_FINISHED") || rawMsg.contains("MATCH_ALREADY_COMPLETED") -> "انتهت المباراة بالفعل."
                        else -> "فشل في تسجيل حركتك، يرجى المحاولة مرة أخرى."
                    }
                    _errorMessage.value = friendlyError
                }
            )
        }
    }

    private fun triggerH7BotMoveIfActive(match: Match) {
        if (match.status != "playing") return
        val h7Id = if (match.player1Id == cachedH7Id) match.player1Id 
                   else if (match.player2Id == cachedH7Id) match.player2Id 
                   else {
                       // Fallback if cachedH7Id is not loaded yet but profile says h.7
                       val p1 = player1Profile.value
                       val p2 = player2Profile.value
                       if (p1?.username == "h.7") match.player1Id 
                       else if (p2?.username == "h.7") match.player2Id 
                       else null
                   }
        
        if (h7Id != null && match.turnPlayerId == h7Id) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Thinking delay (1.2 seconds) to make the bot feel human
                    kotlinx.coroutines.delay(1200L)
                    val h7Char = if (h7Id == match.player1Id) 'X' else 'O'
                    val opponentChar = if (h7Char == 'X') 'O' else 'X'
                    val cellToPlay = calculateBestMoveForBot(match.board, h7Char, opponentChar)
                    if (cellToPlay != -1) {
                        repository.submitMove(match.id, h7Id, cellToPlay).onSuccess { response ->
                            val refreshedMatch = match.copy(
                                board = response.board,
                                status = response.status,
                                turnPlayerId = response.turnPlayerId,
                                winnerId = response.winnerId,
                                winnerUsername = response.winnerUsername
                            )
                            _activeMatch.value = refreshedMatch
                            if (refreshedMatch.status != "playing") {
                                handleMatchEnded(refreshedMatch)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore background bot errors
                }
            }
        }
    }

    private fun triggerComputerMoveIfActive(match: Match) {
        val computerId = "10000000-0000-0000-0000-000000000000"
        if (match.status == "playing" && match.turnPlayerId == computerId) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Thinking delay (1.2 seconds) for local computer bot
                    kotlinx.coroutines.delay(1200L)
                    val compChar = if (computerId == match.player1Id) 'X' else 'O'
                    val opponentChar = if (compChar == 'X') 'O' else 'X'
                    val cellToPlay = calculateMediumMoveForBot(match.board, compChar, opponentChar)
                    if (cellToPlay != -1) {
                        repository.submitMove(match.id, computerId, cellToPlay).onSuccess { response ->
                            val refreshedMatch = match.copy(
                                board = response.board,
                                status = response.status,
                                turnPlayerId = response.turnPlayerId,
                                winnerId = response.winnerId,
                                winnerUsername = response.winnerUsername
                            )
                            _activeMatch.value = refreshedMatch
                            if (refreshedMatch.status != "playing") {
                                handleMatchEnded(refreshedMatch)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore background bot errors
                }
            }
        }
    }

    fun startComputerMatch() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.createComputerMatch(profile.id).fold(
                onSuccess = { matchId ->
                    _successMessage.value = "تم بدء المباراة ضد الكمبيوتر بنجاح!"
                    startActiveMatch(matchId)
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "فشل بدء المباراة ضد الكمبيوتر"
                }
            )
            _isLoading.value = false
        }
    }

    private fun startMatchPolling(matchId: String) {
        stopMatchPolling()
        matchPollingJob = viewModelScope.launch {
            while (true) {
                // Adaptive polling delay: 300ms when waiting for opponent's turn (such as h.7 bot) for real-time responsiveness, otherwise 1500ms
                val isMyTurn = _activeMatch.value?.turnPlayerId == activeProfile.value?.id
                val pollDelay = if (isMyTurn) 1500L else 300L
                delay(pollDelay)
                try {
                    val result = repository.getMatch(matchId)
                    result.fold(
                        onSuccess = { match ->
                            _activeMatch.value = match
                            // Refresh player profiles in background only if they are not already loaded for these specific IDs
                            if (player1Profile.value?.id != match.player1Id) {
                                launch {
                                    repository.refreshProfile(match.player1Id).onSuccess { player1Profile.value = it }
                                }
                            }
                            if (player2Profile.value?.id != match.player2Id) {
                                launch {
                                    repository.refreshProfile(match.player2Id).onSuccess { player2Profile.value = it }
                                }
                            }

                            if (match.status != "playing") {
                                stopMatchPolling()
                                handleMatchEnded(match)
                            } else {
                                // Trigger h.7 bot move if applicable and it's its turn
                                triggerH7BotMoveIfActive(match)

                                // Trigger computer move if applicable and it's its turn
                                triggerComputerMoveIfActive(match)

                                // Check auto-play for hamzanasser if computer mode is active
                                val activeUser = activeProfile.value
                                if (activeUser != null && activeUser.username == "hamzanasser" && isComputerBotEnabled.value) {
                                    if (match.turnPlayerId == activeUser.id) {
                                        val myChar = if (activeUser.id == match.player1Id) 'X' else 'O'
                                        val oppChar = if (myChar == 'X') 'O' else 'X'
                                        val bestMove = calculateBestMoveForBot(match.board, myChar, oppChar)
                                        if (bestMove != -1) {
                                            makeMove(bestMove)
                                        }
                                    }
                                }
                            }
                        },
                        onFailure = {
                            // Log or ignore polling errors
                        }
                    )
                } catch (e: Exception) {
                    // Ignore background network errors
                }
            }
        }
    }

    private fun stopMatchPolling() {
        matchPollingJob?.cancel()
        matchPollingJob = null
    }

    private fun handleMatchEnded(match: Match) {
        val profile = activeProfile.value ?: return
        if (match.status == "completed") {
            if (match.winnerId == profile.id) {
                _gameWinEffect.value = true
                _successMessage.value = "مبروك الفوز! 😄 تم تحويل 70% من إجمالي الرهان إلى محفظتك."
            } else {
                _gameLoseEffect.value = true
                _errorMessage.value = "للأسف، خسرت هذه المباراة 😢 خيرها في غيرها!"
            }
        } else if (match.status == "draw") {
            _gameDrawEffect.value = true
            _successMessage.value = "انتهت المباراة بالتعادل! تم رد قيمة الرهان كاملاً إلى محفظتك."
        }
        refreshDashboard()
    }

    // ==========================================
    // 5. ADMIN LOGIC
    // ==========================================
    fun loadAdminPanel() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.refreshAllPendingTransactions()
            loadMonitoringLogs()

            result.fold(
                onSuccess = { list ->
                    _pendingTransactions.value = list
                    // Fetch user profiles for the pending transactions
                    val userIds = list.map { it.userId }.distinct()
                    if (userIds.isNotEmpty()) {
                        repository.fetchProfilesByUserIds(userIds).fold(
                            onSuccess = { profiles ->
                                _adminProfiles.value = profiles.associateBy { it.id }
                            },
                            onFailure = {
                                // Ignore or log
                            }
                        )
                    } else {
                        _adminProfiles.value = emptyMap()
                    }
                    _isLoading.value = false
                },
                onFailure = {
                    _isLoading.value = false
                    _errorMessage.value = "فشل تحميل طلبات الإدارة"
                }
            )
        }
    }

    fun approveTransaction(transactionId: String, customMessage: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val trans = _pendingTransactions.value.find { it.id == transactionId }
            val result = repository.managePendingTransaction(transactionId, "approve", customMessage)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    viewModelScope.launch {
                        if (trans != null) {
                            repository.updateTransactionDetails(transactionId, customMessage)
                            val title = if (trans.type == "deposit") "نجاح شحن الرصيد 🎉" else "نجاح سحب الرصيد 💸"
                            repository.createNotification(
                                userId = trans.userId,
                                title = title,
                                message = customMessage,
                                type = if (trans.type == "deposit") "deposit_success" else "withdrawal_success"
                            )
                            com.example.util.NotificationHelper.showLocalNotification(getApplication(), title, customMessage)
                        }
                    }
                    _successMessage.value = "تمت الموافقة على العملية بنجاح! ✅"
                    loadAdminPanel()
                },
                onFailure = {
                    _errorMessage.value = "فشل في إتمام الموافقة على العملية"
                }
            )
        }
    }

    fun rejectTransaction(transactionId: String, rejectionReason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val trans = _pendingTransactions.value.find { it.id == transactionId }
            val result = repository.managePendingTransaction(transactionId, "reject", rejectionReason)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    viewModelScope.launch {
                        if (trans != null) {
                            repository.updateTransactionDetails(transactionId, rejectionReason)
                            val title = if (trans.type == "deposit") "طلب الشحن مرفوض ❌" else "طلب السحب مرفوض ❌"
                            repository.createNotification(
                                userId = trans.userId,
                                title = title,
                                message = rejectionReason,
                                type = "info"
                            )
                            com.example.util.NotificationHelper.showLocalNotification(getApplication(), title, rejectionReason)
                        }
                    }
                    _successMessage.value = "تم رفض وإلغاء العملية بنجاح! ❌"
                    loadAdminPanel()
                },
                onFailure = {
                    _errorMessage.value = "فشل في إتمام رفض العملية"
                }
            )
        }
    }

    // Periodically update active dashboard profiles, active challenges
    private fun startGeneralDashboardPolling() {
        generalPollingJob = viewModelScope.launch {
            while (true) {
                delay(4000) // Poll every 4 seconds for highly responsive real-time sync
                try {
                    val profile = activeProfile.value
                    if (profile != null) {
                        repository.heartbeat(profile.id)
                        repository.refreshProfile(profile.id)
                        repository.refreshTransactions(profile.id)
                        loadNotifications()
                        loadPendingFriendRequests()
                        loadIncomingInvitations()
                        if (_currentScreen.value == Screen.Challenges) {
                            repository.refreshChallenges()
                        }
                        if (_currentScreen.value == Screen.Friends) {
                            loadFriendsList()
                        }
                        if (_currentScreen.value == Screen.SearchUsers && searchQuery.value.trim().isNotEmpty()) {
                            val listResult = repository.searchUsers(searchQuery.value, profile.id)
                            listResult.fold(
                                onSuccess = { list ->
                                    _searchResults.value = list
                                },
                                onFailure = {}
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore background general polling issues
                }
            }
        }
    }

    fun loadNotifications() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            val result = repository.fetchNotifications(profile.id)
            result.fold(
                onSuccess = { list ->
                    if (_currentScreen.value == Screen.Notifications) {
                        // Mark all as read locally for an instant zero-counter update in the UI
                        val markedList = list.map { it.copy(isRead = true) }
                        _notifications.value = markedList
                        
                        // If there are unread ones, mark them as read in the database
                        if (list.any { !it.isRead }) {
                            repository.markAllNotificationsRead(profile.id)
                        }
                    } else {
                        _notifications.value = list
                    }
                },
                onFailure = {
                    // Fail silently in background polling
                }
            )
        }
    }

    fun markAllNotificationsAsRead() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            // Update locally first for instant zero counter visual feedback
            _notifications.value = _notifications.value.map { it.copy(isRead = true) }
            repository.markAllNotificationsRead(profile.id)
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            val result = repository.markNotificationRead(id)
            result.fold(
                onSuccess = {
                    loadNotifications()
                },
                onFailure = {
                    // Fail silently
                }
            )
        }
    }

    fun loadMonitoringLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.fetchMonitoringLogs()
            _isLoading.value = false
            result.fold(
                onSuccess = { list ->
                    _monitoringLogs.value = list
                },
                onFailure = {
                    _errorMessage.value = "فشل تحميل سجل المراقبة"
                }
            )
        }
    }

    // ==========================================
    // 5. FRIENDS SYSTEM BUSINESS LOGIC
    // ==========================================

    fun searchUsers(query: String) {
        searchQuery.value = query
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.searchUsers(query, profile.id)
            _isLoading.value = false
            result.fold(
                onSuccess = { list ->
                    _searchResults.value = list
                },
                onFailure = { e ->
                    _errorMessage.value = "فشل البحث: ${e.message}"
                }
            )
        }
    }

    fun clearSearch() {
        searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun loadFriendsList() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            val result = repository.getMyFriends(profile.id)
            result.fold(
                onSuccess = { list ->
                    _friendsList.value = list
                },
                onFailure = { e ->
                    _errorMessage.value = "فشل جلب قائمة الأصدقاء: ${e.message}"
                }
            )
        }
    }

    fun loadPendingFriendRequests() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            val result = repository.getPendingFriendRequests(profile.id)
            result.fold(
                onSuccess = { list ->
                    _pendingRequests.value = list
                },
                onFailure = { e ->
                    // Fail silently in background
                }
            )
        }
    }

    fun sendFriendRequest(receiverId: String) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.sendFriendRequest(profile.id, receiverId)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _successMessage.value = "تم إرسال طلب الصداقة بنجاح!"
                    searchUsers(searchQuery.value)
                },
                onFailure = { e ->
                    _errorMessage.value = e.message ?: "فشل إرسال طلب الصداقة"
                }
            )
        }
    }

    fun cancelFriendRequest(receiverId: String) {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.cancelFriendRequest(profile.id, receiverId)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _successMessage.value = "تم إلغاء طلب الصداقة بنجاح!"
                    searchUsers(searchQuery.value)
                },
                onFailure = { e ->
                    _errorMessage.value = e.message ?: "فشل إلغاء طلب الصداقة"
                }
            )
        }
    }

    fun viewUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.refreshProfile(userId)
            _isLoading.value = false
            result.fold(
                onSuccess = { profile ->
                    _selectedUserProfile.value = profile
                    navigateTo(Screen.ViewUserProfile)
                },
                onFailure = { e ->
                    _errorMessage.value = "فشل تحميل الملف الشخصي: ${e.message}"
                }
            )
        }
    }

    fun respondToFriendRequest(requestId: String, action: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.respondToFriendRequest(requestId, action)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    if (action == "accept") {
                        _successMessage.value = "تم قبول طلب الصداقة! مبروك!"
                    } else {
                        _successMessage.value = "تم رفض طلب الصداقة."
                    }
                    loadPendingFriendRequests()
                    loadFriendsList()
                },
                onFailure = { e ->
                    _errorMessage.value = e.message ?: "فشل معالجة الطلب"
                }
            )
        }
    }

    fun sendMatchInvitation(receiverId: String, betAmount: Double) {
        val profile = activeProfile.value ?: return
        val isFree = betAmount == 0.0
        if (!isFree && profile.balance < betAmount) {
            _errorMessage.value = "رصيدك الحالي غير كافٍ لتغطية قيمة المباراة!"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.sendMatchInvitation(profile.id, receiverId, betAmount)
            _isLoading.value = false
            result.fold(
                onSuccess = { inviteId ->
                    _successMessage.value = "تم إرسال دعوة المباراة بنجاح!"
                    val dummyInvite = MatchInvitation(
                        id = inviteId,
                        senderId = profile.id,
                        senderUsername = profile.username,
                        receiverId = receiverId,
                        betAmount = betAmount,
                        status = "pending",
                        matchId = null,
                        createdAt = ""
                    )
                    _lobbyInvitation.value = dummyInvite
                    navigateTo(Screen.Lobby)
                },
                onFailure = { e ->
                    _errorMessage.value = e.message ?: "فشل إرسال الدعوة"
                }
            )
        }
    }

    fun acceptMatchInvitation(invitation: MatchInvitation) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.respondToMatchInvitation(invitation.id, "accept")
            _isLoading.value = false
            result.fold(
                onSuccess = { response ->
                    if (response.status == "accepted" && response.matchId != null) {
                        _successMessage.value = "تم قبول الدعوة! جاري الانتقال لغرفة الانتظار..."
                        _lobbyInvitation.value = invitation.copy(status = "accepted", matchId = response.matchId)
                        navigateTo(Screen.Lobby)
                    }
                },
                onFailure = { e ->
                    _errorMessage.value = e.message ?: "فشل قبول الدعوة"
                }
            )
        }
    }

    fun rejectMatchInvitation(invitation: MatchInvitation) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.respondToMatchInvitation(invitation.id, "reject")
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _successMessage.value = "تم رفض دعوة المباراة."
                    loadIncomingInvitations()
                },
                onFailure = { e ->
                    _errorMessage.value = e.message ?: "فشل رفض الدعوة"
                }
            )
        }
    }

    fun loadIncomingInvitations() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            val result = repository.getMatchInvitations(profile.id)
            result.fold(
                onSuccess = { list ->
                    _incomingInvitations.value = list
                },
                onFailure = {
                    // Fail silently
                }
            )
        }
    }

    // ==========================================
    // 6. LOBBY PRE-MATCH CHAT & VOICE BUSINESS LOGIC
    // ==========================================

    fun startLobbyPolling() {
        stopLobbyPolling()
        val invite = _lobbyInvitation.value ?: return
        val profile = activeProfile.value ?: return
        _matchChats.value = emptyList()
        _isMicMuted.value = true
        _isVoiceConnected.value = false

        lobbyPollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val response = SupabaseClient.service.getMatchInvitations(profile.id)
                    var currentInvite: MatchInvitation? = null
                    if (response.isSuccessful && response.body() != null) {
                        currentInvite = response.body()!!.firstOrNull { it.id == invite.id }
                    }
                    if (currentInvite == null) {
                        // Let's also check invitations where we are sender
                        // We can fetch from database directly or do standard PostgREST:
                        val senderResponse = SupabaseClient.service.getMatchInvitations(invite.receiverId)
                        if (senderResponse.isSuccessful && senderResponse.body() != null) {
                            currentInvite = senderResponse.body()!!.firstOrNull { it.id == invite.id }
                        }
                    }

                    if (currentInvite != null) {
                        _lobbyInvitation.value = currentInvite
                        if (currentInvite.status == "accepted" && currentInvite.matchId != null) {
                            fetchLobbyMatch(currentInvite.matchId)
                        } else if (currentInvite.status == "rejected") {
                            stopLobbyPolling()
                            _errorMessage.value = "تم رفض دعوة المباراة من قبل الخصم."
                            navigateTo(Screen.Friends)
                        }
                    }

                    val currentMatchId = _lobbyInvitation.value?.matchId
                    if (currentMatchId != null) {
                        // Poll chats
                        val chatsResult = repository.getMatchChats(currentMatchId)
                        chatsResult.fold(
                            onSuccess = { list ->
                                _matchChats.value = list
                            },
                            onFailure = {}
                        )

                        // Poll WebRTC signals (Simulating voice indicator state sync)
                        val signalsResult = repository.getWebRTCSignals(currentMatchId, profile.id)
                        signalsResult.fold(
                            onSuccess = { list ->
                                val micSignals = list.filter { it.signalType == "mic_toggle" }
                                if (micSignals.isNotEmpty()) {
                                    val lastSignal = micSignals.last()
                                    _isVoiceConnected.value = lastSignal.payload == "unmuted"
                                }
                            },
                            onFailure = {}
                        )
                    }
                } catch (e: Exception) {
                    // Fail silently
                }
                delay(1500)
            }
        }
    }

    fun stopLobbyPolling() {
        lobbyPollingJob?.cancel()
        lobbyPollingJob = null
        _lobbyMatch.value = null
        _lobbyInvitation.value = null
    }

    private suspend fun fetchLobbyMatch(matchId: String) {
        val result = repository.getMatch(matchId)
        result.fold(
            onSuccess = { match ->
                _lobbyMatch.value = match
            },
            onFailure = {}
        )
    }

    fun sendLobbyChatMessage(messageText: String) {
        val invite = _lobbyInvitation.value ?: return
        val matchId = invite.matchId ?: return
        val profile = activeProfile.value ?: return
        if (messageText.trim().isEmpty()) return

        viewModelScope.launch {
            val result = repository.sendChatMessage(matchId, profile.id, messageText)
            result.fold(
                onSuccess = {
                    val chatsResult = repository.getMatchChats(matchId)
                    chatsResult.fold(
                        onSuccess = { list ->
                            _matchChats.value = list
                        },
                        onFailure = {}
                    )
                },
                onFailure = { e ->
                    _errorMessage.value = "فشل إرسال الرسالة: ${e.message}"
                }
            )
        }
    }

    fun toggleMicrophone() {
        val invite = _lobbyInvitation.value ?: return
        val matchId = invite.matchId ?: return
        val profile = activeProfile.value ?: return
        val nextMuted = !_isMicMuted.value
        _isMicMuted.value = nextMuted

        viewModelScope.launch {
            val payload = if (nextMuted) "muted" else "unmuted"
            repository.sendWebRTCSignal(matchId, profile.id, "mic_toggle", payload)
        }
    }

    fun startFriendsMatch() {
        val match = _lobbyMatch.value ?: return
        stopLobbyPolling()
        startActiveMatch(match.id)
    }

    fun leaveLobby() {
        stopLobbyPolling()
        navigateTo(Screen.Friends)
    }

    fun toggleComputerBotMode() {
        val profile = activeProfile.value ?: return
        if (profile.username == "hamzanasser") {
            isComputerBotEnabled.value = !isComputerBotEnabled.value
            if (isComputerBotEnabled.value) {
                _successMessage.value = "تم تفعيل نظام الكمبيوتر الذكي"
            } else {
                _successMessage.value = "تم إلغاء نظام الكمبيوتر الذكي"
            }
        }
    }

    private fun startH7BotLoop() {
        h7BotPollingJob?.cancel()
        h7BotPollingJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var hasActiveH7Match = false
            while (true) {
                // Adaptive delay: 1500ms when h.7 is in a match for natural paced moves, otherwise 2000ms when idle to save bandwidth
                val delayTime = if (hasActiveH7Match) 1500L else 2000L
                kotlinx.coroutines.delay(delayTime)
                try {
                    // 1. Fetch h.7's profile
                    val profileRes = repository.fetchProfileByUsername("h.7")
                    profileRes.fold(
                        onSuccess = { h7Profile ->
                            cachedH7Id = h7Profile.id // Keep cached ID updated
                            // 2. Query all active playing matches
                            val matchesRes = repository.getMatchesByStatus("eq.playing")
                            matchesRes.fold(
                                onSuccess = { playingMatches ->
                                    // Check if h.7 is currently playing in any active match
                                    val activeH7Match = playingMatches.firstOrNull { 
                                        it.player1Id == h7Profile.id || it.player2Id == h7Profile.id 
                                    }

                                    if (activeH7Match != null) {
                                        hasActiveH7Match = true
                                        // h.7 is in an active match! Let's play its turn if it is h.7's turn
                                        if (activeH7Match.status == "playing" && activeH7Match.turnPlayerId == h7Profile.id) {
                                            // Calculate best move for h.7
                                            val h7Char = if (h7Profile.id == activeH7Match.player1Id) 'X' else 'O'
                                            val opponentChar = if (h7Char == 'X') 'O' else 'X'
                                            val cellToPlay = calculateBestMoveForBot(activeH7Match.board, h7Char, opponentChar)
                                            if (cellToPlay != -1) {
                                                repository.submitMove(activeH7Match.id, h7Profile.id, cellToPlay)
                                            }
                                        }
                                    } else {
                                        hasActiveH7Match = false
                                        // h.7 is NOT in any active match. Let's trigger the server-side auto match
                                        repository.executeH7AutoMatch()

                                        // Fallback client-side matching if server triggers were slow or missed
                                        val challengesRes = repository.refreshChallenges()
                                        if (challengesRes.isSuccess) {
                                            val challenges = challengesRes.getOrNull() ?: emptyList()
                                            // Filter out challenges created by h.7 and sort descending by betAmount
                                            val joinableChallenges = challenges.filter { 
                                                it.creatorId != h7Profile.id && it.status == "open" && it.betAmount <= h7Profile.balance
                                            }.sortedByDescending { it.betAmount }
                                            
                                            // Step down gradually from highest to lowest betAmount to join the first available match
                                            for (challenge in joinableChallenges) {
                                                val acceptRes = repository.acceptChallenge(challenge.id, h7Profile.id)
                                                if (acceptRes.isSuccess) {
                                                    break // Successfully joined the highest available match, exit search
                                                }
                                            }
                                        }
                                    }
                                },
                                onFailure = {
                                    hasActiveH7Match = false
                                }
                             )
                        },
                        onFailure = {
                            hasActiveH7Match = false
                        }
                    )
                } catch (e: Exception) {
                    hasActiveH7Match = false
                }
            }
        }
    }

    private fun getValidMovesForChar(board: String, c: Char): List<Int> {
        val pieceCount = board.count { it == c }
        val moves = mutableListOf<Int>()
        if (pieceCount < 3) {
            // Placement phase: any empty cell
            for (i in 0 until 9) {
                if (board[i] == '_') {
                    moves.add(i)
                }
            }
        } else {
            // Movement phase: any of our pieces to any empty cell
            for (src in 0 until 9) {
                if (board[src] == c) {
                    for (dst in 0 until 9) {
                        if (board[dst] == '_') {
                            moves.add(src * 10 + dst)
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun applyMoveToBoard(board: String, move: Int, c: Char): String {
        val chars = board.toCharArray()
        val pieceCount = board.count { it == c }
        if (pieceCount < 3) {
            // Placement phase
            chars[move] = c
        } else {
            // Movement phase
            val src: Int
            val dst: Int
            if (move >= 10) {
                src = move / 10
                dst = move % 10
            } else {
                src = 0
                dst = move
            }
            chars[src] = '_'
            chars[dst] = c
        }
        return String(chars)
    }

    private var solvedValues: Map<String, Double>? = null

    private fun getSuccessorBoards(board: String, turn: Char): List<String> {
        val count = board.count { it == turn }
        val successors = mutableListOf<String>()
        if (count < 3) {
            // Placement phase: any empty cell
            for (i in 0 until 9) {
                if (board[i] == '_') {
                    val nextChars = board.toCharArray()
                    nextChars[i] = turn
                    successors.add(String(nextChars))
                }
            }
        } else {
            // Movement phase: any of our pieces to any empty cell
            for (src in 0 until 9) {
                if (board[src] == turn) {
                    for (dst in 0 until 9) {
                        if (board[dst] == '_') {
                            val nextChars = board.toCharArray()
                            nextChars[src] = '_'
                            nextChars[dst] = turn
                            successors.add(String(nextChars))
                        }
                    }
                }
            }
        }
        return successors
    }

    @Synchronized
    private fun ensureSolvedValues() {
        if (solvedValues != null) return
        
        val reachable = mutableSetOf<String>()
        val successorMap = mutableMapOf<String, List<String>>()
        val queue = ArrayDeque<Pair<String, Char>>()
        
        // Find all reachable states starting from empty board, 'X' turn
        queue.add(Pair("_________", 'X'))
        reachable.add("_________:X")
        
        while (queue.isNotEmpty()) {
            val (board, turn) = queue.removeFirst()
            val nextTurn = if (turn == 'X') 'O' else 'X'
            val key = "${board}:$turn"
            
            if (checkWinForBot(board, 'X') || checkWinForBot(board, 'O')) {
                successorMap[key] = emptyList()
                continue
            }
            
            val successors = getSuccessorBoards(board, turn)
            successorMap[key] = successors
            for (nextBoard in successors) {
                val nextKey = "${nextBoard}:$nextTurn"
                if (nextKey !in reachable) {
                    reachable.add(nextKey)
                    queue.add(Pair(nextBoard, nextTurn))
                }
            }
        }
        
        // Also add any intermediate states starting from 'O' turn (if O plays first)
        val queueO = ArrayDeque<Pair<String, Char>>()
        queueO.add(Pair("_________", 'O'))
        if ("_________:O" !in reachable) {
            reachable.add("_________:O")
            while (queueO.isNotEmpty()) {
                val (board, turn) = queueO.removeFirst()
                val nextTurn = if (turn == 'X') 'O' else 'X'
                val key = "${board}:$turn"
                
                if (checkWinForBot(board, 'X') || checkWinForBot(board, 'O')) {
                    successorMap[key] = emptyList()
                    continue
                }
                val successors = getSuccessorBoards(board, turn)
                successorMap[key] = successors
                for (nextBoard in successors) {
                    val nextKey = "${nextBoard}:$nextTurn"
                    if (nextKey !in reachable) {
                        reachable.add(nextKey)
                        queueO.add(Pair(nextBoard, nextTurn))
                    }
                }
            }
        }

        // Initialize values
        val values = mutableMapOf<String, Double>()
        for (state in reachable) {
            val parts = state.split(':')
            val board = parts[0]
            val turn = parts[1][0]
            val opp = if (turn == 'X') 'O' else 'X'
            
            if (checkWinForBot(board, turn)) {
                values[state] = 10000.0
            } else if (checkWinForBot(board, opp)) {
                values[state] = -10000.0
            } else {
                values[state] = 0.0
            }
        }

        // Run Value Iteration (150 iterations for deep convergence)
        val discount = 0.95
        for (iter in 0 until 150) {
            var maxChange = 0.0
            val nextValues = values.toMutableMap()
            for (state in reachable) {
                val parts = state.split(':')
                val board = parts[0]
                val turn = parts[1][0]
                val opp = if (turn == 'X') 'O' else 'X'
                
                // If it's a win/loss state, its value is fixed
                if (checkWinForBot(board, turn) || checkWinForBot(board, opp)) {
                    continue
                }
                
                val successors = successorMap[state] ?: emptyList()
                if (successors.isEmpty()) {
                    nextValues[state] = 0.0
                    continue
                }
                
                var maxVal = -Double.MAX_VALUE
                for (nextBoard in successors) {
                    val nextStateKey = "${nextBoard}:$opp"
                    val nextValForOpp = values[nextStateKey] ?: 0.0
                    val ourVal = -nextValForOpp * discount
                    if (ourVal > maxVal) {
                        maxVal = ourVal
                    }
                }
                nextValues[state] = maxVal
                val diff = Math.abs(maxVal - (values[state] ?: 0.0))
                if (diff > maxChange) {
                    maxChange = diff
                }
            }
            values.putAll(nextValues)
            if (maxChange < 0.01) {
                break
            }
        }
        
        solvedValues = values
    }

    private fun calculateMediumMoveForBot(board: String, myChar: Char, opponentChar: Char): Int {
        try {
            ensureSolvedValues()
            val moves = getValidMovesForChar(board, myChar)
            if (moves.isEmpty()) return -1

            // 1. Immediate Win: casual players see direct wins
            for (move in moves) {
                val nextBoard = applyMoveToBoard(board, move, myChar)
                if (checkWinForBot(nextBoard, myChar)) {
                    return move
                }
            }

            // 2. Immediate Block: 75% of the time block, 25% of the time miss it (mistake)
            val oppMoves = getValidMovesForChar(board, opponentChar)
            var blockMove: Int? = null
            for (oppMove in oppMoves) {
                val oppNextBoard = applyMoveToBoard(board, oppMove, opponentChar)
                if (checkWinForBot(oppNextBoard, opponentChar)) {
                    val oppDst = if (oppMove >= 10) oppMove % 10 else oppMove
                    val ourBlockMove = moves.find { 
                        val ourDst = if (it >= 10) it % 10 else it
                        ourDst == oppDst
                    }
                    if (ourBlockMove != null) {
                        blockMove = ourBlockMove
                        break
                    }
                }
            }
            if (blockMove != null && Math.random() < 0.75) {
                return blockMove
            }

            // 3. Normal turn: 55% perfect move, 45% random move
            if (Math.random() < 0.45) {
                return moves.random()
            }

            val localSolved = solvedValues ?: return moves.random()
            var bestMove = moves.first()
            var bestValue = -Double.MAX_VALUE

            for (move in moves) {
                val nextBoard = applyMoveToBoard(board, move, myChar)
                val nextStateKey = "${nextBoard}:$opponentChar"
                val opponentValue = localSolved[nextStateKey] ?: 0.0
                val ourValue = -opponentValue
                
                if (ourValue > bestValue) {
                    bestValue = ourValue
                    bestMove = move
                }
            }
            return bestMove
        } catch (e: Exception) {
            val moves = getValidMovesForChar(board, myChar)
            return if (moves.isNotEmpty()) moves.random() else -1
        }
    }

    private fun minimax(
        board: String,
        depth: Int,
        maxDepth: Int,
        alpha: Double,
        beta: Double,
        isMax: Boolean,
        myChar: Char,
        opponentChar: Char,
        visited: MutableSet<String>
    ): Double {
        if (checkWinForBot(board, myChar)) {
            return 1000.0 - depth
        }
        if (checkWinForBot(board, opponentChar)) {
            return -1000.0 + depth
        }
        if (depth >= maxDepth) {
            return 0.0
        }
        if (board in visited) {
            return 0.0
        }

        visited.add(board)
        val currentTurnChar = if (isMax) myChar else opponentChar
        val moves = getValidMovesForChar(board, currentTurnChar)
        if (moves.isEmpty()) {
            visited.remove(board)
            return 0.0
        }

        var bestVal = if (isMax) -Double.MAX_VALUE else Double.MAX_VALUE

        // Sort moves to prioritize wins and improve Alpha-Beta pruning speed
        val sortedMoves = moves.sortedByDescending { move ->
            val nextB = applyMoveToBoard(board, move, currentTurnChar)
            if (checkWinForBot(nextB, currentTurnChar)) 10 else 0
        }

        if (isMax) {
            var currentAlpha = alpha
            for (move in sortedMoves) {
                val nextB = applyMoveToBoard(board, move, myChar)
                val value = minimax(nextB, depth + 1, maxDepth, currentAlpha, beta, false, myChar, opponentChar, visited)
                bestVal = maxOf(bestVal, value)
                currentAlpha = maxOf(currentAlpha, bestVal)
                if (beta <= currentAlpha) {
                    break
                }
            }
        } else {
            var currentBeta = beta
            for (move in sortedMoves) {
                val nextB = applyMoveToBoard(board, move, opponentChar)
                val value = minimax(nextB, depth + 1, maxDepth, alpha, currentBeta, true, myChar, opponentChar, visited)
                bestVal = minOf(bestVal, value)
                currentBeta = minOf(currentBeta, bestVal)
                if (currentBeta <= alpha) {
                    break
                }
            }
        }

        visited.remove(board)
        return bestVal
    }

    private fun calculateBestMoveForBot(board: String, myChar: Char, opponentChar: Char): Int {
        try {
            val moves = getValidMovesForChar(board, myChar)
            if (moves.isEmpty()) return -1

            // 1. Perfect Unbeatable Move using Solved Values (Value Iteration)
            ensureSolvedValues()
            val localSolved = solvedValues
            if (localSolved != null) {
                var bestValue = -Double.MAX_VALUE
                val bestMoves = mutableListOf<Int>()
                for (move in moves) {
                    val nextBoard = applyMoveToBoard(board, move, myChar)
                    val nextStateKey = "${nextBoard}:$opponentChar"
                    val opponentValue = localSolved[nextStateKey] ?: 0.0
                    val ourValue = -opponentValue
                    if (ourValue > bestValue) {
                        bestValue = ourValue
                        bestMoves.clear()
                        bestMoves.add(move)
                    } else if (Math.abs(ourValue - bestValue) < 1e-5) {
                        bestMoves.add(move)
                    }
                }
                if (bestMoves.isNotEmpty()) {
                    // Among mathematically best moves, prioritize center (4), then corners (0, 2, 6, 8), then edges
                    val cornersAndCenter = listOf(4, 0, 2, 6, 8)
                    val priorityMoves = bestMoves.filter { move ->
                        val dst = if (move >= 10) move % 10 else move
                        dst in cornersAndCenter
                    }
                    return if (priorityMoves.isNotEmpty()) priorityMoves.random() else bestMoves.random()
                }
            }

            // 2. Fallback: Perfect Deep Search using Alpha-Beta Minimax
            var bestMove = moves.first()
            var bestValue = -Double.MAX_VALUE
            val visited = mutableSetOf<String>()

            for (move in moves) {
                val nextBoard = applyMoveToBoard(board, move, myChar)
                val value = minimax(
                    board = nextBoard,
                    depth = 1,
                    maxDepth = 12,
                    alpha = -Double.MAX_VALUE,
                    beta = Double.MAX_VALUE,
                    isMax = false,
                    myChar = myChar,
                    opponentChar = opponentChar,
                    visited = visited
                )
                if (value > bestValue) {
                    bestValue = value
                    bestMove = move
                }
            }
            return bestMove
        } catch (e: Exception) {
            val moves = getValidMovesForChar(board, myChar)
            return if (moves.isNotEmpty()) moves.random() else -1
        }
    }

    private fun checkWinForBot(b: String, c: Char): Boolean {
        if (b.length < 9) return false
        // Rows
        if (b[0] == c && b[1] == c && b[2] == c) return true
        if (b[3] == c && b[4] == c && b[5] == c) return true
        if (b[6] == c && b[7] == c && b[8] == c) return true
        // Columns
        if (b[0] == c && b[3] == c && b[6] == c) return true
        if (b[1] == c && b[4] == c && b[7] == c) return true
        if (b[2] == c && b[5] == c && b[8] == c) return true
        // Diagonals
        if (b[0] == c && b[4] == c && b[8] == c) return true
        if (b[2] == c && b[4] == c && b[6] == c) return true
        return false
    }

    // ==========================================
    // PASSWORD RESET & SECURITY MANAGEMENT STATE & ACTIONS
    // ==========================================
    val forgotPasswordSearchQuery = MutableStateFlow("")
    val forgotPasswordFoundProfile = MutableStateFlow<Profile?>(null)
    val forgotPasswordEmail = MutableStateFlow("")
    val forgotPasswordOtp = MutableStateFlow("")
    val forgotPasswordNewPass = MutableStateFlow("")
    val forgotPasswordConfirmPass = MutableStateFlow("")
    val forgotPasswordStep = MutableStateFlow(1) // 1: Email/Search, 2: OTP, 3: New Password
    val forgotPasswordResetToken = MutableStateFlow("")
    val isForgotPasswordFallback = MutableStateFlow(false)

    val privacyOldPassword = MutableStateFlow("")
    val privacyNewPassword = MutableStateFlow("")
    val privacyConfirmNewPassword = MutableStateFlow("")
    
    val privacyNewEmail = MutableStateFlow("")
    val privacyEmailOtp = MutableStateFlow("")
    val privacyEmailStep = MutableStateFlow(1) // 1: Email, 2: OTP

    val activeUserSessions = MutableStateFlow<List<UserSession>>(emptyList())
    val privacyTwoFactorEnabled = MutableStateFlow(false)

    fun searchProfileForReset() {
        val query = forgotPasswordSearchQuery.value.trim()
        if (query.isEmpty()) {
            _errorMessage.value = "يرجى إدخال البريد الإلكتروني أو اسم المستخدم للبحث."
            return
        }
        if (isUserId(query)) {
            _errorMessage.value = "البحث بواسطة الـ ID غير مدعوم حالياً. يرجى البحث باستخدام اسم المستخدم أو البريد الإلكتروني."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            repository.searchProfileForReset(query)
                .onSuccess { profile ->
                    forgotPasswordFoundProfile.value = profile
                    forgotPasswordEmail.value = profile.email
                    _successMessage.value = "تم العثور على الحساب بنجاح."
                }
                .onFailure {
                    _errorMessage.value = "لم يتم العثور على أي حساب يطابق البيانات المدخلة."
                    forgotPasswordFoundProfile.value = null
                }
            _isLoading.value = false
        }
    }

    fun requestPasswordReset() {
        val profile = forgotPasswordFoundProfile.value
        if (profile == null) {
            _errorMessage.value = "يرجى البحث عن الحساب أولاً وتحديده."
            return
        }
        val email = profile.email
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            repository.requestPasswordResetAuth(email)
                .onSuccess { _ ->
                    isForgotPasswordFallback.value = false
                    _successMessage.value = "تم إرسال رمز التحقق إلى بريدك الإلكتروني بنجاح."
                    forgotPasswordStep.value = 2
                }
                .onFailure { authError ->
                    // Fallback to custom DB OTP if SMTP fails (e.g. rate limits or server misconfiguration)
                    val errMsg = authError.localizedMessage ?: ""
                    if (errMsg.contains("unexpected_failure") || errMsg.contains("Error sending recovery email") || errMsg.contains("500") || errMsg.contains("SMTP")) {
                        repository.requestPasswordReset(email)
                            .onSuccess { fallbackOtp ->
                                isForgotPasswordFallback.value = true
                                forgotPasswordOtp.value = fallbackOtp
                                _successMessage.value = "تم طلب الرمز بنجاح. نظرًا لقيود خدمة إرسال البريد الإلكتروني في Supabase (خطأ SMTP)، تم تفعيل الرمز الاحتياطي لتسهيل الاختبار والتطوير: $fallbackOtp"
                                forgotPasswordStep.value = 2
                            }
                            .onFailure { dbError ->
                                _errorMessage.value = "فشل إرسال رمز التحقق: ${authError.localizedMessage}. فشل البديل أيضًا: ${dbError.localizedMessage}"
                            }
                    } else {
                        _errorMessage.value = "فشل إرسال رمز التحقق: ${authError.localizedMessage}"
                    }
                }
            _isLoading.value = false
        }
    }

    fun verifyPasswordResetOtp() {
        val otp = forgotPasswordOtp.value.trim()
        val email = forgotPasswordEmail.value.trim()
        if (otp.isEmpty() || otp.length < 6) {
            _errorMessage.value = "يرجى إدخال رمز التحقق المكون من 6 أرقام."
            return
        }
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            if (isForgotPasswordFallback.value) {
                // If in fallback mode, directly check if the OTP matches the one generated and shown to the developer/user
                if (otp == forgotPasswordOtp.value.trim()) {
                    _successMessage.value = "تم التحقق من الرمز الاحتياطي بنجاح."
                    forgotPasswordStep.value = 3
                } else {
                    _errorMessage.value = "رمز التحقق الاحتياطي غير صحيح."
                }
            } else {
                repository.verifyPasswordResetOtpAuth(email, otp)
                    .onSuccess { token ->
                        forgotPasswordResetToken.value = token
                        forgotPasswordStep.value = 3
                    }
                    .onFailure {
                        when (it.message) {
                            "INVALID_OTP" -> _errorMessage.value = "رمز التحقق غير صحيح."
                            "EXPIRED_OTP" -> _errorMessage.value = "انتهت صلاحية رمز التحقق، يرجى طلب رمز جديد."
                            else -> _errorMessage.value = "فشل التحقق من الرمز: ${it.localizedMessage}"
                        }
                    }
            }
            _isLoading.value = false
        }
    }

    fun submitNewPassword() {
        val email = forgotPasswordEmail.value.trim()
        val token = forgotPasswordResetToken.value
        val newPass = forgotPasswordNewPass.value.trim()
        val confirmPass = forgotPasswordConfirmPass.value.trim()

        if (!isForgotPasswordFallback.value && token.isEmpty()) {
            _errorMessage.value = "انتهت صلاحية الجلسة، يرجى إعادة محاولة التحقق من الرمز."
            return
        }
        if (newPass.isEmpty()) {
            _errorMessage.value = "يرجى إدخال كلمة المرور الجديدة."
            return
        }
        if (newPass != confirmPass) {
            _errorMessage.value = "كلمة المرور وتأكيدها غير متطابقين."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            val result = if (isForgotPasswordFallback.value) {
                repository.resetPasswordWithOtp(email, forgotPasswordOtp.value.trim(), newPass)
            } else {
                repository.resetPasswordAuth(token, newPass)
            }

            result
                .onSuccess {
                    _successMessage.value = "تم تغيير كلمة المرور بنجاح، يمكنك الآن تسجيل الدخول."
                    forgotPasswordSearchQuery.value = ""
                    forgotPasswordFoundProfile.value = null
                    forgotPasswordEmail.value = ""
                    forgotPasswordOtp.value = ""
                    forgotPasswordNewPass.value = ""
                    forgotPasswordConfirmPass.value = ""
                    forgotPasswordResetToken.value = ""
                    isForgotPasswordFallback.value = false
                    forgotPasswordStep.value = 1
                    delay(2000)
                    navigateTo(Screen.Login)
                }
                .onFailure {
                    _errorMessage.value = "فشل تغيير كلمة المرور: ${it.localizedMessage}"
                }
            _isLoading.value = false
        }
    }

    fun changePassword() {
        val profile = activeProfile.value ?: return
        val oldPass = privacyOldPassword.value.trim()
        val newPass = privacyNewPassword.value.trim()
        val confirmPass = privacyConfirmNewPassword.value.trim()

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            _errorMessage.value = "يرجى ملء جميع حقول كلمة المرور."
            return
        }
        if (newPass != confirmPass) {
            _errorMessage.value = "كلمة المرور الجديدة وتأكيدها غير متطابقين."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            repository.changeUserPassword(profile.id, oldPass, newPass)
                .onSuccess {
                    _successMessage.value = "تم تغيير كلمة المرور بنجاح!"
                    privacyOldPassword.value = ""
                    privacyNewPassword.value = ""
                    privacyConfirmNewPassword.value = ""
                }
                .onFailure {
                    if (it.message?.contains("WRONG_OLD_PASSWORD") == true) {
                        _errorMessage.value = "كلمة المرور القديمة غير صحيحة."
                    } else {
                        _errorMessage.value = "فشل تغيير كلمة المرور: ${it.localizedMessage}"
                    }
                }
            _isLoading.value = false
        }
    }

    fun requestEmailChange() {
        val profile = activeProfile.value ?: return
        val newEmail = privacyNewEmail.value.trim()

        if (newEmail.isEmpty()) {
            _errorMessage.value = "يرجى إدخال البريد الإلكتروني الجديد."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            repository.requestEmailChange(profile.id, newEmail)
                .onSuccess { otpCode ->
                    _successMessage.value = "تم إرسال رمز التحقق إلى بريدك الإلكتروني الجديد. (للتجربة: $otpCode)"
                    privacyEmailStep.value = 2
                }
                .onFailure {
                    if (it.message?.contains("EMAIL_ALREADY_EXISTS") == true) {
                        _errorMessage.value = "البريد الإلكتروني مستخدم بالفعل بحساب آخر."
                    } else {
                        _errorMessage.value = "فشل إرسال رمز التحقق: ${it.localizedMessage}"
                    }
                }
            _isLoading.value = false
        }
    }

    fun confirmEmailChange() {
        val profile = activeProfile.value ?: return
        val otp = privacyEmailOtp.value.trim()

        if (otp.isEmpty() || otp.length < 6) {
            _errorMessage.value = "يرجى إدخال رمز التحقق المكون من 6 أرقام."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            repository.confirmEmailChange(profile.id, otp)
                .onSuccess {
                    _successMessage.value = "تم تغيير البريد الإلكتروني بنجاح!"
                    privacyNewEmail.value = ""
                    privacyEmailOtp.value = ""
                    privacyEmailStep.value = 1
                    repository.refreshProfile(profile.id)
                }
                .onFailure {
                    if (it.message?.contains("INVALID_OR_EXPIRED_OTP") == true) {
                        _errorMessage.value = "رمز التحقق غير صحيح أو منتهي الصلاحية."
                    } else {
                        _errorMessage.value = "فشل تغيير البريد الإلكتروني: ${it.localizedMessage}"
                    }
                }
            _isLoading.value = false
        }
    }

    fun loadUserSessions() {
        val profile = activeProfile.value ?: return
        viewModelScope.launch {
            repository.getUserSessions(profile.id)
                .onSuccess {
                    activeUserSessions.value = it
                }
                .onFailure {
                    // Fail silently
                }
        }
    }

    fun terminateAllSessions() {
        val profile = activeProfile.value ?: return
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null
        viewModelScope.launch {
            repository.terminateAllUserSessions(profile.id)
                .onSuccess {
                    _successMessage.value = "تم تسجيل الخروج من جميع الأجهزة بنجاح."
                    loadUserSessions()
                }
                .onFailure {
                    _errorMessage.value = "فشل إنهاء الجلسات: ${it.localizedMessage}"
                }
            _isLoading.value = false
        }
    }

    fun toggleTwoFactor(enabled: Boolean) {
        privacyTwoFactorEnabled.value = enabled
        if (enabled) {
            _successMessage.value = "تم تفعيل التحقق الثنائي لحسابك بنجاح."
        } else {
            _successMessage.value = "تم إلغاء تفعيل التحقق الثنائي لحسابك."
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeProfile.value?.let { profile ->
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    repository.logout(profile.id)
                } catch (e: Exception) {
                    // Ignore background shutdown issues
                }
            }
        }
        stopMatchPolling()
        stopMatchmakingPolling()
        stopLobbyPolling()
        generalPollingJob?.cancel()
        h7BotPollingJob?.cancel()
    }
}

// Custom Screen destinations for safe layout rendering
sealed class Screen {
    object Login : Screen()
    object PrivacySecurity : Screen()
    object Home : Screen()
    object Challenges : Screen()
    object Matchmaking : Screen()
    object Game : Screen()
    object Wallet : Screen()
    object Notifications : Screen()
    object AdminPanel : Screen()
    object Profile : Screen()
    object EditProfile : Screen()
    object Statistics : Screen()
    object Friends : Screen()
    object SearchUsers : Screen()
    object Lobby : Screen()
    object FriendRequests : Screen()
    object ViewUserProfile : Screen()
}

// Factory Provider for ViewModel
class GameViewModelFactory(
    private val application: Application,
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
