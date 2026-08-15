package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Profile(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "level") val level: Int = 1,
    @Json(name = "points") val points: Int = 100,
    @Json(name = "balance") val balance: Double = 0.0,
    @Json(name = "frozen_balance") val frozenBalance: Double = 0.0,
    @Json(name = "wins") val wins: Int = 0,
    @Json(name = "losses") val losses: Int = 0,
    @Json(name = "win_rate") val winRate: Double = 0.0,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "status") val status: String = "offline",
    @Json(name = "account_number") val accountNumber: Int? = null,
    @Json(name = "longest_streak") val longestStreak: Int = 0,
    @Json(name = "last_match_at") val lastMatchAt: String? = null,
    @Json(name = "last_seen_at") val lastSeenAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Challenge(
    @Json(name = "id") val id: String,
    @Json(name = "creator_id") val creatorId: String,
    @Json(name = "creator_username") val creatorUsername: String,
    @Json(name = "creator_level") val creatorLevel: Int,
    @Json(name = "creator_avatar_url") val creatorAvatarUrl: String?,
    @Json(name = "bet_amount") val betAmount: Double,
    @Json(name = "status") val status: String,
    @Json(name = "opponent_id") val opponentId: String? = null,
    @Json(name = "match_id") val matchId: String? = null,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class Match(
    @Json(name = "id") val id: String,
    @Json(name = "game_mode") val gameMode: String = "1v1",
    @Json(name = "bet_amount") val betAmount: Double,
    @Json(name = "player1_id") val player1Id: String,
    @Json(name = "player2_id") val player2Id: String,
    @Json(name = "player3_id") val player3Id: String? = null,
    @Json(name = "player4_id") val player4Id: String? = null,
    @Json(name = "board") val board: String = "_________",
    @Json(name = "turn_player_id") val turnPlayerId: String,
    @Json(name = "status") val status: String = "playing",
    @Json(name = "winner_id") val winnerId: String? = null,
    @Json(name = "winner_username") val winnerUsername: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class Transaction(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "type") val type: String, // 'deposit', 'withdrawal', 'match_entry', 'match_win', 'commission'
    @Json(name = "amount") val amount: Double,
    @Json(name = "status") val status: String, // 'pending', 'approved', 'rejected', 'completed'
    @Json(name = "details") val details: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "payment_method") val paymentMethod: String? = null,
    @Json(name = "proof_image") val proofImage: String? = null,
    @Json(name = "payout_details") val payoutDetails: String? = null,
    @Json(name = "rejection_reason") val rejectionReason: String? = null
)

// RPC Request Classes
@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "p_login") val login: String,
    @Json(name = "p_password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "p_username") val username: String,
    @Json(name = "p_email") val email: String,
    @Json(name = "p_password") val password: String
)

@JsonClass(generateAdapter = true)
data class CreateChallengeRequest(
    @Json(name = "p_creator_id") val creatorId: String,
    @Json(name = "p_bet_amount") val betAmount: Double
)

@JsonClass(generateAdapter = true)
data class AcceptChallengeRequest(
    @Json(name = "p_challenge_id") val challengeId: String,
    @Json(name = "p_opponent_id") val opponentId: String
)

@JsonClass(generateAdapter = true)
data class HeartbeatRequest(
    @Json(name = "p_user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class CancelChallengesRequest(
    @Json(name = "p_user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class SubmitMoveRequest(
    @Json(name = "p_match_id") val matchId: String,
    @Json(name = "p_player_id") val playerId: String,
    @Json(name = "p_cell_index") val cellIndex: Int
)

@JsonClass(generateAdapter = true)
data class DepositRequest(
    @Json(name = "p_user_id") val userId: String,
    @Json(name = "p_amount") val amount: Double,
    @Json(name = "p_payment_method") val paymentMethod: String,
    @Json(name = "p_proof_image") val proofImage: String
)

@JsonClass(generateAdapter = true)
data class WithdrawalRequest(
    @Json(name = "p_user_id") val userId: String,
    @Json(name = "p_amount") val amount: Double,
    @Json(name = "p_payment_method") val paymentMethod: String,
    @Json(name = "p_payout_details") val payoutDetails: String
)

@JsonClass(generateAdapter = true)
data class ManageTransactionRequest(
    @Json(name = "p_transaction_id") val transactionId: String,
    @Json(name = "p_action") val action: String, // 'approve' or 'reject'
    @Json(name = "p_rejection_reason") val rejectionReason: String? = null
)

@JsonClass(generateAdapter = true)
data class MatchmakingRequest(
    @Json(name = "p_player_id") val playerId: String,
    @Json(name = "p_bet_amount") val betAmount: Double
)

@JsonClass(generateAdapter = true)
data class CreateComputerMatchRequest(
    @Json(name = "p_player_id") val playerId: String
)

// RPC Response Classes
@JsonClass(generateAdapter = true)
data class MatchmakingResponse(
    @Json(name = "status") val status: String, // 'matched' or 'waiting'
    @Json(name = "match_id") val matchId: String?,
    @Json(name = "challenge_id") val challengeId: String?
)

@JsonClass(generateAdapter = true)
data class SubmitMoveResponse(
    @Json(name = "match_id") val matchId: String,
    @Json(name = "board") val board: String,
    @Json(name = "status") val status: String,
    @Json(name = "turn_player_id") val turnPlayerId: String,
    @Json(name = "winner_id") val winnerId: String?,
    @Json(name = "winner_username") val winnerUsername: String?
)

@JsonClass(generateAdapter = true)
data class Notification(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "type") val type: String, // 'win', 'loss', 'invitation_received', 'invitation_accepted', 'invitation_rejected', 'deposit_success', 'withdrawal_success', 'info'
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class MonitoringLog(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String?,
    @Json(name = "action") val action: String,
    @Json(name = "details") val details: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SearchUsersRequest(
    @Json(name = "p_search_query") val searchQuery: String,
    @Json(name = "p_viewer_id") val viewerId: String
)

@JsonClass(generateAdapter = true)
data class CancelFriendRequestRequest(
    @Json(name = "p_sender_id") val senderId: String,
    @Json(name = "p_receiver_id") val receiverId: String
)

@JsonClass(generateAdapter = true)
data class SendFriendRequestRequest(
    @Json(name = "p_sender_id") val senderId: String,
    @Json(name = "p_receiver_id") val receiverId: String
)

@JsonClass(generateAdapter = true)
data class RespondToFriendRequestRequest(
    @Json(name = "p_request_id") val requestId: String,
    @Json(name = "p_action") val action: String
)

@JsonClass(generateAdapter = true)
data class GetMyFriendsRequest(
    @Json(name = "p_user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class GetPendingFriendRequestsRequest(
    @Json(name = "p_user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class GetUserFriendStatusRequest(
    @Json(name = "p_user1_id") val user1Id: String,
    @Json(name = "p_user2_id") val user2Id: String
)

@JsonClass(generateAdapter = true)
data class SendMatchInvitationRequest(
    @Json(name = "p_sender_id") val senderId: String,
    @Json(name = "p_receiver_id") val receiverId: String,
    @Json(name = "p_bet_amount") val betAmount: Double
)

@JsonClass(generateAdapter = true)
data class RespondToMatchInvitationRequest(
    @Json(name = "p_invitation_id") val invitationId: String,
    @Json(name = "p_action") val action: String
)

@JsonClass(generateAdapter = true)
data class RespondToMatchInvitationResponse(
    @Json(name = "status") val status: String,
    @Json(name = "match_id") val matchId: String?
)

@JsonClass(generateAdapter = true)
data class SendChatMessageRequest(
    @Json(name = "p_match_id") val matchId: String,
    @Json(name = "p_sender_id") val senderId: String,
    @Json(name = "p_message") val message: String
)

@JsonClass(generateAdapter = true)
data class GetMatchChatsRequest(
    @Json(name = "p_match_id") val matchId: String
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    @Json(name = "id") val id: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_username") val senderUsername: String,
    @Json(name = "message") val message: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SendWebRTCSignalRequest(
    @Json(name = "p_match_id") val matchId: String,
    @Json(name = "p_sender_id") val senderId: String,
    @Json(name = "p_signal_type") val signalType: String,
    @Json(name = "p_payload") val payload: String
)

@JsonClass(generateAdapter = true)
data class GetWebRTCSignalsRequest(
    @Json(name = "p_match_id") val matchId: String,
    @Json(name = "p_user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class WebRTCSignal(
    @Json(name = "id") val id: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "signal_type") val signalType: String,
    @Json(name = "payload") val payload: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class FriendUser(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "level") val level: Int = 1,
    @Json(name = "status") val status: String = "offline",
    @Json(name = "request_id") val requestId: String? = null,
    @Json(name = "last_seen_at") val lastSeenAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PendingFriendRequest(
    @Json(name = "request_id") val requestId: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_username") val senderUsername: String,
    @Json(name = "sender_avatar_url") val senderAvatarUrl: String? = null,
    @Json(name = "sender_level") val senderLevel: Int = 1,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class MatchInvitation(
    @Json(name = "id") val id: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_username") val senderUsername: String,
    @Json(name = "receiver_id") val receiverId: String,
    @Json(name = "bet_amount") val betAmount: Double,
    @Json(name = "status") val status: String,
    @Json(name = "match_id") val matchId: String?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class RequestResetRequest(
    @Json(name = "p_email") val email: String
)

@JsonClass(generateAdapter = true)
data class SearchProfileRequest(
    @Json(name = "p_query") val query: String
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    @Json(name = "p_email") val email: String,
    @Json(name = "p_otp") val otp: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    @Json(name = "p_email") val email: String,
    @Json(name = "p_otp") val otp: String,
    @Json(name = "p_new_password") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "p_user_id") val userId: String,
    @Json(name = "p_old_password") val oldPassword: String,
    @Json(name = "p_new_password") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class RequestEmailChangeRequest(
    @Json(name = "p_user_id") val userId: String,
    @Json(name = "p_new_email") val newEmail: String
)

@JsonClass(generateAdapter = true)
data class ConfirmEmailChangeRequest(
    @Json(name = "p_user_id") val userId: String,
    @Json(name = "p_otp") val otp: String
)

@JsonClass(generateAdapter = true)
data class RegisterSessionRequest(
    @Json(name = "p_user_id") val userId: String,
    @Json(name = "p_device_info") val deviceInfo: String,
    @Json(name = "p_ip_address") val ipAddress: String?
)

@JsonClass(generateAdapter = true)
data class TerminateSessionsRequest(
    @Json(name = "p_user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class UserSession(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "device_info") val deviceInfo: String,
    @Json(name = "ip_address") val ipAddress: String?,
    @Json(name = "last_active") val lastActive: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class AuthRecoverRequest(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class AuthVerifyOtpRequest(
    @Json(name = "email") val email: String,
    @Json(name = "token") val token: String,
    @Json(name = "type") val type: String = "recovery"
)

@JsonClass(generateAdapter = true)
data class AuthVerifyResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Long,
    @Json(name = "refresh_token") val refreshToken: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthUpdateUserRequest(
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class ResetProfilePasswordRequest(
    @Json(name = "p_new_password") val newPassword: String
)



