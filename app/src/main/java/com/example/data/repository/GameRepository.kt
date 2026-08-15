package com.example.data.repository

import com.example.data.api.SupabaseClient
import com.example.data.database.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response

class GameRepository(private val cacheDao: CacheDao) {

    private val api = SupabaseClient.service

    // Observe local cached data reactively
    val activeProfileFlow: Flow<Profile?> = cacheDao.getActiveProfileFlow().map { cached ->
        cached?.let {
            Profile(
                id = it.id,
                username = it.username,
                email = it.email,
                level = it.level,
                points = it.points,
                balance = it.balance,
                frozenBalance = it.frozenBalance,
                wins = it.wins,
                losses = it.losses,
                winRate = it.winRate,
                avatarUrl = it.avatarUrl,
                status = it.status,
                accountNumber = it.accountNumber,
                longestStreak = it.longestStreak,
                lastMatchAt = it.lastMatchAt
            )
        }
    }

    val cachedChallengesFlow: Flow<List<Challenge>> = cacheDao.getChallengesFlow().map { list ->
        list.map {
            Challenge(
                id = it.id,
                creatorId = it.creatorId,
                creatorUsername = it.creatorUsername,
                creatorLevel = it.creatorLevel,
                creatorAvatarUrl = it.creatorAvatarUrl,
                betAmount = it.betAmount,
                status = it.status,
                opponentId = it.opponentId,
                matchId = it.matchId,
                createdAt = it.createdAt
            )
        }
    }

    val cachedTransactionsFlow: Flow<List<Transaction>> = cacheDao.getTransactionsFlow().map { list ->
        list.map {
            Transaction(
                id = it.id,
                userId = it.userId,
                type = it.type,
                amount = it.amount,
                status = it.status,
                details = it.details,
                createdAt = it.createdAt,
                paymentMethod = it.paymentMethod,
                proofImage = it.proofImage,
                payoutDetails = it.payoutDetails,
                rejectionReason = it.rejectionReason
            )
        }
    }

    // Auth Actions
    suspend fun login(loginStr: String, passwordStr: String): Result<Profile> {
        return try {
            val response = api.verifyUserLogin(LoginRequest(loginStr, passwordStr))
            if (response.isSuccessful) {
                val profiles = response.body()
                if (!profiles.isNullOrEmpty()) {
                    val profile = profiles.first()
                    // Cache locally
                    cacheDao.clearProfile()
                    cacheDao.insertProfile(profile.toCached())
                    Result.success(profile)
                } else {
                    Result.failure(Exception("USER_NOT_FOUND"))
                }
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                if (errBody.contains("USER_NOT_FOUND")) {
                    Result.failure(Exception("USER_NOT_FOUND"))
                } else if (errBody.contains("WRONG_PASSWORD")) {
                    Result.failure(Exception("WRONG_PASSWORD"))
                } else {
                    Result.failure(Exception("خطأ في تسجيل الدخول: ${response.message()} ($errBody)"))
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("USER_NOT_FOUND")) {
                Result.failure(Exception("USER_NOT_FOUND"))
            } else if (msg.contains("WRONG_PASSWORD")) {
                Result.failure(Exception("WRONG_PASSWORD"))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun register(username: String, email: String, passwordStr: String): Result<String> {
        return try {
            val response = api.registerUser(RegisterRequest(username, email, passwordStr))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                if (errBody.contains("USERNAME_ALREADY_EXISTS")) {
                    Result.failure(Exception("USERNAME_ALREADY_EXISTS"))
                } else if (errBody.contains("EMAIL_ALREADY_EXISTS")) {
                    Result.failure(Exception("EMAIL_ALREADY_EXISTS"))
                } else {
                    Result.failure(Exception("خطأ في التسجيل: ${response.message()} ($errBody)"))
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("USERNAME_ALREADY_EXISTS")) {
                Result.failure(Exception("USERNAME_ALREADY_EXISTS"))
            } else if (msg.contains("EMAIL_ALREADY_EXISTS")) {
                Result.failure(Exception("EMAIL_ALREADY_EXISTS"))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun logout(userId: String? = null) {
        if (userId != null) {
            try {
                api.cancelMyOpenChallenges(CancelChallengesRequest(userId))
                api.updateProfile("eq.$userId", mapOf("status" to "offline"))
            } catch (e: Exception) {
                // Ignore network errors when logging out
            }
        }
        cacheDao.clearProfile()
        cacheDao.clearChallenges()
        cacheDao.clearTransactions()
    }

    suspend fun searchProfileForReset(query: String): Result<Profile> {
        return try {
            val response = api.searchProfileForReset(SearchProfileRequest(query))
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                Result.failure(Exception("USER_NOT_FOUND"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestPasswordResetAuth(email: String): Result<Unit> {
        return try {
            val response = api.requestPasswordResetAuth(AuthRecoverRequest(email))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val err = response.errorBody()?.string() ?: ""
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestPasswordReset(email: String): Result<String> {
        return try {
            val response = api.requestPasswordReset(RequestResetRequest(email))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: ""
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPasswordResetOtpAuth(email: String, otp: String): Result<String> {
        return try {
            val response = api.verifyPasswordResetOtpAuth(AuthVerifyOtpRequest(email, otp))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.accessToken)
            } else {
                val err = response.errorBody()?.string() ?: ""
                val lowerErr = err.lowercase()
                when {
                    lowerErr.contains("invalid_grant") || lowerErr.contains("invalid token") || lowerErr.contains("invalid_token") || lowerErr.contains("incorrect") -> 
                        Result.failure(Exception("INVALID_OTP"))
                    lowerErr.contains("expired") -> 
                        Result.failure(Exception("EXPIRED_OTP"))
                    else -> Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPasswordAuth(token: String, newPass: String): Result<Boolean> {
        return try {
            // 1. Update password in auth.users
            val authResponse = api.updateUserPasswordAuth("Bearer $token", AuthUpdateUserRequest(newPass))
            if (!authResponse.isSuccessful) {
                val err = authResponse.errorBody()?.string() ?: ""
                return Result.failure(Exception("Failed to update auth password: $err"))
            }

            // 2. Update password in public.profiles
            val profileResponse = api.resetProfilePasswordAuthenticated("Bearer $token", ResetProfilePasswordRequest(newPass))
            if (profileResponse.isSuccessful && profileResponse.body() == true) {
                Result.success(true)
            } else {
                val err = profileResponse.errorBody()?.string() ?: ""
                Result.failure(Exception("Failed to update profile password: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPasswordWithOtp(email: String, otp: String, newPass: String): Result<Boolean> {
        return try {
            val response = api.resetPasswordWithOtp(ResetPasswordRequest(email, otp, newPass))
            if (response.isSuccessful && response.body() == true) {
                Result.success(true)
            } else {
                val err = response.errorBody()?.string() ?: ""
                if (err.contains("INVALID_OR_EXPIRED_OTP")) {
                    Result.failure(Exception("INVALID_OR_EXPIRED_OTP"))
                } else {
                    Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeUserPassword(userId: String, oldPass: String, newPass: String): Result<Boolean> {
        return try {
            val response = api.changeUserPassword(ChangePasswordRequest(userId, oldPass, newPass))
            if (response.isSuccessful && response.body() == true) {
                Result.success(true)
            } else {
                val err = response.errorBody()?.string() ?: ""
                if (err.contains("WRONG_OLD_PASSWORD")) {
                    Result.failure(Exception("WRONG_OLD_PASSWORD"))
                } else {
                    Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestEmailChange(userId: String, newEmail: String): Result<String> {
        return try {
            val response = api.requestEmailChange(RequestEmailChangeRequest(userId, newEmail))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val err = response.errorBody()?.string() ?: ""
                if (err.contains("EMAIL_ALREADY_EXISTS")) {
                    Result.failure(Exception("EMAIL_ALREADY_EXISTS"))
                } else {
                    Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmEmailChange(userId: String, otp: String): Result<Boolean> {
        return try {
            val response = api.confirmEmailChange(ConfirmEmailChangeRequest(userId, otp))
            if (response.isSuccessful && response.body() == true) {
                Result.success(true)
            } else {
                val err = response.errorBody()?.string() ?: ""
                if (err.contains("INVALID_OR_EXPIRED_OTP")) {
                    Result.failure(Exception("INVALID_OR_EXPIRED_OTP"))
                } else {
                    Result.failure(Exception(err))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUserSession(userId: String, deviceInfo: String, ipAddress: String?): Result<String> {
        return try {
            val response = api.registerUserSession(RegisterSessionRequest(userId, deviceInfo, ipAddress))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error registering session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun terminateAllUserSessions(userId: String): Result<Boolean> {
        return try {
            val response = api.terminateAllUserSessions(TerminateSessionsRequest(userId))
            if (response.isSuccessful && response.body() == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error terminating sessions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSessions(userId: String): Result<List<UserSession>> {
        return try {
            val response = api.getUserSessions("eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error fetching sessions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Sync remote profile to local cache
    suspend fun refreshProfile(userId: String): Result<Profile> {
        return try {
            val response = api.getProfile("eq.$userId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val profile = response.body()!!.first()
                val active = cacheDao.getActiveProfile()
                if (active == null || active.id == userId) {
                    cacheDao.clearProfile()
                    cacheDao.insertProfile(profile.toCached())
                }
                Result.success(profile)
            } else {
                Result.failure(Exception("فشل تحديث الحساب"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProfileByUsername(username: String): Result<Profile> {
        return try {
            val response = api.getProfileByUsername("eq.$username")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                Result.failure(Exception("الحساب غير موجود"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchesByStatus(status: String = "eq.playing"): Result<List<Match>> {
        return try {
            val response = api.getMatchesByStatus(status)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب المباريات المفتوحة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, username: String, avatarUrl: String): Result<Profile> {
        return try {
            val response = api.updateProfile(
                queryId = "eq.$userId",
                updates = mapOf("username" to username, "avatar_url" to avatarUrl)
            )
            if (response.isSuccessful) {
                refreshProfile(userId)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("فشل تعديل الملف الشخصي: ${response.message()} ($errBody)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Challenges Remote Actions
    suspend fun refreshChallenges(): Result<List<Challenge>> {
        return try {
            val response = api.getChallenges("eq.open")
            if (response.isSuccessful && response.body() != null) {
                val challenges = response.body()!!
                cacheDao.clearChallenges()
                cacheDao.insertChallenges(challenges.map { it.toCached() })
                Result.success(challenges)
            } else {
                Result.failure(Exception("فشل تحميل التحديات المفتوحة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChallenge(creatorId: String, betAmount: Double): Result<String> {
        return try {
            val response = api.createChallenge(CreateChallengeRequest(creatorId, betAmount))
            if (response.isSuccessful && response.body() != null) {
                refreshProfile(creatorId)
                refreshChallenges()
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                if (errBody.contains("INSUFFICIENT_BALANCE")) {
                    Result.failure(Exception("INSUFFICIENT_BALANCE"))
                } else {
                    Result.failure(Exception("فشل إنشاء التحدي"))
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("INSUFFICIENT_BALANCE")) {
                Result.failure(Exception("INSUFFICIENT_BALANCE"))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun heartbeat(userId: String): Result<Unit> {
        return try {
            val response = api.heartbeat(HeartbeatRequest(userId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send heartbeat"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelMyOpenChallenges(userId: String): Result<Unit> {
        return try {
            val response = api.cancelMyOpenChallenges(CancelChallengesRequest(userId))
            if (response.isSuccessful) {
                refreshProfile(userId)
                refreshChallenges()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to cancel open challenges"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptChallenge(challengeId: String, opponentId: String): Result<String> {
        return try {
            val response = api.acceptChallenge(AcceptChallengeRequest(challengeId, opponentId))
            if (response.isSuccessful && response.body() != null) {
                refreshProfile(opponentId)
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                if (errBody.contains("INSUFFICIENT_BALANCE")) {
                    Result.failure(Exception("INSUFFICIENT_BALANCE"))
                } else if (errBody.contains("CANNOT_PLAY_SELF")) {
                    Result.failure(Exception("CANNOT_PLAY_SELF"))
                } else {
                    Result.failure(Exception("فشل قبول التحدي"))
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("INSUFFICIENT_BALANCE")) {
                Result.failure(Exception("INSUFFICIENT_BALANCE"))
            } else if (msg.contains("CANNOT_PLAY_SELF")) {
                Result.failure(Exception("CANNOT_PLAY_SELF"))
            } else {
                Result.failure(e)
            }
        }
    }

    // Matchmaking
    suspend fun performMatchmaking(playerId: String, betAmount: Double): Result<MatchmakingResponse> {
        return try {
            val response = api.performMatchmaking(MatchmakingRequest(playerId, betAmount))
            if (response.isSuccessful && response.body() != null) {
                refreshProfile(playerId)
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                if (errBody.contains("INSUFFICIENT_BALANCE")) {
                    Result.failure(Exception("INSUFFICIENT_BALANCE"))
                } else {
                    Result.failure(Exception("فشل البحث عن مباراة"))
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("INSUFFICIENT_BALANCE")) {
                Result.failure(Exception("INSUFFICIENT_BALANCE"))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun createComputerMatch(playerId: String): Result<String> {
        return try {
            val response = api.createComputerMatch(CreateComputerMatchRequest(playerId))
            if (response.isSuccessful && response.body() != null) {
                refreshProfile(playerId)
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception(errBody.ifEmpty { "فشل بدء مباراة ضد الكمبيوتر" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeH7AutoMatch(): Result<Unit> {
        return try {
            val response = api.executeH7AutoMatch()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to execute server-side h7 auto-match"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Game Session Play Actions
    suspend fun getMatch(matchId: String): Result<Match> {
        return try {
            val response = api.getMatch("eq.$matchId")
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                Result.failure(Exception("فشل جلب تفاصيل المباراة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitMove(matchId: String, playerId: String, cellIndex: Int): Result<SubmitMoveResponse> {
        return try {
            val response = api.submitMatchMove(SubmitMoveRequest(matchId, playerId, cellIndex))
            if (response.isSuccessful && response.body() != null) {
                // Also update local profile to reflect points, level, and balance if match completed
                val moveResponse = response.body()!!
                if (moveResponse.status != "playing") {
                    refreshProfile(playerId)
                }
                Result.success(moveResponse)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception(errorBody.ifEmpty { "حركة غير صالحة أو ليس دورك" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Wallet & Transactions
    suspend fun refreshTransactions(userId: String): Result<List<Transaction>> {
        return try {
            val response = api.getTransactions(userId = "eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                cacheDao.clearTransactions()
                cacheDao.insertTransactions(list.map { it.toCached() })
                Result.success(list)
            } else {
                Result.failure(Exception("فشل جلب العمليات المالية"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestDeposit(userId: String, amount: Double, paymentMethod: String, proofImage: String): Result<String> {
        return try {
            val response = api.requestDeposit(DepositRequest(userId, amount, paymentMethod, proofImage))
            if (response.isSuccessful && response.body() != null) {
                refreshTransactions(userId)
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل طلب الشحن"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestWithdrawal(userId: String, amount: Double, paymentMethod: String, payoutDetails: String): Result<String> {
        return try {
            val response = api.requestWithdrawal(WithdrawalRequest(userId, amount, paymentMethod, payoutDetails))
            if (response.isSuccessful && response.body() != null) {
                refreshProfile(userId)
                refreshTransactions(userId)
                Result.success(response.body()!!)
            } else {
                val errBody = response.errorBody()?.string() ?: ""
                if (errBody.contains("INSUFFICIENT_BALANCE")) {
                    Result.failure(Exception("INSUFFICIENT_BALANCE"))
                } else {
                    Result.failure(Exception("فشل طلب السحب"))
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("INSUFFICIENT_BALANCE")) {
                Result.failure(Exception("INSUFFICIENT_BALANCE"))
            } else {
                Result.failure(e)
            }
        }
    }

    // Admin Panel Actions
    suspend fun refreshAllPendingTransactions(): Result<List<Transaction>> {
        return try {
            val response = api.getTransactions(status = "eq.pending")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب الطلبات المعلقة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProfilesByUserIds(userIds: List<String>): Result<List<Profile>> {
        if (userIds.isEmpty()) return Result.success(emptyList())
        return try {
            val filter = "in.(${userIds.distinct().joinToString(",")})"
            val response = api.getProfile(filter)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب الحسابات"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun managePendingTransaction(transactionId: String, action: String, rejectionReason: String? = null): Result<Boolean> {
        return try {
            val body = mutableMapOf<String, String>(
                "p_transaction_id" to transactionId,
                "p_action" to action
            )
            if (rejectionReason != null) {
                body["p_rejection_reason"] = rejectionReason
            }
            val response = api.managePendingTransaction(body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                if (body.containsKey("p_rejection_reason")) {
                    val fallbackBody = mapOf(
                        "p_transaction_id" to transactionId,
                        "p_action" to action
                    )
                    val fallbackRes = api.managePendingTransaction(fallbackBody)
                    if (fallbackRes.isSuccessful && fallbackRes.body() != null) {
                        return Result.success(fallbackRes.body()!!)
                    }
                }
                Result.failure(Exception("فشل تنفيذ الإجراء الإداري"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransactionDetails(transactionId: String, customMessage: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "rejection_reason" to customMessage,
                "details" to customMessage
            )
            val response = api.updateTransaction(queryId = "eq.$transactionId", updates = updates)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("فشل تحديث تفاصيل المعاملة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNotification(userId: String, title: String, message: String, type: String): Result<Unit> {
        return try {
            val fields = mapOf(
                "user_id" to userId,
                "title" to title,
                "message" to message,
                "type" to type
            )
            val response = api.createNotification(fields)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("فشل إضافة الإشعار"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Notifications & Monitoring Logs
    suspend fun fetchNotifications(userId: String): Result<List<Notification>> {
        return try {
            val response = api.getNotifications(userId = "eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب الإشعارات"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(id: String): Result<Unit> {
        return try {
            val response = api.markNotificationRead(id, mapOf("is_read" to true))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("فشل تحديث حالة الإشعار"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(userId: String): Result<Unit> {
        return try {
            val response = api.markAllNotificationsRead("eq.$userId", mapOf("is_read" to true))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("فشل تحديث حالة الإشعارات"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMonitoringLogs(): Result<List<MonitoringLog>> {
        return try {
            val response = api.getMonitoringLogs()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب سجل المراقبة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String, viewerId: String): Result<List<FriendUser>> {
        return try {
            val response = api.searchUsers(SearchUsersRequest(query, viewerId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل البحث عن المستخدمين"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelFriendRequest(senderId: String, receiverId: String): Result<Boolean> {
        return try {
            val response = api.cancelFriendRequest(CancelFriendRequestRequest(senderId, receiverId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل إلغاء طلب الصداقة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(senderId: String, receiverId: String): Result<String> {
        return try {
            val response = api.sendFriendRequest(SendFriendRequestRequest(senderId, receiverId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: ""
                if (errorMsg.contains("FRIENDSHIP_ALREADY_EXISTS_OR_PENDING")) {
                    Result.failure(Exception("يوجد طلب صداقة معلق أو علاقة صداقة قائمة بالفعل!"))
                } else {
                    Result.failure(Exception("فشل إرسال طلب الصداقة: $errorMsg"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(requestId: String, action: String): Result<Boolean> {
        return try {
            val response = api.respondToFriendRequest(RespondToFriendRequestRequest(requestId, action))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل الرد على طلب الصداقة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyFriends(userId: String): Result<List<FriendUser>> {
        return try {
            val response = api.getMyFriends(GetMyFriendsRequest(userId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب قائمة الأصدقاء"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingFriendRequests(userId: String): Result<List<PendingFriendRequest>> {
        return try {
            val response = api.getPendingFriendRequests(GetPendingFriendRequestsRequest(userId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب طلبات الصداقة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFriendStatus(user1Id: String, user2Id: String): Result<String> {
        return try {
            val response = api.getUserFriendStatus(GetUserFriendStatusRequest(user1Id, user2Id))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب حالة الصداقة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMatchInvitation(senderId: String, receiverId: String, betAmount: Double): Result<String> {
        return try {
            val response = api.sendMatchInvitation(SendMatchInvitationRequest(senderId, receiverId, betAmount))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: ""
                Result.failure(Exception("فشل إرسال الدعوة: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToMatchInvitation(invitationId: String, action: String): Result<RespondToMatchInvitationResponse> {
        return try {
            val response = api.respondToMatchInvitation(RespondToMatchInvitationRequest(invitationId, action))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: ""
                if (errorMsg.contains("INSUFFICIENT_BALANCE_SENDER")) {
                    Result.failure(Exception("رصيد صديقك غير كافٍ لبدء المباراة!"))
                } else if (errorMsg.contains("INSUFFICIENT_BALANCE_RECEIVER")) {
                    Result.failure(Exception("رصيدك غير كافٍ لقبول هذه الدعوة!"))
                } else if (errorMsg.contains("SENDER_ALREADY_IN_MATCH") || errorMsg.contains("RECEIVER_ALREADY_IN_MATCH")) {
                    Result.failure(Exception("أحد اللاعبين في مباراة نشطة حالياً!"))
                } else {
                    Result.failure(Exception("فشل الرد على الدعوة: $errorMsg"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(matchId: String, senderId: String, message: String): Result<String> {
        return try {
            val response = api.sendChatMessage(SendChatMessageRequest(matchId, senderId, message))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل إرسال الرسالة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchChats(matchId: String): Result<List<ChatMessage>> {
        return try {
            val response = api.getMatchChats(GetMatchChatsRequest(matchId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب رسائل الدردشة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendWebRTCSignal(matchId: String, senderId: String, signalType: String, payload: String): Result<String> {
        return try {
            val response = api.sendWebRTCSignal(SendWebRTCSignalRequest(matchId, senderId, signalType, payload))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل إرسال إشارة WebRTC"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWebRTCSignals(matchId: String, userId: String): Result<List<WebRTCSignal>> {
        return try {
            val response = api.getWebRTCSignals(GetWebRTCSignalsRequest(matchId, userId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب إشارات WebRTC"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchInvitations(receiverId: String): Result<List<MatchInvitation>> {
        return try {
            val response = api.getMatchInvitations("eq.$receiverId")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("فشل جلب دعوات المباريات"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper mappers
    private fun Profile.toCached() = CachedProfile(
        id = id,
        username = username,
        email = email,
        level = level,
        points = points,
        balance = balance,
        frozenBalance = frozenBalance,
        wins = wins,
        losses = losses,
        winRate = winRate,
        avatarUrl = avatarUrl,
        status = status,
        accountNumber = accountNumber,
        longestStreak = longestStreak,
        lastMatchAt = lastMatchAt,
        lastSeenAt = lastSeenAt
    )

    private fun Challenge.toCached() = CachedChallenge(
        id = id,
        creatorId = creatorId,
        creatorUsername = creatorUsername,
        creatorLevel = creatorLevel,
        creatorAvatarUrl = creatorAvatarUrl,
        betAmount = betAmount,
        status = status,
        opponentId = opponentId,
        matchId = matchId,
        createdAt = createdAt
    )

    private fun Transaction.toCached() = CachedTransaction(
        id = id,
        userId = userId,
        type = type,
        amount = amount,
        status = status,
        details = details,
        createdAt = createdAt,
        paymentMethod = paymentMethod,
        proofImage = proofImage,
        payoutDetails = payoutDetails,
        rejectionReason = rejectionReason
    )
}
