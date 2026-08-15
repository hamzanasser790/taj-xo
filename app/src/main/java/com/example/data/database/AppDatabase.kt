package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Entities
@Entity(tableName = "profiles")
data class CachedProfile(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val level: Int,
    val points: Int,
    val balance: Double,
    val frozenBalance: Double,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val avatarUrl: String?,
    val status: String,
    val accountNumber: Int? = null,
    val longestStreak: Int = 0,
    val lastMatchAt: String? = null,
    val lastSeenAt: String? = null
)

@Entity(tableName = "challenges")
data class CachedChallenge(
    @PrimaryKey val id: String,
    val creatorId: String,
    val creatorUsername: String,
    val creatorLevel: Int,
    val creatorAvatarUrl: String?,
    val betAmount: Double,
    val status: String,
    val opponentId: String?,
    val matchId: String?,
    val createdAt: String
)

@Entity(tableName = "transactions")
data class CachedTransaction(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val amount: Double,
    val status: String,
    val details: String,
    val createdAt: String,
    val paymentMethod: String? = null,
    val proofImage: String? = null,
    val payoutDetails: String? = null,
    val rejectionReason: String? = null
)

// 2. DAO
@Dao
interface CacheDao {
    @Query("SELECT * FROM profiles LIMIT 1")
    fun getActiveProfileFlow(): Flow<CachedProfile?>

    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getActiveProfile(): CachedProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CachedProfile)

    @Query("DELETE FROM profiles")
    suspend fun clearProfile()

    @Query("SELECT * FROM challenges ORDER BY createdAt DESC")
    fun getChallengesFlow(): Flow<List<CachedChallenge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<CachedChallenge>)

    @Query("DELETE FROM challenges")
    suspend fun clearChallenges()

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getTransactionsFlow(): Flow<List<CachedTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<CachedTransaction>)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()
}

// 3. Room Database
@Database(
    entities = [CachedProfile::class, CachedChallenge::class, CachedTransaction::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tajxo_cache_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
