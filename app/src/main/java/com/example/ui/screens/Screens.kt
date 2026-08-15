package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.components.FloatingAppMessageBanner
import com.example.ui.components.MessageType
import com.example.util.ErrorSanitizer
import com.example.util.ImmersiveSystemBarsEffect
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Helper functions for unified currency formatting and level system calculations
fun formatCurrency(amount: Double): String {
    return String.format(java.util.Locale.US, "$ %.2f", amount)
}

fun formatLastSeen(lastSeenStr: String?): String {
    if (lastSeenStr.isNullOrEmpty()) return "غير متصل"
    try {
        var normalized = lastSeenStr.replace("Z", "")
        if (normalized.contains("+")) {
            normalized = normalized.substringBefore("+")
        }
        if (normalized.contains(".")) {
            normalized = normalized.substringBefore(".")
        }
        normalized = normalized.trim()
        
        // Normalize any 'T' separators to space to support all standard SQL/ISO timestamp representations
        normalized = normalized.replace("T", " ")
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(normalized) ?: return "غير متصل"
        val timeMs = date.time

        val diffMs = System.currentTimeMillis() - timeMs
        if (diffMs < 0) return "آخر ظهور منذ ثوانٍ"

        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        val diffDay = diffHour / 24

        return when {
            diffMin < 1 -> "آخر ظهور منذ ثوانٍ"
            diffMin < 2 -> "آخر ظهور منذ دقيقة"
            diffMin < 3 -> "آخر ظهور منذ دقيقتين"
            diffMin < 11 -> "آخر ظهور منذ $diffMin دقائق"
            diffMin < 60 -> "آخر ظهور منذ $diffMin دقيقة"
            diffHour < 2 -> "آخر ظهور منذ ساعة"
            diffHour < 3 -> "آخر ظهور منذ ساعتين"
            diffHour < 11 -> "آخر ظهور منذ $diffHour ساعات"
            diffHour < 24 -> "آخر ظهور منذ $diffHour ساعة"
            diffDay < 2 -> "آخر ظهور أمس"
            diffDay < 3 -> "آخر ظهور منذ يومين"
            diffDay < 7 -> "آخر ظهور منذ $diffDay أيام"
            diffDay < 14 -> "آخر ظهور منذ أسبوع"
            diffDay < 21 -> "آخر ظهور منذ أسبوعين"
            diffDay < 30 -> "آخر ظهور منذ 3 أسابيع"
            else -> "آخر ظهور منذ شهر"
        }
    } catch (e: Exception) {
        return "غير متصل"
    }
}

fun getNextLevelPoints(points: Int): Int {
    return when {
        points < 100 -> 100
        points < 250 -> 250
        points < 450 -> 450
        points < 700 -> 700
        points < 1000 -> 1000
        points < 1400 -> 1400
        points < 1900 -> 1900
        points < 2500 -> 2500
        points < 3200 -> 3200
        else -> {
            val levelBase = 3200
            val levelOffset = (points - levelBase) / 1000
            levelBase + (levelOffset + 1) * 1000
        }
    }
}

fun getLevelProgress(points: Int): Float {
    val nextLevelPoints = getNextLevelPoints(points)
    val basePoints = when {
        points < 100 -> 0
        points < 250 -> 100
        points < 450 -> 250
        points < 700 -> 450
        points < 1000 -> 700
        points < 1400 -> 1000
        points < 1900 -> 1400
        points < 2500 -> 1900
        points < 3200 -> 2500
        else -> {
            val levelBase = 3200
            val levelOffset = (points - levelBase) / 1000
            levelBase + levelOffset * 1000
        }
    }
    val range = nextLevelPoints - basePoints
    return if (range > 0) {
        ((points - basePoints).toFloat() / range).coerceIn(0f, 1f)
    } else {
        0f
    }
}

// ==========================================
// MAIN APP COMPOSABLE NAVIGATOR
// ==========================================
@Composable
fun AppContent(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val isMainLoggedScreen = activeProfile != null && (
        currentScreen is Screen.Home ||
        currentScreen is Screen.Challenges ||
        currentScreen is Screen.Wallet ||
        currentScreen is Screen.Notifications ||
        currentScreen is Screen.Profile ||
        currentScreen is Screen.EditProfile ||
        currentScreen is Screen.PrivacySecurity ||
        currentScreen is Screen.Statistics ||
        currentScreen is Screen.Friends ||
        currentScreen is Screen.SearchUsers ||
        currentScreen is Screen.FriendRequests ||
        currentScreen is Screen.ViewUserProfile
    )

    // Dynamic Sticky Immersive System Bars management across all screens
    ImmersiveSystemBarsEffect(currentScreen = currentScreen)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CosmicBackground, Color(0xFF03050B))
                )
            )
    ) {
        if (isMainLoggedScreen) {
            Scaffold(
                topBar = {
                    AppTopBar(viewModel = viewModel, currentScreen = currentScreen)
                },
                bottomBar = {
                    AppBottomNavigation(viewModel = viewModel, currentScreen = currentScreen)
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Crossfade(targetState = currentScreen, label = "LoggedScreenTransition") { screen ->
                        when (screen) {
                            is Screen.Home -> HomeScreen(viewModel)
                            is Screen.Challenges -> ChallengesScreen(viewModel)
                            is Screen.Wallet -> WalletScreen(viewModel)
                            is Screen.Notifications -> NotificationsScreen(viewModel)
                            is Screen.Profile -> ProfileScreen(viewModel)
                            is Screen.EditProfile -> EditProfileScreen(viewModel)
                            is Screen.PrivacySecurity -> PrivacySecurityScreen(viewModel)
                            is Screen.Statistics -> StatisticsScreen(viewModel)
                            is Screen.Friends -> FriendsScreen(viewModel)
                            is Screen.SearchUsers -> SearchScreen(viewModel)
                            is Screen.FriendRequests -> FriendRequestsScreen(viewModel)
                            is Screen.ViewUserProfile -> ViewUserProfileScreen(viewModel)
                            else -> {}
                        }
                    }
                }
            }
        } else {
            // Main Screen Router for non-logged or full-screen gaming
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    is Screen.Login -> LoginScreen(viewModel)
                    is Screen.Home -> HomeScreen(viewModel)
                    is Screen.Challenges -> ChallengesScreen(viewModel)
                    is Screen.Matchmaking -> MatchmakingScreen(viewModel)
                    is Screen.Game -> GameScreen(viewModel)
                    is Screen.Wallet -> WalletScreen(viewModel)
                    is Screen.Notifications -> NotificationsScreen(viewModel)
                    is Screen.AdminPanel -> AdminPanelScreen(viewModel)
                    is Screen.Lobby -> LobbyScreen(viewModel)
                    else -> {}
                }
            }
        }

        // Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = CosmicCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري الاتصال بقاعدة البيانات...",
                            color = TextLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Floating Dismissible / Swipeable Error Banner
        if (errorMessage != null) {
            FloatingAppMessageBanner(
                message = errorMessage!!,
                type = MessageType.ERROR,
                onDismiss = { viewModel.clearErrorMessage() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isMainLoggedScreen) 76.dp else 0.dp)
            )
        }

        // Floating Dismissible / Swipeable Success Banner
        if (successMessage != null) {
            FloatingAppMessageBanner(
                message = successMessage!!,
                type = MessageType.SUCCESS,
                onDismiss = { viewModel.clearSuccessMessage() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isMainLoggedScreen) 76.dp else 0.dp)
            )
        }
    }
}

// ==========================================
// SHARED TOP BAR AND BOTTOM NAVIGATION BAR
// ==========================================

@Composable
fun AppTopBar(viewModel: GameViewModel, currentScreen: Screen) {
    var showSettingsMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Title or Back button
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentScreen is Screen.EditProfile || currentScreen is Screen.Statistics || currentScreen is Screen.Notifications) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "عودة",
                        tint = TextLight
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (currentScreen) {
                        is Screen.EditProfile -> "تعديل الملف الشخصي"
                        is Screen.Statistics -> "الإحصائيات الحقيقية"
                        is Screen.Notifications -> "الإشعارات"
                        else -> ""
                    },
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // Main Logo / Title
                Text(
                    text = "XO MAX",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(CosmicGold, CosmicCyan)
                        ),
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = CosmicCyan.copy(alpha = 0.4f),
                            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                            blurRadius = 4f
                        )
                    ),
                    modifier = Modifier.padding(start = 4.dp),
                    letterSpacing = 1.sp
                )
            }
        }

        // Right side: Notification Bell & Settings Icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentScreen != Screen.Login) {
                val notifications by viewModel.notifications.collectAsStateWithLifecycle()
                val unreadCount = notifications.count { !it.isRead }

                IconButton(onClick = { viewModel.navigateTo(Screen.Notifications) }) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "الإشعارات",
                            tint = if (unreadCount > 0) CosmicGold else TextLight,
                            modifier = Modifier.size(24.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(CosmicOrange)
                                    .offset(x = 4.dp, y = (-4).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = TextLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            Box {
                IconButton(onClick = { showSettingsMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSettingsMenu,
                    onDismissRequest = { showSettingsMenu = false },
                    modifier = Modifier.background(CosmicCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("تعديل الملف الشخصي", color = TextLight, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showSettingsMenu = false
                            viewModel.navigateTo(Screen.EditProfile)
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CosmicCyan) }
                    )
                    DropdownMenuItem(
                        text = { Text("المحفظة الإلكترونية", color = TextLight, fontWeight = FontWeight.Medium) },
                        onClick = {
                            showSettingsMenu = false
                            viewModel.navigateTo(Screen.Wallet)
                        },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = CosmicGold) }
                    )
                    HorizontalDivider(color = CosmicSlate)
                    DropdownMenuItem(
                        text = { Text("تسجيل الخروج", color = CosmicOrange, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showSettingsMenu = false
                            viewModel.performLogout()
                        },
                        leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = CosmicOrange) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigation(viewModel: GameViewModel, currentScreen: Screen) {
    NavigationBar(
        containerColor = CosmicCard.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val navItems = listOf(
            Triple(Screen.Home, Icons.Default.Home, "الرئيسية"),
            Triple(Screen.Challenges, Icons.Default.List, "التحديات"),
            Triple(Screen.Wallet, Icons.Default.AccountBalanceWallet, "المحفظة"),
            Triple(Screen.Friends, Icons.Default.Group, "الأصدقاء"),
            Triple(Screen.Profile, Icons.Default.Person, "حسابي")
        )

        navItems.forEach { (screen, icon, label) ->
            val isSelected = when (screen) {
                Screen.Home -> currentScreen is Screen.Home
                Screen.Challenges -> currentScreen is Screen.Challenges
                Screen.Wallet -> currentScreen is Screen.Wallet
                Screen.Friends -> currentScreen is Screen.Friends || currentScreen is Screen.SearchUsers || currentScreen is Screen.FriendRequests || currentScreen is Screen.ViewUserProfile
                Screen.Profile -> currentScreen is Screen.Profile
                else -> false
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { viewModel.navigateTo(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) CosmicCyan else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (isSelected) CosmicCyan else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CosmicSlate
                )
            )
        }
    }
}

// ==========================================
// IMAGE CROPPING HELPERS
// ==========================================

fun cropProfileBitmap(
    source: Bitmap,
    containerWidth: Float,
    containerHeight: Float,
    cropSize: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Bitmap {
    val cropped = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(cropped)
    
    val srcW = source.width.toFloat()
    val srcH = source.height.toFloat()
    
    val baseScale = Math.min(containerWidth / srcW, containerHeight / srcH)
    val startX = (containerWidth - srcW * baseScale) / 2f
    val startY = (containerHeight - srcH * baseScale) / 2f
    
    val mapToContainer = android.graphics.Matrix()
    mapToContainer.postScale(baseScale, baseScale)
    mapToContainer.postTranslate(startX, startY)
    
    val cx = containerWidth / 2f
    val cy = containerHeight / 2f
    mapToContainer.postScale(scale, scale, cx, cy)
    mapToContainer.postTranslate(offsetX, offsetY)
    
    val cropLeft = (containerWidth - cropSize) / 2f
    val cropTop = (containerHeight - cropSize) / 2f
    
    val containerToCropped = android.graphics.Matrix()
    containerToCropped.postTranslate(-cropLeft, -cropTop)
    val scaleToTarget = 256f / cropSize
    containerToCropped.postScale(scaleToTarget, scaleToTarget)
    
    val finalMatrix = android.graphics.Matrix()
    finalMatrix.set(mapToContainer)
    finalMatrix.postConcat(containerToCropped)
    
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, finalMatrix, paint)
    
    return cropped
}

fun Bitmap.toBase64String(): String {
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    val byteArray = outputStream.toByteArray()
    return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

// ==========================================
// 1. LOGIN & REGISTER SCREEN
// ==========================================
@Composable
fun LoginScreen(viewModel: GameViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isRegPasswordVisible by remember { mutableStateOf(false) }

    // Dialog state variables
    var showGoogleChooser by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var customGoogleEmail by remember { mutableStateOf("") }
    var customGoogleName by remember { mutableStateOf("") }
    var isAddingCustomGoogleAccount by remember { mutableStateOf(false) }

    val usernameOrEmail by viewModel.loginUsernameOrEmail.collectAsStateWithLifecycle()
    val password by viewModel.loginPassword.collectAsStateWithLifecycle()

    val regUsername by viewModel.registerUsername.collectAsStateWithLifecycle()
    val regEmail by viewModel.registerEmail.collectAsStateWithLifecycle()
    val regPassword by viewModel.registerPassword.collectAsStateWithLifecycle()

    // 1. Google Account Chooser Dialog
    if (showGoogleChooser) {
        AlertDialog(
            onDismissRequest = { 
                showGoogleChooser = false 
                isAddingCustomGoogleAccount = false
                customGoogleEmail = ""
                customGoogleName = ""
            },
            title = {
                Text(
                    "اختر حساباً للمتابعة إلى XO MAX",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isAddingCustomGoogleAccount) {
                        Text(
                            "للمتابعة، اختر أحد حساباتك المسجلة أو أضف حساباً جديداً:",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Default Account: Naseer Hamza
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSlate),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.performGoogleAuth("naseerhamza432@gmail.com", "Naseer Hamza")
                                    showGoogleChooser = false
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(CosmicCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("N", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Naseer Hamza", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("naseerhamza432@gmail.com", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }

                        // Option: Add another account
                        OutlinedButton(
                            onClick = { isAddingCustomGoogleAccount = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CosmicCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استخدام حساب جوجل آخر", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "أدخل بيانات حساب جوجل الجديد:",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customGoogleName,
                            onValueChange = { customGoogleName = it },
                            label = { Text("الاسم الكامل بالحساب", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = customGoogleEmail,
                            onValueChange = { customGoogleEmail = it },
                            label = { Text("البريد الإلكتروني (Gmail)", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (customGoogleEmail.trim().isNotEmpty() && customGoogleName.trim().isNotEmpty()) {
                                        viewModel.performGoogleAuth(customGoogleEmail.trim(), customGoogleName.trim())
                                        showGoogleChooser = false
                                        isAddingCustomGoogleAccount = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                enabled = customGoogleEmail.contains("@") && customGoogleName.length >= 3
                            ) {
                                Text("متابعة", color = CosmicBackground, fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = { isAddingCustomGoogleAccount = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("رجوع", color = TextMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (!isAddingCustomGoogleAccount) {
                    TextButton(onClick = { showGoogleChooser = false }) {
                        Text("إلغاء", color = TextMuted)
                    }
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 2. Technical Support Dialog
    if (showSupportDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = {
                Text("الدعم الفني ومساعدة اللاعبين 📞", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "يسعدنا دائماً تواصلك معنا لحل أي مشاكل فنية، استعادة أو إنشاء الحسابات، أو الاستفسار عن المعاملات المالية.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicSlate)
                            .padding(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("البريد الإلكتروني المباشر للدعم:", color = CosmicCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "h7amzanasser@gmail.com",
                                color = TextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("h7amzanasser@gmail.com"))
                            Toast.makeText(context, "تم نسخ البريد الإلكتروني بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CosmicBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نسخ البريد الإلكتروني", color = CosmicBackground, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:h7amzanasser@gmail.com")
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "طلب دعم فني - تطبيق XO MAX")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم يتم العثور على تطبيق بريد إلكتروني.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSlate),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TextLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال رسالة بريد مباشر", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("إغلاق", color = TextMuted)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Technical Support Top Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { showSupportDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = CosmicCyan)
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = CosmicCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("الدعم الفني والشكاوى 📞", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // XO MAX Premium Logo
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CosmicCard)
                .border(2.dp, Brush.linearGradient(listOf(CosmicGold, CosmicCyan)), RoundedCornerShape(24.dp))
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = com.example.R.drawable.xo_max_logo),
                contentDescription = "XO MAX Championship Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "XO MAX",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(CosmicGold, CosmicCyan)
                ),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = CosmicCyan.copy(alpha = 0.5f),
                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                    blurRadius = 8f
                )
            ),
            letterSpacing = 3.sp
        )

        Text(
            text = "بطولة التحديات الكبرى والجوائز المالية 🏆",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Frosted Glass Form Container
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = CosmicCyan)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tab Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSlate, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isRegisterMode) CosmicCyan else Color.Transparent)
                            .clickable { isRegisterMode = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "تسجيل الدخول",
                            color = if (!isRegisterMode) CosmicBackground else TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRegisterMode) CosmicCyan else Color.Transparent)
                            .clickable { isRegisterMode = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "إنشاء حساب",
                            color = if (isRegisterMode) CosmicBackground else TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!isRegisterMode) {
                    // LOGIN FIELDS
                    OutlinedTextField(
                        value = usernameOrEmail,
                        onValueChange = { viewModel.loginUsernameOrEmail.value = it },
                        label = { Text("اسم المستخدم أو البريد الإلكتروني", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CosmicCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CosmicSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.loginPassword.value = it },
                        label = { Text("كلمة المرور", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CosmicCyan) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                    tint = CosmicCyan
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CosmicSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.performLogin() },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("دخول للعبة الآمنة", color = CosmicBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign in with Google Button
                    Button(
                        onClick = { showGoogleChooser = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G ",
                                color = Color(0xFF4285F4),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تسجيل الدخول بحساب Google",
                                color = Color(0xFF3C4043),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Password reminder warning card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicOrange.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicOrange.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚠️ تنبيه هام: إذا نسيت كلمة المرور الخاصة بك، فلن تتمكن من تغييرها أو فتح حسابك بنفسك. لاستعادة الحساب أو فتحه، يرجى التواصل مباشرة مع الدعم الفني للشركة.",
                                color = CosmicOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            
                            TextButton(
                                onClick = { showSupportDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = CosmicCyan)
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تواصل مع الدعم الفني الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                } else {
                    // REGISTER FIELDS
                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { viewModel.registerUsername.value = it },
                        label = { Text("اسم مستخدم جديد فريد", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CosmicCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CosmicSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { viewModel.registerEmail.value = it },
                        label = { Text("البريد الإلكتروني الحقيقي", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CosmicCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CosmicSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { viewModel.registerPassword.value = it },
                        label = { Text("كلمة المرور القوية", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CosmicCyan) },
                        trailingIcon = {
                            IconButton(onClick = { isRegPasswordVisible = !isRegPasswordVisible }) {
                                Icon(
                                    imageVector = if (isRegPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isRegPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                    tint = CosmicCyan
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CosmicSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.performRegister() },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إنشاء حساب جديد", color = CosmicBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign in with Google Button (Register Mode)
                    Button(
                        onClick = { showGoogleChooser = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G ",
                                color = Color(0xFF4285F4),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "التسجيل السريع بحساب Google",
                                color = Color(0xFF3C4043),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { showSupportDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = CosmicCyan)
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("للمساعدة والدعم الفني 📞", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. DASHBOARD HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(viewModel: GameViewModel) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()

    if (profile == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Gaming Welcome Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(CosmicCyan.copy(alpha = 0.15f), CosmicGold.copy(alpha = 0.15f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "مرحباً بك في بطولة XO MAX الماليّة الكبرى 🏆",
                        color = CosmicGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "تحدّ وتغلّب على منافسيك واربح جوائز حقيقية ومباشرة!",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Profile Card / Wallet Dashboard Header
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = CosmicCyan)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image loaded via Coil - Clickable to open Statistics
                    AsyncImage(
                        model = profile!!.avatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${profile!!.username}",
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, CosmicCyan, CircleShape)
                            .clickable { viewModel.navigateTo(Screen.Statistics) },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(Screen.Statistics) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var dashboardClickCount by remember { mutableStateOf(0) }
                            var dashboardLastClickTime by remember { mutableStateOf(0L) }
                            Text(
                                text = profile!!.username,
                                color = TextLight,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    if (profile!!.username == "hamzanasser") {
                                        val now = System.currentTimeMillis()
                                        if (now - dashboardLastClickTime < 1000) {
                                            dashboardClickCount++
                                        } else {
                                            dashboardClickCount = 1
                                        }
                                        dashboardLastClickTime = now
                                        if (dashboardClickCount >= 3) {
                                            dashboardClickCount = 0
                                            viewModel.toggleComputerBotMode()
                                        }
                                    } else {
                                        viewModel.navigateTo(Screen.Statistics)
                                    }
                                }
                            )
                            if (profile!!.accountNumber != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ID: #${profile!!.accountNumber}",
                                    color = CosmicCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(CosmicSlate, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المستوى ${profile!!.level}",
                                color = CosmicGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${profile!!.points} نقطة)",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        // Level Progress Bar
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = getLevelProgress(profile!!.points)
                        LinearProgressIndicator(
                            progress = { progress },
                            color = CosmicCyan,
                            trackColor = CosmicSlate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                HorizontalDivider(color = CosmicSlate, modifier = Modifier.padding(vertical = 16.dp))

                // Wallet Quick Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الرصيد المتاح", color = TextMuted, fontSize = 12.sp)
                        Text(
                            formatCurrency(profile!!.balance),
                            color = TextLight,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.navigateTo(Screen.Wallet) },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicSlate),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = CosmicCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("المحفظة", color = TextLight, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Menu Options
        Text(
            "أوضاع اللعب والمنافسة حقيقية",
            color = TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Matchmaking 1v1
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo(Screen.Matchmaking) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(CosmicCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CosmicCyan, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("1 ضد 1 حقيقي", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("مطابقة مستويات حية", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }

            // Challenges board
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo(Screen.Challenges) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(CosmicGold.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = CosmicGold, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("لوحة التحديات", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("تحديات اللاعبين النشطة", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active match button if matchmaking is running
        val isMatching by viewModel.isMatchmaking.collectAsStateWithLifecycle()
        if (isMatching) {
            Button(
                onClick = { viewModel.navigateTo(Screen.Matchmaking) },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("البحث عن مباراة نشط الآن - انقر للمتابعة", color = TextLight, fontWeight = FontWeight.Bold)
            }
        }

        // Admin Access Toggle (Strictly for taj)
        if (profile!!.username == "taj") {
            Button(
                onClick = { viewModel.navigateTo(Screen.AdminPanel) },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = CosmicBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("لوحة تحكم المشرف الرئيسي taj", color = CosmicBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 3. CHALLENGES SCREEN
// ==========================================
@Composable
fun ChallengesScreen(viewModel: GameViewModel) {
    val challengesList by viewModel.challenges.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var challengeBetInput by remember { mutableStateOf("5") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = TextLight)
            }

            Text("تحديات اللاعبين النشطة", color = TextLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            IconButton(onClick = { viewModel.refreshChallenges() }) {
                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = CosmicCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create custom Challenge Button
        Button(
            onClick = { showCreateDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = CosmicBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إنشاء تحدي مالي جديد مخصص", color = CosmicBackground, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (challengesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد تحديات مالية مفتوحة حالياً.", color = TextMuted, fontSize = 14.sp)
                    Text("كن أول من ينشئ تحدياً الآن!", color = CosmicCyan, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(challengesList) { challenge ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            AsyncImage(
                                model = challenge.creatorAvatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${challenge.creatorUsername}",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, CosmicGold, CircleShape)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(challenge.creatorUsername, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("مستوى ${challenge.creatorLevel}", color = CosmicGold, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(GlowGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("متصل", color = GlowGreen, fontSize = 11.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (challenge.betAmount <= 0.0) "لعب مجاني 🌟" else formatCurrency(challenge.betAmount),
                                    color = if (challenge.betAmount <= 0.0) GlowGreen else CosmicCyan,
                                    fontSize = if (challenge.betAmount <= 0.0) 14.sp else 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                if (challenge.creatorId == activeProfile?.id) {
                                    Button(
                                        onClick = { viewModel.cancelMyOpenChallengeDirectly(challenge.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("إلغاء", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.acceptChallengeDirectly(challenge) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("مبارزة", color = CosmicBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Challenge Dialog
    if (showCreateDialog) {
        var isFreeChallenge by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (isFreeChallenge) "تحدي مجاني جديد 🌟" else "تحدي مالي جديد 💰", color = TextLight, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isFreeChallenge,
                            onCheckedChange = { isFreeChallenge = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CosmicCyan,
                                uncheckedColor = CosmicSlate,
                                checkmarkColor = CosmicBackground
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء تحدي مجاني (للتدريب والترقية)", color = TextLight, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isFreeChallenge) {
                        Text("أدخل قيمة دخول التحدي بالدولار الأمريكي:", color = TextMuted, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = challengeBetInput,
                            onValueChange = { challengeBetInput = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            "في وضع التحدي المجاني، لن يتم رهن أي مبالغ مالية. يمكن لأي لاعب قبول التحدي مجاناً وسيحصل الفائز على نقاط وخبرة للمستوى فقط.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = if (isFreeChallenge) 0.0 else (challengeBetInput.toDoubleOrNull() ?: 5.0)
                        viewModel.createCustomChallenge(amountVal)
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan)
                ) {
                    Text("تأكيد ونشر", color = CosmicBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("إلغاء", color = CosmicOrange)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ==========================================
// 4. MATCHMAKING / RADAR PULSING SCREEN
// ==========================================
@Composable
fun MatchmakingScreen(viewModel: GameViewModel) {
    val isMatching by viewModel.isMatchmaking.collectAsStateWithLifecycle()
    val status by viewModel.matchmakingStatus.collectAsStateWithLifecycle()
    val betAmount by viewModel.matchmakingBetAmount.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Betting, 1: Free

    // Radar Waves pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarAnimation")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarScale1"
    )
    val opacity1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarOpacity1"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isMatching) {
            // MATCHMAKING CONFIGURATION
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Home) },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = TextLight)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TAB SWITCHER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicCard, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedTab == 0) CosmicCyan else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "اللعب بالمراهنات 💰",
                        color = if (selectedTab == 0) CosmicBackground else TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selectedTab == 1) CosmicCyan else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "اللعب المجاني 🌟",
                        color = if (selectedTab == 1) CosmicBackground else TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (selectedTab == 0) {
                Text("البحث الذكي عن منافس مالي", color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "يقوم النظام بالبحث التلقائي ومطابقة مستواك فقط مع الخصوم الذين يمتلكون نفس رهانك المالي تماماً لضمان النزاهة المطلقة.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Value selector card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("قيمة رهان المباراة (USD)", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = betAmount,
                            onValueChange = { viewModel.setMatchmakingBetAmount(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicCyan
                            ),
                            modifier = Modifier
                                .width(180.dp)
                                .testTag("bet_amount_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("أقل قيمة للمباراة هي 1 دولار أمريكي.", color = TextMuted, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.startMatchmaking(isFree = false) },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("search_match_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("ابدأ البحث المالي المطور", color = CosmicBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("وضع اللعب المجاني", color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "استمتع باللعب دون أي تكلفة أو مراهنات مالية! ستحافظ على نقاط الترقية وستتطور في المستوى والمهارة مجاناً.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "مجاني",
                            tint = CosmicCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("ميزات الوضع المجاني", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "✓ لا توجد أي مراهنات مادية مخصومة\n✓ حساب نقاط الفوز والخسارة وتطوير المستوى\n✓ اللعب الفوري ضد مستخدمين حقيقيين\n✓ اللعب والتدريب ضد الكمبيوتر الذكي (متوسط)",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.startMatchmaking(isFree = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("لاعب حقيقي مجاناً", color = CosmicBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.startComputerMatch() },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("العب ضد الكمبيوتر 🤖", color = CosmicBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

        } else {
            // RADAR PULSING SCREEN
            Spacer(modifier = Modifier.weight(1f))

            // Cosmic Pulse Wave
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawCircle(
                                color = CosmicCyan,
                                radius = (size.minDimension / 2) * scale1,
                                alpha = opacity1,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                )

                // Inner Solid Pulse
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(24.dp, CircleShape, ambientColor = CosmicCyan)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(CosmicCyan, CosmicSlate)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = CosmicGold, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "جاري مطابقة مباراتك...",
                color = TextLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = status,
                color = CosmicCyan,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.cancelMatchmaking() },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("إلغاء البحث والرجوع", color = TextLight, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// 5. XO GAMEPLAY INTERACTIVE SCREEN
// ==========================================
private fun getWinningCombination(board: String): List<Int>? {
    val wins = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // columns
        listOf(0, 4, 8), listOf(2, 4, 6)                  // diagonals
    )
    for (win in wins) {
        val c = board[win[0]]
        if (c != '_' && board[win[1]] == c && board[win[2]] == c) {
            return win
        }
    }
    return null
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val match by viewModel.activeMatch.collectAsStateWithLifecycle()
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()

    val winEffect by viewModel.gameWinEffect.collectAsStateWithLifecycle()
    val loseEffect by viewModel.gameLoseEffect.collectAsStateWithLifecycle()
    val drawEffect by viewModel.gameDrawEffect.collectAsStateWithLifecycle()

    if (match == null || profile == null) return

    val board = match!!.board
    val isMyTurn = match!!.turnPlayerId == profile!!.id
    val myChar = if (profile!!.id == match!!.player1Id) 'X' else 'O'

    // Local state for selecting a piece to move (0-8 index)
    var selectedCellIndex by remember { mutableStateOf<Int?>(null) }
    var dragSourceIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var isResultDialogDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(match!!.id) {
        isResultDialogDismissed = false
    }

    // Reset selection when it's no longer our turn or when match ends
    LaunchedEffect(match!!.turnPlayerId, match!!.status) {
        selectedCellIndex = null
        dragSourceIndex = null
        isDragging = false
    }

    // Collect players profiles
    val p1Profile by viewModel.player1Profile.collectAsStateWithLifecycle()
    val p2Profile by viewModel.player2Profile.collectAsStateWithLifecycle()

    // Lose Shake animation
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(loseEffect) {
        if (loseEffect) {
            repeat(6) {
                shakeOffset.animateTo(15f, tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-15f, tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // MATCH RESULTS POPUP (Win/Lose/Draw)
        if (!isResultDialogDismissed && (winEffect || loseEffect || drawEffect || match!!.status == "completed" || match!!.status == "draw")) {
            // Show confetti effects if the match ended with a win
            if (match!!.status == "completed") {
                ConfettiParticles()
            }

            // Custom Result Pop-up with Glow & Sound
            LaunchedEffect(Unit) {
                try {
                    val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                    toneGen.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 180)
                } catch (e: Exception) {
                    // Fail-safe
                }
            }

            Dialog(onDismissRequest = { 
                isResultDialogDismissed = true
            }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicCard, RoundedCornerShape(24.dp))
                        .border(2.dp, if (match!!.status == "completed") CosmicGold else CosmicSlate, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ResultGlow")
                    val glowScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.12f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "GlowScale"
                    )

                    val isCompleted = match!!.status == "completed"
                    val winnerName = if (isCompleted) {
                        match!!.winnerUsername ?: (if (match!!.winnerId == match!!.player1Id) p1Profile?.username else p2Profile?.username) ?: "الفائز"
                    } else {
                        "التعادل"
                    }

                    val isWeWinner = isCompleted && match!!.winnerId == profile?.id
                    val isWeLoser = isCompleted && match!!.winnerId != profile?.id
                    val isDraw = !isCompleted

                    val rewardAmount = if (isWeWinner) {
                        match!!.betAmount * 2 * 0.70
                    } else {
                        match!!.betAmount
                    }

                    var animatedReward by remember { mutableStateOf(0.0) }
                    LaunchedEffect(rewardAmount) {
                        val steps = 25
                        var toneGen: android.media.ToneGenerator? = null
                        if (isWeWinner || isDraw) {
                            try {
                                toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 75)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                        for (i in 1..steps) {
                            delay(50) // Satisfying ticking delay
                            animatedReward = (rewardAmount / steps) * i
                            if (isWeWinner || isDraw) {
                                try {
                                    toneGen?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 40)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                        }
                        animatedReward = rewardAmount
                        if (isWeWinner || isDraw) {
                            try {
                                toneGen?.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 140)
                                delay(160)
                                toneGen?.release()
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer(scaleX = glowScale, scaleY = glowScale)
                                .size(84.dp)
                                .background(if (isCompleted) CosmicGold.copy(alpha = 0.15f) else CosmicSlate.copy(alpha = 0.2f), CircleShape)
                                .border(1.5.dp, if (isCompleted) CosmicGold else CosmicSlate, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isCompleted) "🏆" else "🤝", fontSize = 44.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (isCompleted) "🎉 نهاية المباراة!" else "مباراة متعادلة 🤝",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isCompleted) {
                            Text(
                                text = "الفائز هو: $winnerName 👑",
                                color = CosmicGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "انتهت الجولة بالتعادل الإيجابي",
                                color = TextMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isCompleted) "تمت تسوية أرباح الجولة مباشرة في المحفظة!" else "تمت إعادة مبلغ الرهان بالكامل لمحفظتك.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .background(if (isWeWinner) GlowGreen.copy(alpha = 0.12f) else CosmicOrange.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, if (isWeWinner) GlowGreen.copy(alpha = 0.30f) else CosmicOrange.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = if (isWeWinner) "+${formatCurrency(animatedReward)} USD" else if (isWeLoser) "-${formatCurrency(animatedReward)} USD" else "${formatCurrency(animatedReward)} USD",
                                color = if (isWeWinner) GlowGreen else CosmicOrange,
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    isResultDialogDismissed = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicSlate),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("مشاهدة اللوحة", color = TextLight, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    viewModel.resetGameEffects()
                                    viewModel.navigateTo(Screen.Home)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isCompleted) CosmicGold else CosmicCyan),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("الرئيسية", color = CosmicSlate, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Game Top header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "انسحاب", tint = TextLight)
                }

                Text(
                    text = "🏆 جائزة الفوز: ${formatCurrency(match!!.betAmount * 2 * 0.70)}",
                    color = CosmicGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .background(CosmicSlate, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (match!!.status == "playing") "مباراة جارية" else "منتهية",
                        color = if (match!!.status == "playing") CosmicCyan else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Professional Players Profile Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicCard, RoundedCornerShape(16.dp))
                    .border(
                        1.5.dp,
                        if (isMyTurn) CosmicCyan.copy(alpha = 0.8f) else CosmicSlate,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1 (Creator - X)
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val p1 = p1Profile
                    AsyncImage(
                        model = p1?.avatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${p1?.username ?: "X"}",
                        contentDescription = "P1 Avatar",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(2.dp, CosmicCyan, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = p1?.username ?: "تحميل...",
                        color = if (profile!!.id == match!!.player1Id) CosmicCyan else TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (p1?.username != "h.7") {
                        Text(
                            text = "المستوى: ${p1?.level ?: 1}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(28.dp)
                            .background(CosmicCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("X", color = CosmicCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    if (match!!.turnPlayerId == match!!.player1Id && match!!.status == "playing") {
                        Text("دور X", color = CosmicCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }

                // VS Middle Accent
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CosmicSlate, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VS", color = CosmicGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Player 2 (Joiner - O)
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val p2 = p2Profile
                    AsyncImage(
                        model = p2?.avatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${p2?.username ?: "O"}",
                        contentDescription = "P2 Avatar",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(2.dp, CosmicOrange, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = p2?.username ?: "تحميل...",
                        color = if (profile!!.id == match!!.player2Id) CosmicOrange else TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (p2?.username != "h.7") {
                        Text(
                            text = "المستوى: ${p2?.level ?: 1}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(28.dp)
                            .background(CosmicOrange.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("O", color = CosmicOrange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    if (match!!.turnPlayerId == match!!.player2Id && match!!.status == "playing") {
                        Text("دور O", color = CosmicOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val density = LocalDensity.current
            val gridSizePx = with(density) { 288.dp.toPx() }
            val cellSizePx = gridSizePx / 3f

            // Game Board layout
            Box(
                modifier = Modifier
                    .offset(x = shakeOffset.value.dp)
                    .size(320.dp)
                    .background(CosmicCard, RoundedCornerShape(20.dp))
                    .padding(16.dp)
                    .pointerInput(isMyTurn, board, myChar, selectedCellIndex, isDragging) {
                        if (!isMyTurn || match!!.status != "playing" || profile!!.username == "h.7") return@pointerInput
                        val pieceCount = board.count { it == myChar }
                        
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val startOffset = down.position
                                val col = (startOffset.x / cellSizePx).toInt().coerceIn(0, 2)
                                val row = (startOffset.y / cellSizePx).toInt().coerceIn(0, 2)
                                val index = row * 3 + col
                                val cellChar = board[index]
                                
                                var isDrag = false
                                var currentPosition = startOffset
                                
                                dragSourceIndex = null
                                dragCurrentOffset = startOffset
                                
                                val dragResult = drag(down.id) { change ->
                                    change.consume()
                                    currentPosition = change.position
                                    dragCurrentOffset = currentPosition
                                    
                                    if (!isDrag && (currentPosition - startOffset).getDistance() > 15f) {
                                        if (pieceCount == 3 && cellChar == myChar) {
                                            isDrag = true
                                            dragSourceIndex = index
                                            selectedCellIndex = index
                                            isDragging = true
                                        }
                                    }
                                }
                                
                                if (isDrag) {
                                    val endX = currentPosition.x
                                    val endY = currentPosition.y
                                    val endCol = (endX / cellSizePx).toInt().coerceIn(0, 2)
                                    val endRow = (endY / cellSizePx).toInt().coerceIn(0, 2)
                                    val targetIndex = endRow * 3 + endCol
                                    
                                    val src = dragSourceIndex
                                    if (src != null && targetIndex != src && board[targetIndex] == '_') {
                                        viewModel.makeMove(src * 10 + targetIndex)
                                        selectedCellIndex = null
                                    }
                                    dragSourceIndex = null
                                    isDragging = false
                                } else {
                                    // Clean click/tap
                                    if (pieceCount < 3) {
                                        if (cellChar == '_') {
                                            viewModel.makeMove(index)
                                        }
                                    } else {
                                        if (cellChar == myChar) {
                                            selectedCellIndex = if (selectedCellIndex == index) null else index
                                        } else if (cellChar == '_') {
                                            val src = selectedCellIndex
                                            if (src != null && board[src] == myChar) {
                                                viewModel.makeMove(src * 10 + index)
                                                selectedCellIndex = null
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Grid Draw Behind lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 4.dp.toPx()
                    val lineC = CosmicSlate

                    // Draw vertical lines
                    drawLine(lineC, Offset(size.width / 3, 0f), Offset(size.width / 3, size.height), strokeW)
                    drawLine(lineC, Offset(size.width * 2 / 3, 0f), Offset(size.width * 2 / 3, size.height), strokeW)

                    // Draw horizontal lines
                    drawLine(lineC, Offset(0f, size.height / 3), Offset(size.width, size.height / 3), strokeW)
                    drawLine(lineC, Offset(0f, size.height * 2 / 3), Offset(size.width, size.height * 2 / 3), strokeW)

                    // Draw glowing winning line if the game has ended with a win
                    val winningCombo = getWinningCombination(board)
                    if (winningCombo != null) {
                        val firstIdx = winningCombo[0]
                        val lastIdx = winningCombo[2]
                        
                        val r1 = firstIdx / 3
                        val c1 = firstIdx % 3
                        val r2 = lastIdx / 3
                        val c2 = lastIdx % 3
                        
                        val startX = (c1 + 0.5f) * (size.width / 3f)
                        val startY = (r1 + 0.5f) * (size.height / 3f)
                        val endX = (c2 + 0.5f) * (size.width / 3f)
                        val endY = (r2 + 0.5f) * (size.height / 3f)
                        
                        val winnerChar = board[firstIdx]
                        val winLineColor = if (winnerChar == 'X') CosmicCyan else CosmicOrange
                        
                        // Draw outer glowing line
                        drawLine(
                            color = winLineColor.copy(alpha = 0.45f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 14.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        // Draw inner core line
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.White,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 6.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }

                // Grid Cells
                Column(modifier = Modifier.fillMaxSize()) {
                    for (row in 0..2) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (col in 0..2) {
                                val index = row * 3 + col
                                val cellChar = board[index]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(
                                            if (selectedCellIndex == index) {
                                                if (myChar == 'X') CosmicCyan.copy(alpha = 0.25f) else CosmicOrange.copy(alpha = 0.25f)
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = if (selectedCellIndex == index) 2.dp else 0.dp,
                                            color = if (selectedCellIndex == index) {
                                                if (myChar == 'X') CosmicCyan else CosmicOrange
                                            } else {
                                                Color.Transparent
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .testTag("cell_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isSourceBeingDragged = isDragging && dragSourceIndex == index
                                    val alpha = if (isSourceBeingDragged) 0.3f else 1f
                                    
                                    Box(modifier = Modifier.graphicsLayer(alpha = alpha)) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = cellChar != '_',
                                            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
                                        ) {
                                            if (cellChar == 'X') {
                                                Text(
                                                    "X",
                                                    color = CosmicCyan,
                                                    fontSize = 54.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.shadow(8.dp, CircleShape, ambientColor = CosmicCyan)
                                                )
                                            } else if (cellChar == 'O') {
                                                Text(
                                                    "O",
                                                    color = CosmicOrange,
                                                    fontSize = 54.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.shadow(8.dp, CircleShape, ambientColor = CosmicOrange)
                                                )
                                            }
                                        }
                                    }

                                    // Display static down-arrow indicator if cell is empty and a piece is selected
                                    if (cellChar == '_') {
                                        val pieceCount = board.count { it == myChar }
                                        val showArrow = isMyTurn && pieceCount == 3 && (selectedCellIndex != null || isDragging)
                                        if (showArrow) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "انتقال هنا",
                                                tint = if (myChar == 'X') CosmicCyan.copy(alpha = 0.8f) else CosmicOrange.copy(alpha = 0.8f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Smooth Floating Drag Piece Preview
                if (isDragging && dragSourceIndex != null) {
                    val floatChar = board[dragSourceIndex!!]
                    Box(
                        modifier = Modifier
                            .offset(
                                x = with(LocalDensity.current) { (dragCurrentOffset.x - cellSizePx / 2f).toDp() },
                                y = with(LocalDensity.current) { (dragCurrentOffset.y - cellSizePx / 2f).toDp() }
                            )
                            .size(with(LocalDensity.current) { cellSizePx.toDp() }),
                        contentAlignment = Alignment.Center
                    ) {
                        if (floatChar == 'X') {
                            Text(
                                "X",
                                color = CosmicCyan,
                                fontSize = 58.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.shadow(12.dp, CircleShape, ambientColor = CosmicCyan)
                            )
                        } else if (floatChar == 'O') {
                            Text(
                                "O",
                                color = CosmicOrange,
                                fontSize = 58.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.shadow(12.dp, CircleShape, ambientColor = CosmicOrange)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (match!!.status != "playing") {
                // Game Finished Overlay Actions
                Button(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("الرجوع للقائمة الرئيسية", color = CosmicBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Win Confetti effect generator
@Composable
fun ConfettiParticles() {
    val particles = remember { List(80) { ConfettiParticleState() } }
    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiLoop")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "ConfettiProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (animProgress + p.offsetY) % 1f
            val x = p.offsetX * size.width
            val y = progress * size.height
            drawCircle(
                color = p.color,
                radius = p.size,
                center = Offset(x, y)
            )
        }
    }
}

class ConfettiParticleState {
    val offsetX = Random.nextFloat()
    val offsetY = Random.nextFloat()
    val size = Random.nextFloat() * 10f + 5f
    val color = when (Random.nextInt(4)) {
        0 -> CosmicCyan
        1 -> CosmicGold
        2 -> CosmicOrange
        else -> GlowGreen
    }
}

// ==========================================
// 6. WALLET & TRANSACTIONS HISTORY
// ==========================================
enum class WalletSubScreen {
    Main,
    Deposit,
    Withdrawal
}

@Composable
fun WalletScreen(viewModel: GameViewModel) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val transactionsList by viewModel.transactions.collectAsStateWithLifecycle()
    
    var activeSubScreen by remember { mutableStateOf(WalletSubScreen.Main) }
    
    if (profile == null) return

    val context = LocalContext.current

    AnimatedContent(
        targetState = activeSubScreen,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { if (targetState == WalletSubScreen.Main) -it else it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { if (targetState == WalletSubScreen.Main) it else -it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            )
        },
        label = "wallet_navigation"
    ) { subScreen ->
        when (subScreen) {
            WalletSubScreen.Main -> {
                WalletMainView(
                    profile = profile!!,
                    transactions = transactionsList,
                    onNavigateBack = { viewModel.navigateTo(Screen.Home) },
                    onNavigateToDeposit = { activeSubScreen = WalletSubScreen.Deposit },
                    onNavigateToWithdrawal = { activeSubScreen = WalletSubScreen.Withdrawal }
                )
            }
            WalletSubScreen.Deposit -> {
                DepositView(
                    profile = profile!!,
                    onNavigateBack = { activeSubScreen = WalletSubScreen.Main },
                    onSubmitDeposit = { amount, paymentMethod, proofImage ->
                        viewModel.requestDeposit(amount, paymentMethod, proofImage)
                        activeSubScreen = WalletSubScreen.Main
                    }
                )
            }
            WalletSubScreen.Withdrawal -> {
                WithdrawalView(
                    profile = profile!!,
                    onNavigateBack = { activeSubScreen = WalletSubScreen.Main },
                    onSubmitWithdrawal = { amount, paymentMethod, payoutDetails ->
                        viewModel.requestWithdrawal(amount, paymentMethod, payoutDetails)
                        activeSubScreen = WalletSubScreen.Main
                    }
                )
            }
        }
    }
}

// 6.1. WALLET MAIN VIEW (GORGEOUS FINANCIAL WALLET SCREEN)
@Composable
fun WalletMainView(
    profile: Profile,
    transactions: List<Transaction>,
    onNavigateBack: () -> Unit,
    onNavigateToDeposit: () -> Unit,
    onNavigateToWithdrawal: () -> Unit
) {
    var selectedTransactionForDetails by remember { mutableStateOf<Transaction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Wallet Top Header bar (RTL Arabic)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = TextLight)
            }
            Text(
                "المحفظة الرقمية والعمليات",
                color = TextLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            // Empty space to balance header
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visual Balance Display Card (Premium dark gradient)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = CosmicCyan, spotColor = CosmicCyan)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F1E4A), Color(0xFF060B1C)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("إجمالي الرصيد بالمحفظة", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    formatCurrency(profile.balance),
                    color = CosmicCyan,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions: Deposit / Withdraw with beautiful M3 buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onNavigateToDeposit,
                colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("request_deposit_button")
            ) {
                Icon(Icons.Default.AddCard, contentDescription = null, tint = CosmicBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("شحن رصيد", color = CosmicBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToWithdrawal,
                colors = ButtonDefaults.buttonColors(containerColor = CosmicSlate),
                border = BorderStroke(1.5.dp, CosmicCyan.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("request_withdraw_button")
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = CosmicCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("سحب الرصيد", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // History list section
        Text(
            "سجل العمليات المالية والألعاب",
            color = TextLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد حركات مالية مسجلة في حسابك بعد.", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(transactions) { trans ->
                    TransactionItemRow(
                        transaction = trans,
                        onItemClick = { selectedTransactionForDetails = trans }
                    )
                }
            }
        }
    }

    // Details Modal Dialog for Transaction
    if (selectedTransactionForDetails != null) {
        val trans = selectedTransactionForDetails!!
        AlertDialog(
            onDismissRequest = { selectedTransactionForDetails = null },
            title = {
                Text(
                    text = if (trans.type == "deposit") "تفاصيل طلب الشحن" else "تفاصيل المعاملة المالية",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(label = "نوع المعاملة", value = when (trans.type) {
                        "deposit" -> "إيداع وشحن رصيد"
                        "withdrawal" -> "سحب رصيد"
                        "match_entry" -> "رهان دخول مباراة"
                        "match_win" -> "جائزة فوز بالدورة"
                        else -> "عمولة إدارة"
                    })
                    DetailRow(label = "المبلغ", value = formatCurrency(trans.amount), valueColor = if (trans.type == "match_win" || (trans.type == "deposit" && trans.status == "completed")) GlowGreen else CosmicOrange)
                    DetailRow(label = "تاريخ الطلب", value = trans.createdAt)
                    DetailRow(label = "حالة الطلب", value = when (trans.status) {
                        "pending" -> "⏳ تحت المراجعة"
                        "approved" -> "✅ تم قبول الطلب"
                        "completed" -> "✔️ تم تنفيذ العملية"
                        "rejected" -> "❌ تم رفض الطلب"
                        else -> "⚠️ فشل العملية"
                    }, valueColor = when (trans.status) {
                        "completed" -> GlowGreen
                        "pending" -> CosmicGold
                        "rejected" -> CosmicOrange
                        else -> TextLight
                    })
                    
                    if (!trans.paymentMethod.isNullOrEmpty()) {
                        DetailRow(label = "طريقة التحويل", value = trans.paymentMethod)
                    }

                    if (!trans.payoutDetails.isNullOrEmpty()) {
                        DetailRow(label = "بيانات الاستلام", value = trans.payoutDetails)
                    }

                    if (trans.status == "rejected" && !trans.rejectionReason.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CosmicOrange.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, CosmicOrange, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "سبب الرفض: ${trans.rejectionReason}",
                                color = CosmicOrange,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if ((trans.status == "completed" || trans.status == "approved") && !trans.rejectionReason.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlowGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, GlowGreen, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "ملاحظات الإدارة: ${trans.rejectionReason}",
                                color = GlowGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!trans.proofImage.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("إثبات التحويل المرفوع:", color = TextMuted, fontSize = 13.sp)
                        Base64Image(
                            base64Str = trans.proofImage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CosmicSlate, RoundedCornerShape(12.dp))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedTransactionForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan)
                ) {
                    Text("إغلاق", color = CosmicBackground, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// 6.2. TRANSACTION ITEM ROW COMPONENT
@Composable
fun TransactionItemRow(
    transaction: Transaction,
    onItemClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicCard.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        onClick = onItemClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CosmicSlate.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left amount indicator (Arabic layout reads RTL, so amount on left is great)
            val isAddition = transaction.type == "match_win" || (transaction.type == "deposit" && transaction.status == "completed")
            Text(
                text = "${if (isAddition) "+" else "-"}${formatCurrency(transaction.amount)}",
                color = if (isAddition) GlowGreen else CosmicOrange,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.width(100.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Right Info Details (Arabic reads right-to-left)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(2f)
            ) {
                Text(
                    text = transaction.details,
                    color = TextLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Time/Date Text
                    Text(
                        text = transaction.createdAt.substringBefore("T"),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .background(
                                when (transaction.status) {
                                    "completed" -> GlowGreen.copy(alpha = 0.15f)
                                    "approved" -> GlowGreen.copy(alpha = 0.15f)
                                    "pending" -> CosmicGold.copy(alpha = 0.15f)
                                    "rejected" -> CosmicOrange.copy(alpha = 0.15f)
                                    else -> CosmicOrange.copy(alpha = 0.15f)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (transaction.status) {
                                "pending" -> "⏳ تحت المراجعة"
                                "approved" -> "✅ تم قبول الطلب"
                                "rejected" -> "❌ تم رفض الطلب"
                                "completed" -> "✔️ تم تنفيذ العملية"
                                else -> "⚠️ فشل العملية"
                            },
                            color = when (transaction.status) {
                                "completed" -> GlowGreen
                                "approved" -> GlowGreen
                                "pending" -> CosmicGold
                                "rejected" -> CosmicOrange
                                else -> CosmicOrange
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Show rejection reason banner directly in item list if rejected
                if (transaction.status == "rejected" && !transaction.rejectionReason.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "سبب الرفض: ${transaction.rejectionReason}",
                        color = CosmicOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// 6.3. DEPOSIT VIEW (FULL SCREEN RE-DESIGN)
@Composable
fun DepositView(
    profile: Profile,
    onNavigateBack: () -> Unit,
    onSubmitDeposit: (amount: Double, paymentMethod: String, proofImage: String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val paymentMethods = listOf(
        Triple("فودافون كاش", "01030352077", Icons.Default.PhoneAndroid),
        Triple("إنستاباي", "01030352077", Icons.Default.SendToMobile),
        Triple("PayPal", "hamzanaseer1001@gmail.com", Icons.Default.Email),
        Triple("Visa", "4745010178665205", Icons.Default.CreditCard),
        Triple("حساب بنكي", "EG840002082308230383000001972", Icons.Default.AccountBalance),
        Triple("Binance (USDT)", "0x2d05c38890ab62854a6ba1861df7b06e0adac9f3", Icons.Default.CurrencyBitcoin)
    )

    var selectedMethodIndex by remember { mutableStateOf(0) }
    val selectedMethod = paymentMethods[selectedMethodIndex]

    var amountInput by remember { mutableStateOf("10") }
    var selectedProofImageBase64 by remember { mutableStateOf("") }
    
    // File upload picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                    selectedProofImageBase64 = "data:image/jpeg;base64,$base64"
                    Toast.makeText(context, "تم رفع الصورة بنجاح! 📸", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "فشل قراءة الصورة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = TextLight)
            }
            Text(
                "شحن رصيد المحفظة",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payment Method label
        Text(
            "اختر طريقة الدفع المفضلة:",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Payment Methods horizontal row grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            paymentMethods.forEachIndexed { index, item ->
                val isSelected = index == selectedMethodIndex
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CosmicSlate else CosmicCard
                    ),
                    border = BorderStroke(1.5.dp, if (isSelected) CosmicCyan else Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    onClick = { selectedMethodIndex = index },
                    modifier = Modifier
                        .width(135.dp)
                        .height(95.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.third,
                            contentDescription = null,
                            tint = if (isSelected) CosmicCyan else TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.first,
                            color = if (isSelected) TextLight else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected payment details card
        Text(
            "بيانات التحويل الخاصة بالطريقة المختارة:",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicSlate, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Main address/number field
                val label = when (selectedMethod.first) {
                    "PayPal" -> "البريد الإلكتروني للإرسال"
                    "Visa" -> "رقم بطاقة الفيزا"
                    "حساب بنكي" -> "رقم الحساب البنكي (IBAN)"
                    "Binance (USDT)" -> "عنوان محفظة الإيداع (USDT)"
                    else -> "رقم الهاتف للتحويل المالي"
                }
                
                DetailItemWithCopy(
                    label = label,
                    value = selectedMethod.second,
                    onCopy = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedMethod.second))
                        Toast.makeText(context, "تم نسخ القيمة بنجاح! 📋", Toast.LENGTH_SHORT).show()
                    }
                )

                // Additional details for Binance BEP20 USDT
                if (selectedMethod.first == "Binance (USDT)") {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailItemWithCopy(
                        label = "الشبكة المطلوبة",
                        value = "BEP20",
                        onCopy = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("BEP20"))
                            Toast.makeText(context, "تم نسخ الشبكة! 📋", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("عدد التأكيدات", color = TextMuted, fontSize = 11.sp)
                            Text("1 Confirmation", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الحد الأدنى للإيداع", color = TextMuted, fontSize = 11.sp)
                            Text("1.00 USD", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "الوقت المتوقع لوصول التحويل: حوالي دقيقة واحدة.",
                        color = CosmicGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input Field for amount
        Text(
            "مبلغ الشحن بالدولار الأمريكي (USD):",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Text("$", color = CosmicGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedBorderColor = CosmicCyan,
                unfocusedBorderColor = CosmicSlate,
                focusedContainerColor = CosmicCard,
                unfocusedContainerColor = CosmicCard
            ),
            placeholder = { Text("أدخل مبلغ الشحن المطلوب", color = TextMuted) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Minimum Alert text
        val enteredAmount = amountInput.toDoubleOrNull() ?: 0.0
        val isAmountValid = enteredAmount >= 1.0
        
        if (!isAmountValid && amountInput.isNotEmpty()) {
            Text(
                "الحد الأدنى للشحن هو 1 دولار أمريكي (USD) ولا يسمح بأقل من ذلك.",
                color = CosmicOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mandatory Upload Transfer Proof Image
        Text(
            "رفع صورة إثبات التحويل (إلزامي):",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = CosmicSlate),
            border = BorderStroke(1.dp, CosmicCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CosmicCyan)
            Spacer(modifier = Modifier.width(6.dp))
            Text("اختر صورة إثبات التحويل من الاستوديو", color = TextLight, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Image preview or missing warning
        if (selectedProofImageBase64.isNotEmpty()) {
            Text("معاينة الإثبات المرفوع:", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Base64Image(
                base64Str = selectedProofImageBase64,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, GlowGreen, RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(CosmicOrange.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .border(1.dp, CosmicOrange.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "❌ يجب رفع صورة إثبات التحويل من الاستوديو للمتابعة",
                    color = CosmicOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Severe Warning box
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicOrange.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicOrange.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "⚠️ تحذير أمني هام جداً:",
                    color = CosmicOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "يجب رفع صورة حقيقية وواضحة لإثبات التحويل. في حال إرسال إثبات مزيف أو التلاعب في الصورة، سيتم رفض الطلب تلقائياً، وقد يؤدي ذلك إلى إيقاف أو حظر حسابك بالكامل وبشكل دائم.",
                    color = TextLight,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Right
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        val canSubmit = isAmountValid && selectedProofImageBase64.isNotEmpty()
        Button(
            onClick = {
                onSubmitDeposit(enteredAmount, selectedMethod.first, selectedProofImageBase64)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canSubmit) GlowGreen else CosmicSlate
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "إرسال طلب الشحن للإدارة",
                color = if (canSubmit) CosmicBackground else TextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 6.4. WITHDRAWAL VIEW (FULL SCREEN RE-DESIGN)
@Composable
fun WithdrawalView(
    profile: Profile,
    onNavigateBack: () -> Unit,
    onSubmitWithdrawal: (amount: Double, paymentMethod: String, payoutDetails: String) -> Unit
) {
    val context = LocalContext.current

    val withdrawalMethods = listOf(
        "فودافون كاش",
        "إنستاباي",
        "PayPal",
        "حساب بنكي",
        "Visa",
        "Binance (USDT)"
    )

    var selectedMethodIndex by remember { mutableStateOf(0) }
    var amountInput by remember { mutableStateOf("10") }
    var payoutDetails by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = TextLight)
            }
            Text(
                "سحب رصيد المحفظة",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visual Balance Header
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CosmicSlate, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("الرصيد المتاح للسحب الفوري", color = TextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatCurrency(profile.balance),
                    color = GlowGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Selection of payout method
        Text(
            "اختر طريقة استلام الأموال:",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            withdrawalMethods.forEachIndexed { index, method ->
                val isSelected = index == selectedMethodIndex
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CosmicSlate else CosmicCard
                    ),
                    border = BorderStroke(1.5.dp, if (isSelected) CosmicCyan else Color.Transparent),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { selectedMethodIndex = index },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .height(50.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            color = if (isSelected) TextLight else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Payout recipient details
        Text(
            "بيانات الاستلام (رقم الهاتف / البريد الإلكتروني / الحساب البنكي):",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = payoutDetails,
            onValueChange = { payoutDetails = it },
            placeholder = { Text("أدخل رقم المحفظة أو الحساب البنكي للتسليم", color = TextMuted, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedBorderColor = CosmicCyan,
                unfocusedBorderColor = CosmicSlate,
                focusedContainerColor = CosmicCard,
                unfocusedContainerColor = CosmicCard
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Amount input
        Text(
            "مبلغ السحب بالدولار الأمريكي (USD):",
            color = TextLight,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Text("$", color = CosmicGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedBorderColor = CosmicCyan,
                unfocusedBorderColor = CosmicSlate,
                focusedContainerColor = CosmicCard,
                unfocusedContainerColor = CosmicCard
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Validations
        val enteredAmount = amountInput.toDoubleOrNull() ?: 0.0
        val isAmountValid = enteredAmount >= 1.0 && enteredAmount <= profile.balance
        
        if (amountInput.isNotEmpty()) {
            if (enteredAmount < 1.0) {
                Text(
                    "الحد الأدنى لطلب السحب هو 1 دولار أمريكي (USD).",
                    color = CosmicOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (enteredAmount > profile.balance) {
                Text(
                    "رصيدك الحالي غير كافٍ لإتمام عملية السحب.",
                    color = CosmicOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "ملاحظة هامة: لن يتم خصم المبلغ من رصيدك إلا بعد مراجعة وموافقة الإدارة taj للتأكد من وصول الحوالة لك.",
            color = CosmicGold,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Right
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Submit button
        val canSubmit = isAmountValid && payoutDetails.trim().isNotEmpty()
        Button(
            onClick = {
                onSubmitWithdrawal(enteredAmount, withdrawalMethods[selectedMethodIndex], payoutDetails)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canSubmit) CosmicCyan else CosmicSlate
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "إرسال طلب السحب للإدارة",
                color = if (canSubmit) CosmicBackground else TextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 6.5. DECODE BASE64 PROGRAMMATIC IMAGE COMPONENT
@Composable
fun Base64Image(base64Str: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64Str) {
        try {
            val cleanStr = if (base64Str.startsWith("data:image")) {
                base64Str.substringAfter("base64,")
            } else {
                base64Str
            }
            val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "إثبات التحويل المالي",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier.background(CosmicSlate, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("فشل تحميل صورة الإثبات المالي", color = TextMuted, fontSize = 12.sp)
        }
    }
}

// 6.6. HELPER SUB-COMPONENTS
@Composable
fun DetailRow(label: String, value: String, valueColor: Color = TextLight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Left)
        Text(text = label, color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Right)
    }
}

@Composable
fun DetailItemWithCopy(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .border(1.dp, CosmicSlate, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Copy icon button on the left
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "نسخ",
                    tint = CosmicCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Value text on the right
            Text(
                text = value,
                color = TextLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 6.7. MOCK RECEIPT PROGRAMMATIC GENERATION FOR TESTING IN EMULATORS
fun generateMockReceiptBitmap(amount: Double, method: String, username: String): String {
    val width = 400
    val height = 450
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    // Draw card background
    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    
    // Draw a nice green border at the top for receipt style
    paint.color = android.graphics.Color.parseColor("#20E377")
    canvas.drawRect(0f, 0f, width.toFloat(), 25f, paint)
    
    // Draw receipt title
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 24f
    paint.isFakeBoldText = true
    paint.isAntiAlias = true
    canvas.drawText("TAJ XO WALLET RECEIPT", 40f, 70f, paint)
    
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 16f
    canvas.drawText("--------------------------------------------------", 40f, 100f, paint)
    
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 18f
    paint.isFakeBoldText = false
    canvas.drawText("User Name: $username", 40f, 140f, paint)
    canvas.drawText("Payment Method: $method", 40f, 185f, paint)
    
    paint.color = android.graphics.Color.parseColor("#070B19")
    paint.textSize = 22f
    paint.isFakeBoldText = true
    canvas.drawText("Amount: $$amount USD", 40f, 235f, paint)
    
    paint.color = android.graphics.Color.parseColor("#20E377")
    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText("Status: TRANSFERRED (SUCCESS)", 40f, 280f, paint)
    
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 16f
    paint.isFakeBoldText = false
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    val dateStr = sdf.format(java.util.Date())
    canvas.drawText("Date/Time: $dateStr", 40f, 330f, paint)
    
    paint.color = android.graphics.Color.parseColor("#8C9BB4")
    paint.textSize = 14f
    canvas.drawText("This receipt is verified digitally.", 40f, 380f, paint)
    canvas.drawText("Thank you for using XO MAX application.", 40f, 410f, paint)
    
    val byteArrayOutputStream = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

// ==========================================
// 7. ADMIN PANEL SCREEN
// ==========================================
@Composable
fun AdminPanelScreen(viewModel: GameViewModel) {
    val pendingTrans by viewModel.pendingTransactions.collectAsStateWithLifecycle()
    val adminProfiles by viewModel.adminProfiles.collectAsStateWithLifecycle()
    val monitoringLogs by viewModel.monitoringLogs.collectAsStateWithLifecycle()
    var activeAdminTab by remember { mutableStateOf(0) } // 0: Requests, 1: Monitoring Logs

    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var showRejectionDialogForTransId by remember { mutableStateOf<String?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var selectedRejectionOption by remember { mutableStateOf(1) } // 1: "التحويل الوهمي", 2: "المبلغ غير كامل", 3: Custom only

    var showApprovalDialogForTransId by remember { mutableStateOf<String?>(null) }
    var approvalReasonInput by remember { mutableStateOf("") }
    var selectedApprovalOption by remember { mutableStateOf(1) } // 1: "تم قبول عملية السحب...", 2: Custom only

    var viewingProofImageBase64 by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = TextLight)
            }

            Text("لوحة تحكم الإدارة taj", color = CosmicGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            IconButton(onClick = { viewModel.loadAdminPanel() }) {
                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = CosmicCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { activeAdminTab = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeAdminTab == 0) CosmicCyan else CosmicCard
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("الطلبات المالية", color = if (activeAdminTab == 0) CosmicBackground else TextLight, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { activeAdminTab = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeAdminTab == 1) CosmicCyan else CosmicCard
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("سجل المراقبة", color = if (activeAdminTab == 1) CosmicBackground else TextLight, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeAdminTab == 0) {
            Text(
                "طلبات الشحن والسحب المعلقة",
                color = TextLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (pendingTrans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد طلبات معلقة مسجلة حالياً.", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingTrans) { trans ->
                        val transUser = adminProfiles[trans.userId]
                        val username = transUser?.username ?: "تحميل الاسم..."
                        val avatarUrl = transUser?.avatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${trans.userId}"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // User Info Header (Avatar + Username + Time)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "صورة المستخدم",
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, CosmicCyan, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = username,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        // Transaction Time
                                        val formattedTime = try {
                                            if (trans.createdAt.contains("T")) {
                                                val date = trans.createdAt.substringBefore("T")
                                                val time = trans.createdAt.substringAfter("T").substring(0, 5)
                                                "📅 $date  ⏰ $time"
                                            } else {
                                                "📅 ${trans.createdAt}"
                                            }
                                        } catch (e: Exception) {
                                            trans.createdAt
                                        }

                                        Text(
                                            text = formattedTime,
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Type and Amount Badge
                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (trans.type == "deposit") GlowGreen.copy(alpha = 0.15f) else CosmicOrange.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (trans.type == "deposit") "شحن 📥" else "سحب 📤",
                                                color = if (trans.type == "deposit") GlowGreen else CosmicOrange,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatCurrency(trans.amount),
                                            color = TextLight,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Transaction description / info
                                Text("ملاحظات الطلب:", color = CosmicGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(trans.details, color = TextLight, fontSize = 13.sp)

                                Spacer(modifier = Modifier.height(8.dp))

                                // Payment method display
                                if (!trans.paymentMethod.isNullOrEmpty()) {
                                    Text("طريقة الدفع: ${trans.paymentMethod}", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // If it is a deposit, show proof image button
                                if (trans.type == "deposit" && !trans.proofImage.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewingProofImageBase64 = trans.proofImage },
                                        colors = ButtonDefaults.buttonColors(containerColor = CosmicSlate),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = CosmicCyan)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("👁️ عرض صورة إثبات التحويل المالي", color = TextLight, fontSize = 12.sp)
                                    }
                                }

                                // If it is a withdrawal, show receipt details with copy button
                                if (trans.type == "withdrawal" && !trans.payoutDetails.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DetailItemWithCopy(
                                        label = "بيانات استلام السحب الخاصة بالمستخدم",
                                        value = trans.payoutDetails,
                                        onCopy = {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(trans.payoutDetails))
                                            Toast.makeText(context, "تم نسخ بيانات الاستلام! 📋", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { showApprovalDialogForTransId = trans.id },
                                        colors = ButtonDefaults.buttonColors(containerColor = GlowGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("موافقة واعتماد", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { showRejectionDialogForTransId = trans.id },
                                        colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("رفض وإلغاء", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "سجل العمليات والرقابة العامة للكابتن",
                color = TextLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (monitoringLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد سجلات مراقبة مسجلة حالياً.", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(monitoringLogs) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicCard.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (log.action) {
                                            "login" -> "تسجيل دخول 🔑"
                                            "register" -> "تسجيل جديد 👤"
                                            "deposit_request" -> "طلب شحن 📥"
                                            "withdrawal_request" -> "طلب سحب 📤"
                                            "deposit_approved" -> "قبول شحن ✅"
                                            "withdrawal_approved" -> "قبول سحب ✅"
                                            "deposit_rejected" -> "رفض شحن ❌"
                                            "withdrawal_rejected" -> "رفض سحب ❌"
                                            "challenge_accepted" -> "بدء تحدي ⚔️"
                                            "match_completed" -> "مباراة منتهية 🏆"
                                            else -> log.action
                                        },
                                        color = CosmicGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = log.createdAt.substringBefore("T") + " " + log.createdAt.substringAfter("T").substring(0, 5),
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(log.details, color = TextLight, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Helper custom option card for dialogs
    @Composable
    fun DialogOptionCard(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        activeColor: androidx.compose.ui.graphics.Color
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) activeColor.copy(alpha = 0.12f) else CosmicBackground)
                .border(
                    1.dp,
                    if (isSelected) activeColor else CosmicSlate,
                    RoundedCornerShape(10.dp)
                )
                .clickable { onClick() }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (isSelected) activeColor else CosmicSlate)
                )
                Text(
                    text = text,
                    color = if (isSelected) TextLight else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Modern Rejection Dialog with Reason Input & Presets
    if (showRejectionDialogForTransId != null) {
        val transId = showRejectionDialogForTransId!!
        AlertDialog(
            onDismissRequest = { 
                showRejectionDialogForTransId = null
                rejectionReasonInput = ""
                selectedRejectionOption = 1
            },
            title = { Text("سبب رفض طلب المعاملة المالية ❌", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حدد رسالة الرفض المناسبة أو اكتب تفاصيل مخصصة:", color = TextMuted, fontSize = 13.sp)
                    
                    DialogOptionCard(
                        text = "التحويل وهمي/خادع: تم رفض عملية التحويل بسبب التحويل الوهمي أو الخادع والفلوس لم تصل ولو العملية تكررت سوف يتم قفل الحساب نهائياً.",
                        isSelected = selectedRejectionOption == 1,
                        onClick = { selectedRejectionOption = 1 },
                        activeColor = CosmicOrange
                    )

                    DialogOptionCard(
                        text = "المبلغ غير كامل: عذراً، المبلغ المرسل غير كامل والتحويل لا يطابق القيمة المطلوبة.",
                        isSelected = selectedRejectionOption == 2,
                        onClick = { selectedRejectionOption = 2 },
                        activeColor = CosmicOrange
                    )

                    DialogOptionCard(
                        text = "كتابة رسالة مخصصة مئة بالمئة فقط",
                        isSelected = selectedRejectionOption == 3,
                        onClick = { selectedRejectionOption = 3 },
                        activeColor = CosmicOrange
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("ملحوظات مخصصة إضافية من الإدارة (تظهر مع الرسالة):", color = TextMuted, fontSize = 12.sp)

                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        placeholder = { Text("اكتب تفاصيل إضافية للمستخدم هنا...", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = CosmicOrange,
                            unfocusedBorderColor = CosmicSlate,
                            focusedContainerColor = CosmicBackground,
                            unfocusedContainerColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = buildString {
                            if (selectedRejectionOption == 1) {
                                append("تم رفض عملية التحويل بسبب التحويل الوهمي أو الخادع والفلوس لم تصل، ولو العملية تكررت سوف يتم قفل الحساب نهائياً.")
                            } else if (selectedRejectionOption == 2) {
                                append("عذراً، المبلغ المرسل غير كامل والتحويل لا يطابق القيمة المطلوبة.")
                            }
                            val customText = rejectionReasonInput.trim()
                            if (customText.isNotEmpty()) {
                                if (isNotEmpty()) append(" ")
                                append(customText)
                            }
                        }.trim().ifEmpty { "تم الرفض من قبل الإدارة بسبب عدم مطابقة البيانات." }

                        viewModel.rejectTransaction(transId, finalReason)
                        showRejectionDialogForTransId = null
                        rejectionReasonInput = ""
                        selectedRejectionOption = 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange)
                ) {
                    Text("تأكيد الرفض والإلغاء", color = TextLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRejectionDialogForTransId = null
                    rejectionReasonInput = ""
                    selectedRejectionOption = 1
                }) {
                    Text("إلغاء", color = TextMuted)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modern Approval Dialog with Custom Message Presets
    if (showApprovalDialogForTransId != null) {
        val transId = showApprovalDialogForTransId!!
        AlertDialog(
            onDismissRequest = { 
                showApprovalDialogForTransId = null
                approvalReasonInput = ""
                selectedApprovalOption = 1
            },
            title = { Text("اعتماد وقبول المعاملة المالية ✅", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حدد رسالة القبول المناسبة أو اكتب تفاصيل مخصصة:", color = TextMuted, fontSize = 13.sp)
                    
                    DialogOptionCard(
                        text = "القبول النموذجي: تم قبول عملية السحب أو الشحن بنجاح ونرحب بكم في منصتنا.",
                        isSelected = selectedApprovalOption == 1,
                        onClick = { selectedApprovalOption = 1 },
                        activeColor = GlowGreen
                    )

                    DialogOptionCard(
                        text = "كتابة رسالة مخصصة مئة بالمئة فقط",
                        isSelected = selectedApprovalOption == 2,
                        onClick = { selectedApprovalOption = 2 },
                        activeColor = GlowGreen
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("ملحوظات مخصصة إضافية من الإدارة (تظهر مع الرسالة):", color = TextMuted, fontSize = 12.sp)

                    OutlinedTextField(
                        value = approvalReasonInput,
                        onValueChange = { approvalReasonInput = it },
                        placeholder = { Text("اكتب تفاصيل إضافية للمستخدم هنا...", color = TextMuted, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = GlowGreen,
                            unfocusedBorderColor = CosmicSlate,
                            focusedContainerColor = CosmicBackground,
                            unfocusedContainerColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalMessage = buildString {
                            if (selectedApprovalOption == 1) {
                                append("تم قبول عملية السحب أو الشحن بنجاح.")
                            }
                            val customText = approvalReasonInput.trim()
                            if (customText.isNotEmpty()) {
                                if (isNotEmpty()) append(" ")
                                append(customText)
                            }
                        }.trim().ifEmpty { "تم قبول عملية السحب أو الشحن بنجاح." }

                        viewModel.approveTransaction(transId, finalMessage)
                        showApprovalDialogForTransId = null
                        approvalReasonInput = ""
                        selectedApprovalOption = 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowGreen)
                ) {
                    Text("تأكيد القبول والاعتماد", color = CosmicBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showApprovalDialogForTransId = null
                    approvalReasonInput = ""
                    selectedApprovalOption = 1
                }) {
                    Text("إلغاء", color = TextMuted)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal view for checking base64 receipts
    if (viewingProofImageBase64 != null) {
        AlertDialog(
            onDismissRequest = { viewingProofImageBase64 = null },
            title = { Text("إثبات التحويل المرفوع", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(CosmicBackground, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Base64Image(
                        base64Str = viewingProofImageBase64!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewingProofImageBase64 = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan)
                ) {
                    Text("إغلاق معاينة الصورة", color = CosmicBackground, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ==========================================
// 8. PROFILE MAIN SCREEN
// ==========================================
@Composable
fun ProfileScreen(viewModel: GameViewModel) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    var showSupportDialogInProfile by remember { mutableStateOf(false) }

    if (profile == null) return

    // Technical Support Dialog for ProfileScreen
    if (showSupportDialogInProfile) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showSupportDialogInProfile = false },
            title = {
                Text("الدعم الفني ومساعدة اللاعبين 📞", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "يسعدنا دائماً تواصلك معنا لحل أي مشاكل فنية، استعادة أو إنشاء الحسابات، أو الاستفسار عن المعاملات المالية.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicSlate)
                            .padding(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("البريد الإلكتروني المباشر للدعم:", color = CosmicCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "h7amzanasser@gmail.com",
                                color = TextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("h7amzanasser@gmail.com"))
                            Toast.makeText(context, "تم نسخ البريد الإلكتروني بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CosmicBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نسخ البريد الإلكتروني", color = CosmicBackground, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:h7amzanasser@gmail.com")
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "طلب دعم فني - تطبيق XO MAX")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم يتم العثور على تطبيق بريد إلكتروني.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicSlate),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TextLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال رسالة بريد مباشر", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSupportDialogInProfile = false }) {
                    Text("إغلاق", color = TextMuted)
                }
            },
            containerColor = CosmicCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Avatar with animated glow / border
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = profile!!.avatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${profile!!.username}",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(3.dp, CosmicCyan, CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Username & Account Number
        var profileClickCount by remember { mutableStateOf(0) }
        var profileLastClickTime by remember { mutableStateOf(0L) }
        Text(
            text = profile!!.username,
            color = TextLight,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                if (profile!!.username == "hamzanasser") {
                    val now = System.currentTimeMillis()
                    if (now - profileLastClickTime < 1000) {
                        profileClickCount++
                    } else {
                        profileClickCount = 1
                    }
                    profileLastClickTime = now
                    if (profileClickCount >= 3) {
                        profileClickCount = 0
                        viewModel.toggleComputerBotMode()
                    }
                }
            }
        )

        if (profile!!.accountNumber != null) {
            Text(
                text = "رقم الحساب المميز: #${profile!!.accountNumber}",
                color = CosmicCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(CosmicSlate, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Level details card
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المستوى الحالي", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("المستوى ${profile!!.level}", color = CosmicGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress towards next level
                val progress = getLevelProgress(profile!!.points)
                LinearProgressIndicator(
                    progress = { progress },
                    color = CosmicCyan,
                    trackColor = CosmicSlate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("النقاط: ${profile!!.points}", color = TextMuted, fontSize = 12.sp)
                    Text("متبقي ${(getNextLevelPoints(profile!!.points) - profile!!.points)} نقطة للمستوى التالي", color = CosmicCyan, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balances card (Available)
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(CosmicSlate.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("الرصيد الحالي", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    formatCurrency(profile!!.balance),
                    color = GlowGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Button(
            onClick = { viewModel.navigateTo(Screen.Statistics) },
            colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = CosmicBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text("عرض الإحصائيات الحقيقية", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.navigateTo(Screen.EditProfile) },
            colors = ButtonDefaults.buttonColors(containerColor = CosmicSlate),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TextLight)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تعديل اسم وصورة الحساب", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { 
                viewModel.loadUserSessions()
                viewModel.navigateTo(Screen.PrivacySecurity) 
            },
            colors = ButtonDefaults.buttonColors(containerColor = CosmicSlate),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextLight)
            Spacer(modifier = Modifier.width(8.dp))
            Text("الخصوصية والأمان", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showSupportDialogInProfile = true },
            colors = ButtonDefaults.buttonColors(containerColor = CosmicGold),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = CosmicBackground)
            Spacer(modifier = Modifier.width(8.dp))
            Text("الدعم الفني والاتصال 📞", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ==========================================
// 9. EDIT PROFILE SCREEN
// ==========================================
@Composable
fun EditProfileScreen(viewModel: GameViewModel) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()

    if (profile == null) return

    var usernameInput by remember { mutableStateOf(profile!!.username) }
    var avatarBase64 by remember { mutableStateOf<String?>(profile!!.avatarUrl) }
    var selectedBitmapForCropper by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                // Open cropper dialog
                selectedBitmapForCropper = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Avatar selector / preview
        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = avatarBase64 ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${profile!!.username}",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, CosmicCyan, CircleShape),
                contentScale = ContentScale.Crop
            )

            // Edit icon badge on bottom-right of avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(CosmicCyan, CircleShape)
                    .border(2.dp, CosmicBackground, CircleShape)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = CosmicBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "اضغط على الصورة لتعديلها",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Username field
        OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it },
            label = { Text("اسم المستخدم الجديد", color = TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CosmicCyan,
                unfocusedBorderColor = CosmicSlate,
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                if (usernameInput.trim().isEmpty()) return@Button
                viewModel.updateProfile(usernameInput, avatarBase64 ?: "") {
                    viewModel.navigateTo(Screen.Profile)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("حفظ التغييرات ومزامنة البيانات", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }

    // Interactive Image Cropper Dialog
    if (selectedBitmapForCropper != null) {
        ImageCropperDialog(
            bitmap = selectedBitmapForCropper!!,
            onDismiss = { selectedBitmapForCropper = null },
            onCropped = { croppedBitmap ->
                selectedBitmapForCropper = null
                avatarBase64 = croppedBitmap.toBase64String()
            }
        )
    }
}

// ==========================================
// 10. STATISTICS SCREEN
// ==========================================
@Composable
fun StatisticsScreen(viewModel: GameViewModel) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()

    if (profile == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Gamer Profile Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmicCard, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profile!!.avatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${profile!!.username}",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, CosmicCyan, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(profile!!.username, color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("مستوى ${profile!!.level} • ${profile!!.points} نقطة", color = CosmicGold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "إحصائيات الألعاب المزامنة حالياً",
            color = TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        // Grid of Stats
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Wins & Losses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isH7 = profile!!.username == "h.7"
                StatCard(
                    title = "مرات الفوز",
                    value = if (isH7) "-" else "${profile!!.wins}",
                    color = GlowGreen,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "مرات الخسارة",
                    value = if (isH7) "-" else "${profile!!.losses}",
                    color = CosmicOrange,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Win Rate & Total Matches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isH7 = profile!!.username == "h.7"
                val winPercent = if ((profile!!.wins + profile!!.losses) > 0) {
                    (profile!!.wins.toDouble() / (profile!!.wins + profile!!.losses)) * 100
                } else 0.0

                StatCard(
                    title = "نسبة الفوز",
                    value = if (isH7) "-" else "${String.format("%.1f", winPercent)}%",
                    color = CosmicCyan,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "إجمالي المباريات",
                    value = if (isH7) "-" else "${profile!!.wins + profile!!.losses}",
                    color = TextLight,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Streak & Last Match Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "أطول سلسلة انتصارات",
                    value = "${profile!!.longestStreak} متتالية",
                    color = CosmicGold,
                    modifier = Modifier.weight(1f)
                )

                val formattedDate = if (!profile!!.lastMatchAt.isNullOrEmpty()) {
                    profile!!.lastMatchAt!!.substringBefore("T")
                } else {
                    "لا يوجد حالياً"
                }

                StatCard(
                    title = "تاريخ آخر مباراة",
                    value = formattedDate,
                    color = TextLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CosmicCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ImageCropperDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val densityVal = LocalDensity.current
            val cropSizeDp = 240.dp
            val cropSizePx = with(densityVal) { cropSizeDp.toPx() }

            Text(
                text = "اسحب وقرب لتحديد الجزء الظاهر ✂️",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .align(Alignment.Center)
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offset = offset + pan
                        }
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = ContentScale.Fit
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color.Black.copy(alpha = 0.75f))
                    drawCircle(
                        color = Color.Transparent,
                        radius = cropSizePx / 2f,
                        center = center,
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )
                }

                Box(
                    modifier = Modifier
                        .size(cropSizeDp)
                        .align(Alignment.Center)
                        .border(2.dp, CosmicCyan, CircleShape)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSlate),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء", color = TextLight)
                }

                Button(
                    onClick = {
                        val cropped = cropProfileBitmap(
                            source = bitmap,
                            containerWidth = screenWidthPx,
                            containerHeight = screenWidthPx,
                            cropSize = cropSizePx,
                            scale = scale,
                            offsetX = offset.x,
                            offsetY = offset.y
                        )
                        onCropped(cropped)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("قص وحفظ الصّورة", color = CosmicBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(viewModel: GameViewModel) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = TextLight)
            }

            Text("مركز الإشعارات", color = CosmicGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            IconButton(onClick = { viewModel.loadNotifications() }) {
                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = CosmicCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("لا توجد إشعارات حالياً.", color = TextMuted, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { item ->
                    val icon = when (item.type) {
                        "win" -> Icons.Default.Star
                        "loss" -> Icons.Default.Warning
                        "deposit_success" -> Icons.Default.AddCircle
                        "withdrawal_success" -> Icons.Default.RemoveCircle
                        "invitation_accepted" -> Icons.Default.Check
                        "invitation_rejected" -> Icons.Default.Close
                        else -> Icons.Default.Info
                    }

                    val color = when (item.type) {
                        "win", "deposit_success" -> GlowGreen
                        "loss" -> CosmicOrange
                        "withdrawal_success" -> CosmicCyan
                        "invitation_accepted" -> CosmicGold
                        else -> TextLight
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isRead) CosmicCard.copy(alpha = 0.5f) else CosmicCard
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!item.isRead) {
                                    viewModel.markNotificationAsRead(item.id)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.title,
                                        color = if (item.isRead) TextMuted else TextLight,
                                        fontSize = 14.sp,
                                        fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold
                                    )
                                    if (!item.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CosmicOrange)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    item.message,
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    item.createdAt.substringBefore("T"),
                                    color = TextMuted.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. FRIENDS SYSTEM SCREENS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(viewModel: GameViewModel) {
    val friendsList by viewModel.friendsList.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val incomingInvitations by viewModel.incomingInvitations.collectAsStateWithLifecycle()
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()

    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedFriendForInvite by remember { mutableStateOf<FriendUser?>(null) }
    var betAmountString by remember { mutableStateOf("5") }

    LaunchedEffect(Unit) {
        viewModel.loadFriendsList()
        viewModel.loadPendingFriendRequests()
        viewModel.loadIncomingInvitations()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الأصدقاء واللعب المشترك",
                color = TextLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Search button
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.SearchUsers) },
                    modifier = Modifier
                        .background(CosmicSlate, CircleShape)
                        .testTag("go_to_search_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "بحث", tint = CosmicCyan)
                }

                // Requests badge button
                Box {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.FriendRequests) },
                        modifier = Modifier
                            .background(CosmicSlate, CircleShape)
                            .testTag("friend_requests_badge_button")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "طلبات الصداقة", tint = CosmicCyan)
                    }
                    if (pendingRequests.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .background(CosmicOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pendingRequests.size.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Incoming Match Invitations Section
        if (incomingInvitations.isNotEmpty()) {
            Text(
                text = "دعوات المباريات الواردة ⚔️",
                color = CosmicOrange,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            ) {
                items(incomingInvitations) { invitation ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("match_invite_card_${invitation.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "دعوة من ${invitation.senderUsername}",
                                    color = TextLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "مبلغ الرهان: ${invitation.betAmount} ر.س",
                                    color = CosmicCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.acceptMatchInvitation(invitation) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("accept_invite_${invitation.id}")
                                ) {
                                    Text("قبول", color = Color.Black, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.rejectMatchInvitation(invitation) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("reject_invite_${invitation.id}")
                                ) {
                                    Text("رفض", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Friends List Section
        Text(
            text = "قائمة الأصدقاء (${friendsList.size})",
            color = TextLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (friendsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا يوجد أصدقاء بعد. ابدأ بالبحث عن لاعبين وإضافتهم!",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(friendsList) { friend ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("friend_card_${friend.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.clickable { viewModel.viewUserProfile(friend.id) }
                            ) {
                                // Online/Offline Indicator with Avatar
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(CosmicSlate, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = TextLight)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(12.dp)
                                            .background(
                                                if (friend.status == "online") Color.Green else Color.Gray,
                                                CircleShape
                                            )
                                            .border(1.5.dp, CosmicCard, CircleShape)
                                    )
                                }

                                Column {
                                    Text(
                                        text = friend.username,
                                        color = TextLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "المستوى ${friend.level}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (friend.status == "online") "🟢 متصل الآن" else formatLastSeen(friend.lastSeenAt),
                                            color = if (friend.status == "online") Color.Green else TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            if (friend.status == "online") {
                                Button(
                                    onClick = {
                                        selectedFriendForInvite = friend
                                        showInviteDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("challenge_friend_${friend.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تحدي", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "غير متصل",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Invitation Dialog
    if (showInviteDialog && selectedFriendForInvite != null) {
        var isFreeInvite by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            containerColor = CosmicCard,
            title = {
                Text(
                    text = "دعوة تحدي لمباراة ⚔️",
                    color = TextLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "اختر نوع المباراة ومبلغ الرهان ضد ${selectedFriendForInvite!!.username}:",
                        color = TextLight,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isFreeInvite,
                            onCheckedChange = { isFreeInvite = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CosmicCyan,
                                uncheckedColor = CosmicSlate,
                                checkmarkColor = CosmicBackground
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مباراة مجانية (بدون رهان مالي)", color = TextLight, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isFreeInvite) {
                        OutlinedTextField(
                            value = betAmountString,
                            onValueChange = { betAmountString = it },
                            label = { Text("قيمة الرهان (ر.س)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedLabelColor = CosmicCyan,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "في الوضع المجاني، لن يتم خصم أو إضافة أي رصيد مالي. سيتم فقط احتساب نقاط الفوز والخسارة وتحديث المستوى.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = if (isFreeInvite) 0.0 else (betAmountString.toDoubleOrNull() ?: 5.0)
                        viewModel.sendMatchInvitation(selectedFriendForInvite!!.id, amt)
                        showInviteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CosmicCyan)
                ) {
                    Text("إرسال الدعوة")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showInviteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = CosmicOrange)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: GameViewModel) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Button & Search Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Friends) },
                modifier = Modifier.background(CosmicSlate, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextLight)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "البحث عن لاعبين",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search text field
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.searchUsers(it)
            },
            placeholder = { Text("ابحث بالاسم أو بمعرف اللاعب (User ID)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CosmicCyan) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CosmicCyan,
                unfocusedBorderColor = CosmicSlate,
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedLabelColor = CosmicCyan,
                unfocusedLabelColor = TextMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results LazyColumn
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isEmpty()) "ابدأ بكتابة اسم اللاعب للبحث عنه..." else "لم يتم العثور على أي لاعبين.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(searchResults) { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_result_item_${user.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.clickable { viewModel.viewUserProfile(user.id) }
                            ) {
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(CosmicSlate, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = TextLight)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(12.dp)
                                            .background(
                                                if (user.status == "online") Color.Green else Color.Gray,
                                                CircleShape
                                            )
                                            .border(1.5.dp, CosmicCard, CircleShape)
                                    )
                                }

                                Column {
                                    Text(
                                        text = user.username,
                                        color = TextLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "المستوى ${user.level}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (user.status == "online") "🟢 متصل الآن" else formatLastSeen(user.lastSeenAt),
                                            color = if (user.status == "online") Color.Green else TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Friendship Actions
                            if (user.requestId != null && user.requestId == "accepted") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                                    Text("صديق", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (user.requestId != null && user.requestId == "pending_sent") {
                                Button(
                                    onClick = { viewModel.cancelFriendRequest(user.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("cancel_request_button_${user.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إلغاء الطلب", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (user.requestId != null && user.requestId == "pending_received") {
                                Button(
                                    onClick = { viewModel.navigateTo(Screen.FriendRequests) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("مراجعة الطلب", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (user.requestId != null && user.requestId == "self") {
                                Text("حسابك الشخصي", color = CosmicCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Button(
                                    onClick = { viewModel.sendFriendRequest(user.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("send_request_button_${user.id}")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة صديق", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRequestsScreen(viewModel: GameViewModel) {
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Button & Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Friends) },
                modifier = Modifier.background(CosmicSlate, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextLight)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "طلبات الصداقة المعلقة",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد طلبات صداقة واردة حالياً.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(pendingRequests) { request ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pending_request_item_${request.requestId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.clickable { viewModel.viewUserProfile(request.senderId) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(CosmicSlate, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TextLight)
                                }

                                Column {
                                    Text(
                                        text = request.senderUsername,
                                        color = TextLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "المستوى ${request.senderLevel}",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.respondToFriendRequest(request.requestId, "accept") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("accept_friend_${request.requestId}")
                                ) {
                                    Text("قبول", color = Color.Black, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.respondToFriendRequest(request.requestId, "reject") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("reject_friend_${request.requestId}")
                                ) {
                                    Text("رفض", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LobbyScreen(viewModel: GameViewModel) {
    val lobbyMatch by viewModel.lobbyMatch.collectAsStateWithLifecycle()
    val lobbyInvitation by viewModel.lobbyInvitation.collectAsStateWithLifecycle()
    val matchChats by viewModel.matchChats.collectAsStateWithLifecycle()
    val isMicMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()
    val isVoiceConnected by viewModel.isVoiceConnected.collectAsStateWithLifecycle()
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()

    var typedMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.startLobbyPolling()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "غرفة انتظار اللعب المشترك ⚔️",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { viewModel.leaveLobby() },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange),
                modifier = Modifier.testTag("leave_lobby_button")
            ) {
                Text("مغادرة", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Players & Voice status layout
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current User
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(CosmicSlate, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextLight, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = profile?.username ?: "", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "أنت", color = CosmicCyan, fontSize = 12.sp)
                    }

                    Text(text = "ضد", color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    // Opponent
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(CosmicSlate, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextLight, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lobbyInvitation?.senderId == profile?.id) "الخصم" else lobbyInvitation?.senderUsername ?: "الخصم",
                            color = TextLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "صديق", color = CosmicCyan, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CosmicSlate)
                Spacer(modifier = Modifier.height(12.dp))

                // Voice status bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isVoiceConnected) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = if (isVoiceConnected) CosmicCyan else TextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isVoiceConnected) "متصل بالقناة الصوتية 🎙️" else "القناة الصوتية: صامت",
                            color = if (isVoiceConnected) CosmicCyan else TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Toggle voice
                    Button(
                        onClick = { viewModel.toggleMicrophone() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMicMuted) CosmicSlate else CosmicCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("toggle_mic_button")
                    ) {
                        Text(
                            text = if (isMicMuted) "تفعيل المايك" else "كتم المايك",
                            color = if (isMicMuted) TextLight else Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pre-match Lobby Chat Panel
        Text(text = "دردشة ما قبل المباراة 💬", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    reverseLayout = true
                ) {
                    val reversedChats = matchChats.reversed()
                    items(reversedChats) { chat ->
                        val isMe = chat.senderId == profile?.id
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) CosmicCyan else CosmicSlate
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isMe) 12.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 12.dp
                                ),
                                modifier = Modifier.widthIn(max = 240.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = chat.senderUsername,
                                        color = if (isMe) Color.Black.copy(alpha = 0.8f) else CosmicCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = chat.message,
                                        color = if (isMe) Color.Black else TextLight,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input message bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = typedMessage,
                        onValueChange = { typedMessage = it },
                        placeholder = { Text("اكتب رسالة...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CosmicSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lobby_chat_input")
                    )

                    IconButton(
                        onClick = {
                            if (typedMessage.trim().isNotEmpty()) {
                                viewModel.sendLobbyChatMessage(typedMessage)
                                typedMessage = ""
                            }
                        },
                        modifier = Modifier
                            .background(CosmicCyan, CircleShape)
                            .size(48.dp)
                            .testTag("lobby_chat_send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start Match trigger
        if (lobbyMatch != null) {
            Button(
                onClick = { viewModel.startFriendsMatch() },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_match_now_button")
            ) {
                Text("ابدأ المباراة الآن ⚔️", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(CosmicSlate, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "بانتظار موافقة الخصم وبدء الجلسة...",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewUserProfileScreen(viewModel: GameViewModel) {
    val profile by viewModel.selectedUserProfile.collectAsStateWithLifecycle()

    if (profile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CosmicCyan)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.SearchUsers) },
                modifier = Modifier.background(CosmicSlate, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextLight)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "الملف الشخصي للاعب",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = profile!!.avatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${profile!!.username}",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(3.dp, CosmicCyan, CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Username
        Text(
            text = profile!!.username,
            color = TextLight,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        // Status Indicator
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (profile!!.status == "online") Color.Green else Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (profile!!.status == "online") "🟢 متصل الآن" else formatLastSeen(profile!!.lastSeenAt),
                color = if (profile!!.status == "online") Color.Green else TextMuted,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Unique Account Number or ID
        Text(
            text = "معرف اللاعب: #${profile!!.accountNumber ?: profile!!.id.substring(0, 8)}",
            color = CosmicCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(CosmicSlate, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Level details card
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المستوى الحالي", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("المستوى ${profile!!.level}", color = CosmicGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val progress = getLevelProgress(profile!!.points)
                LinearProgressIndicator(
                    progress = { progress },
                    color = CosmicCyan,
                    trackColor = CosmicSlate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("النقاط: ${profile!!.points}", color = TextMuted, fontSize = 12.sp)
                    Text("متبقي ${(getNextLevelPoints(profile!!.points) - profile!!.points)} نقطة للمستوى التالي", color = CosmicCyan, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game Stats Header
        Text(
            text = "إحصائيات اللعب",
            color = TextLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            textAlign = TextAlign.Right
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stats grid
        val isH7 = profile!!.username == "h.7"
        val totalMatches = if (isH7) 0 else (profile!!.wins + profile!!.losses)
        val winRate = if (totalMatches > 0) (profile!!.wins * 100 / totalMatches) else 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "الفوز",
                value = if (isH7) "-" else "${profile!!.wins}",
                color = Color.Green,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "الخسارة",
                value = if (isH7) "-" else "${profile!!.losses}",
                color = CosmicOrange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "مجموع المباريات",
                value = if (isH7) "-" else "$totalMatches",
                color = CosmicCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "نسبة الفوز",
                value = if (isH7) "-" else "$winRate%",
                color = CosmicGold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ==========================================
// Forgot Password Screen
// ==========================================
@Composable
fun ForgotPasswordScreen(viewModel: GameViewModel) {
    val searchQuery by viewModel.forgotPasswordSearchQuery.collectAsStateWithLifecycle()
    val foundProfile by viewModel.forgotPasswordFoundProfile.collectAsStateWithLifecycle()
    val email by viewModel.forgotPasswordEmail.collectAsStateWithLifecycle()
    val otp by viewModel.forgotPasswordOtp.collectAsStateWithLifecycle()
    val newPass by viewModel.forgotPasswordNewPass.collectAsStateWithLifecycle()
    val confirmPass by viewModel.forgotPasswordConfirmPass.collectAsStateWithLifecycle()
    val step by viewModel.forgotPasswordStep.collectAsStateWithLifecycle()

    var isNewPassVisible by remember { mutableStateOf(false) }
    var isConfirmPassVisible by remember { mutableStateOf(false) }

    // Helper to mask email for privacy: e.g. naseerhamza432@gmail.com -> na****2@gmail.com
    val maskedEmail = remember(email) {
        val parts = email.split("@")
        if (parts.size == 2) {
            val name = parts[0]
            val domain = parts[1]
            if (name.length <= 2) {
                name.first() + "***@" + domain
            } else {
                name.substring(0, 2) + "****" + name.takeLast(1) + "@" + domain
            }
        } else {
            email
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Login) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = TextLight
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "أمان",
            tint = CosmicCyan,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "إعادة تعيين كلمة المرور",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight,
            textAlign = TextAlign.Center
        )

        Text(
            text = "استعد حسابك بخطوات بسيطة وآمنة",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = CosmicCyan)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    1 -> {
                        // STEP 1: SEARCH PROFILE & REQUEST RESET
                        Text(
                            text = "الخطوة 1: ابحث عن حسابك",
                            color = CosmicCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.forgotPasswordSearchQuery.value = it },
                            label = { Text("البريد الإلكتروني أو اسم المستخدم", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CosmicCyan) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.searchProfileForReset() },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("البحث عن الحساب", color = CosmicBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        // Display Found Account Card
                        if (foundProfile != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CosmicSlate.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CosmicCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Avatar
                                    AsyncImage(
                                        model = foundProfile!!.avatarUrl ?: "https://api.dicebear.com/7.x/bottts/svg?seed=${foundProfile!!.username}",
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, CosmicCyan, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Username
                                    Text(
                                        text = foundProfile!!.username,
                                        color = TextLight,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Sequential Account Number / ID
                                    Text(
                                        text = "رقم الحساب (ID): #${foundProfile!!.accountNumber}",
                                        color = CosmicCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Masked Email for Privacy
                                    val profileMaskedEmail = remember(foundProfile!!.email) {
                                        val pEmail = foundProfile!!.email
                                        val parts = pEmail.split("@")
                                        if (parts.size == 2) {
                                            val name = parts[0]
                                            val domain = parts[1]
                                            if (name.length <= 2) {
                                                name.first() + "***@" + domain
                                            } else {
                                                name.substring(0, 2) + "****" + name.takeLast(1) + "@" + domain
                                            }
                                        } else {
                                            pEmail
                                        }
                                    }

                                    Text(
                                        text = "البريد الإلكتروني: $profileMaskedEmail",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { viewModel.requestPasswordReset() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CosmicGold),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("إرسال رمز التحقق", color = CosmicBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Text(
                                        text = "سيتم إرسال رمز تحقق (OTP) إلى بريدك الإلكتروني المسجل لحماية خصوصية حسابك.",
                                        color = TextMuted.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // STEP 2: ENTER OTP
                        Text(
                            text = "الخطوة 2: أدخل رمز التحقق",
                            color = CosmicCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )

                        Text(
                            text = "تم إرسال رمز التحقق (OTP) بنجاح إلى البريد الإلكتروني المرتبط بحسابك ($maskedEmail). يرجى التحقق من صندوق الوارد وإدخاله أدناه للمتابعة.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = otp,
                            onValueChange = { viewModel.forgotPasswordOtp.value = it },
                            label = { Text("رمز التحقق (OTP)", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = CosmicCyan) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.verifyPasswordResetOtp() },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("التحقق من الرمز", color = CosmicBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = { viewModel.forgotPasswordStep.value = 1 }) {
                            Text("الرجوع لإعادة البحث عن الحساب", color = CosmicCyan, fontSize = 13.sp)
                        }
                    }
                    3 -> {
                        // STEP 3: NEW PASSWORD
                        Text(
                            text = "الخطوة 3: تعيين كلمة المرور الجديدة",
                            color = CosmicCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { viewModel.forgotPasswordNewPass.value = it },
                            label = { Text("كلمة المرور الجديدة", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CosmicCyan) },
                            trailingIcon = {
                                IconButton(onClick = { isNewPassVisible = !isNewPassVisible }) {
                                    Icon(
                                        imageVector = if (isNewPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = CosmicCyan
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            visualTransformation = if (isNewPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { viewModel.forgotPasswordConfirmPass.value = it },
                            label = { Text("تأكيد كلمة المرور الجديدة", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CosmicCyan) },
                            trailingIcon = {
                                IconButton(onClick = { isConfirmPassVisible = !isConfirmPassVisible }) {
                                    Icon(
                                        imageVector = if (isConfirmPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = CosmicCyan
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicCyan,
                                unfocusedBorderColor = CosmicSlate,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            visualTransformation = if (isConfirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.submitNewPassword() },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تغيير كلمة المرور", color = CosmicBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Privacy and Security Screen
// ==========================================
@Composable
fun PrivacySecurityScreen(viewModel: GameViewModel) {
    val profile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val oldPass by viewModel.privacyOldPassword.collectAsStateWithLifecycle()
    val newPass by viewModel.privacyNewPassword.collectAsStateWithLifecycle()
    val confirmNewPass by viewModel.privacyConfirmNewPassword.collectAsStateWithLifecycle()

    val newEmail by viewModel.privacyNewEmail.collectAsStateWithLifecycle()
    val emailOtp by viewModel.privacyEmailOtp.collectAsStateWithLifecycle()
    val emailStep by viewModel.privacyEmailStep.collectAsStateWithLifecycle()

    val sessions by viewModel.activeUserSessions.collectAsStateWithLifecycle()
    val twoFactorEnabled by viewModel.privacyTwoFactorEnabled.collectAsStateWithLifecycle()

    var isOldPassVisible by remember { mutableStateOf(false) }
    var isNewPassVisible by remember { mutableStateOf(false) }
    var isConfirmPassVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Profile) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = TextLight
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "الخصوصية والأمان",
                color = TextLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: CHANGE PASSWORD CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CosmicCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "تغيير كلمة المرور",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { viewModel.privacyOldPassword.value = it },
                    label = { Text("كلمة المرور الحالية", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = CosmicCyan) },
                    trailingIcon = {
                        IconButton(onClick = { isOldPassVisible = !isOldPassVisible }) {
                            Icon(
                                imageVector = if (isOldPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = CosmicCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = CosmicSlate,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    visualTransformation = if (isOldPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPass,
                    onValueChange = { viewModel.privacyNewPassword.value = it },
                    label = { Text("كلمة المرور الجديدة", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CosmicCyan) },
                    trailingIcon = {
                        IconButton(onClick = { isNewPassVisible = !isNewPassVisible }) {
                            Icon(
                                imageVector = if (isNewPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = CosmicCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = CosmicSlate,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    visualTransformation = if (isNewPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmNewPass,
                    onValueChange = { viewModel.privacyConfirmNewPassword.value = it },
                    label = { Text("تأكيد كلمة المرور الجديدة", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CosmicCyan) },
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPassVisible = !isConfirmPassVisible }) {
                            Icon(
                                imageVector = if (isConfirmPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = CosmicCyan
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicCyan,
                        unfocusedBorderColor = CosmicSlate,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    visualTransformation = if (isConfirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.changePassword() },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ كلمة المرور الجديدة", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // SECTION 2: CHANGE EMAIL CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = CosmicCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "تغيير البريد الإلكتروني",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "البريد الحالي: ${profile?.email ?: "غير متوفر"}",
                    color = CosmicCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (emailStep == 1) {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { viewModel.privacyNewEmail.value = it },
                        label = { Text("البريد الإلكتروني الجديد", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CosmicCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CosmicSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.requestEmailChange() },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إرسال رمز تغيير البريد", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Text(
                        text = "تم إرسال رمز التحقق إلى $newEmail. يرجى إدخاله في الأسفل للتأكيد.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = emailOtp,
                        onValueChange = { viewModel.privacyEmailOtp.value = it },
                        label = { Text("رمز التحقق (OTP)", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = CosmicCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicCyan,
                            unfocusedBorderColor = CosmicSlate,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.confirmEmailChange() },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تأكيد تغيير البريد", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { viewModel.privacyEmailStep.value = 1 }) {
                        Text("إلغاء والرجوع", color = CosmicCyan, fontSize = 12.sp)
                    }
                }
            }
        }

        // SECTION 3: TWO-FACTOR AUTH
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = CosmicCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "المصادقة الثنائية (2FA)",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Switch(
                        checked = twoFactorEnabled,
                        onCheckedChange = { viewModel.toggleTwoFactor(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CosmicBackground,
                            checkedTrackColor = CosmicCyan,
                            uncheckedThumbColor = CosmicSlate,
                            uncheckedTrackColor = CosmicCard
                        )
                    )
                }
                Text(
                    text = "قم بزيادة حماية حسابك من خلال تفعيل المصادقة الثنائية التي تطلب رمز تحقق إضافي عند الدخول.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // SECTION 4: LOGIN INFO AND SESSION DEVICES CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Devices, contentDescription = null, tint = CosmicCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "الأجهزة والجلسات النشطة",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "آخر وقت لتسجيل الدخول: ${formatLastSeen(profile?.lastSeenAt)}",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                HorizontalDivider(color = CosmicSlate)

                Spacer(modifier = Modifier.height(12.dp))

                if (sessions.isEmpty()) {
                    Text("لا توجد جلسات نشطة أخرى معروضة حالياً.", color = TextMuted, fontSize = 13.sp)
                } else {
                    sessions.forEach { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(session.deviceInfo, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("عنوان IP: ${session.ipAddress ?: "غير معروف"}", color = TextMuted, fontSize = 11.sp)
                            }
                            Text(
                                text = "نشط",
                                color = GlowGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(GlowGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.terminateAllSessions() },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = TextLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج من جميع الأجهزة", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

