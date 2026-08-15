package com.example.data.api

import com.example.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface SupabaseService {

    @POST("rpc/verify_user_login")
    suspend fun verifyUserLogin(
        @Body request: LoginRequest
    ): Response<List<Profile>>

    @POST("rpc/register_user")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<String>

    @POST("rpc/create_game_challenge")
    suspend fun createChallenge(
        @Body request: CreateChallengeRequest
    ): Response<String>

    @POST("rpc/accept_game_challenge")
    suspend fun acceptChallenge(
        @Body request: AcceptChallengeRequest
    ): Response<String>

    @POST("rpc/heartbeat")
    suspend fun heartbeat(
        @Body request: HeartbeatRequest
    ): Response<Unit>

    @POST("rpc/cancel_my_open_challenges")
    suspend fun cancelMyOpenChallenges(
        @Body request: CancelChallengesRequest
    ): Response<Unit>

    @POST("rpc/submit_match_move")
    suspend fun submitMatchMove(
        @Body request: SubmitMoveRequest
    ): Response<SubmitMoveResponse>

    @POST("rpc/request_deposit")
    suspend fun requestDeposit(
        @Body request: DepositRequest
    ): Response<String>

    @POST("rpc/request_withdrawal")
    suspend fun requestWithdrawal(
        @Body request: WithdrawalRequest
    ): Response<String>

    @POST("rpc/manage_pending_transaction")
    suspend fun managePendingTransaction(
        @Body request: Map<String, String>
    ): Response<Boolean>

    @POST("rpc/perform_matchmaking")
    suspend fun performMatchmaking(
        @Body request: MatchmakingRequest
    ): Response<MatchmakingResponse>

    @POST("rpc/create_computer_match")
    suspend fun createComputerMatch(
        @Body request: CreateComputerMatchRequest
    ): Response<String>

    @POST("rpc/execute_h7_auto_match")
    suspend fun executeH7AutoMatch(
        @Body body: Map<String, String> = emptyMap()
    ): Response<Unit>

    @GET("challenges")
    suspend fun getChallenges(
        @Query("status") status: String? = null,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<Challenge>>

    @GET("matches")
    suspend fun getMatch(
        @Query("id") matchId: String
    ): Response<List<Match>>

    @GET("transactions")
    suspend fun getTransactions(
        @Query("user_id") userId: String? = null,
        @Query("status") status: String? = null,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<Transaction>>

    @GET("profiles")
    suspend fun getProfile(
        @Query("id") userId: String
    ): Response<List<Profile>>

    @GET("profiles")
    suspend fun getProfileByUsername(
        @Query("username") username: String
    ): Response<List<Profile>>

    @GET("matches")
    suspend fun getMatchesByStatus(
        @Query("status") status: String = "eq.playing"
    ): Response<List<Match>>

    @PATCH("profiles")
    suspend fun updateProfile(
        @Query("id") queryId: String,
        @Body updates: Map<String, String>
    ): Response<Unit>

    @PATCH("transactions")
    suspend fun updateTransaction(
        @Query("id") queryId: String,
        @Body updates: Map<String, String>
    ): Response<Unit>

    @POST("notifications")
    suspend fun createNotification(
        @Body fields: Map<String, String>
    ): Response<Unit>

    @GET("notifications")
    suspend fun getNotifications(
        @Query("user_id") userId: String? = null,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<Notification>>

    @PATCH("notifications")
    suspend fun markNotificationRead(
        @Query("id") queryId: String,
        @Body updates: Map<String, Boolean>
    ): Response<Unit>

    @PATCH("notifications")
    suspend fun markAllNotificationsRead(
        @Query("user_id") userId: String,
        @Body updates: Map<String, Boolean>
    ): Response<Unit>

    @GET("monitoring_logs")
    suspend fun getMonitoringLogs(
        @Query("order") order: String = "created_at.desc"
    ): Response<List<MonitoringLog>>

    @POST("rpc/search_users")
    suspend fun searchUsers(
        @Body request: SearchUsersRequest
    ): Response<List<FriendUser>>

    @POST("rpc/send_friend_request")
    suspend fun sendFriendRequest(
        @Body request: SendFriendRequestRequest
    ): Response<String>

    @POST("rpc/cancel_friend_request")
    suspend fun cancelFriendRequest(
        @Body request: CancelFriendRequestRequest
    ): Response<Boolean>

    @POST("rpc/respond_to_friend_request")
    suspend fun respondToFriendRequest(
        @Body request: RespondToFriendRequestRequest
    ): Response<Boolean>

    @POST("rpc/get_my_friends")
    suspend fun getMyFriends(
        @Body request: GetMyFriendsRequest
    ): Response<List<FriendUser>>

    @POST("rpc/get_pending_friend_requests")
    suspend fun getPendingFriendRequests(
        @Body request: GetPendingFriendRequestsRequest
    ): Response<List<PendingFriendRequest>>

    @POST("rpc/get_user_friend_status")
    suspend fun getUserFriendStatus(
        @Body request: GetUserFriendStatusRequest
    ): Response<String>

    @POST("rpc/send_match_invitation")
    suspend fun sendMatchInvitation(
        @Body request: SendMatchInvitationRequest
    ): Response<String>

    @POST("rpc/respond_to_match_invitation")
    suspend fun respondToMatchInvitation(
        @Body request: RespondToMatchInvitationRequest
    ): Response<RespondToMatchInvitationResponse>

    @POST("rpc/send_chat_message")
    suspend fun sendChatMessage(
        @Body request: SendChatMessageRequest
    ): Response<String>

    @POST("rpc/get_match_chats")
    suspend fun getMatchChats(
        @Body request: GetMatchChatsRequest
    ): Response<List<ChatMessage>>

    @POST("rpc/send_webrtc_signal")
    suspend fun sendWebRTCSignal(
        @Body request: SendWebRTCSignalRequest
    ): Response<String>

    @POST("rpc/get_webrtc_signals")
    suspend fun getWebRTCSignals(
        @Body request: GetWebRTCSignalsRequest
    ): Response<List<WebRTCSignal>>

    @GET("match_invitations")
    suspend fun getMatchInvitations(
        @Query("receiver_id") receiverId: String,
        @Query("status") status: String = "eq.pending"
    ): Response<List<MatchInvitation>>

    @POST("rpc/search_profile_for_reset")
    suspend fun searchProfileForReset(
        @Body request: SearchProfileRequest
    ): Response<List<Profile>>

    @POST("rpc/request_password_reset")
    suspend fun requestPasswordReset(
        @Body request: RequestResetRequest
    ): Response<String>

    @POST("/auth/v1/recover")
    suspend fun requestPasswordResetAuth(
        @Body request: AuthRecoverRequest
    ): Response<Unit>

    @POST("/auth/v1/verify")
    suspend fun verifyPasswordResetOtpAuth(
        @Body request: AuthVerifyOtpRequest
    ): Response<AuthVerifyResponse>

    @PUT("/auth/v1/user")
    suspend fun updateUserPasswordAuth(
        @Header("Authorization") token: String,
        @Body request: AuthUpdateUserRequest
    ): Response<Unit>

    @POST("rpc/reset_profile_password_authenticated")
    suspend fun resetProfilePasswordAuthenticated(
        @Header("Authorization") token: String,
        @Body request: ResetProfilePasswordRequest
    ): Response<Boolean>

    @POST("rpc/reset_password_with_otp")
    suspend fun resetPasswordWithOtp(
        @Body request: ResetPasswordRequest
    ): Response<Boolean>

    @POST("rpc/change_user_password")
    suspend fun changeUserPassword(
        @Body request: ChangePasswordRequest
    ): Response<Boolean>

    @POST("rpc/request_email_change")
    suspend fun requestEmailChange(
        @Body request: RequestEmailChangeRequest
    ): Response<String>

    @POST("rpc/confirm_email_change")
    suspend fun confirmEmailChange(
        @Body request: ConfirmEmailChangeRequest
    ): Response<Boolean>

    @POST("rpc/register_user_session")
    suspend fun registerUserSession(
        @Body request: RegisterSessionRequest
    ): Response<String>

    @POST("rpc/terminate_all_user_sessions")
    suspend fun terminateAllUserSessions(
        @Body request: TerminateSessionsRequest
    ): Response<Boolean>

    @GET("user_sessions")
    suspend fun getUserSessions(
        @Query("user_id") userId: String
    ): Response<List<UserSession>>
}
