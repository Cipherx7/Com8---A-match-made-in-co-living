package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CoM8Dao {
    @Query("SELECT * FROM user_profiles WHERE phoneNumber = :phone LIMIT 1")
    fun observeUserProfile(phone: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getUserProfileOnce(phone: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM local_listings ORDER BY createdAt DESC")
    fun observeLocalListings(): Flow<List<LocalListingEntity>>

    @Query("SELECT * FROM local_listings WHERE ownerPhone = :ownerPhone LIMIT 1")
    fun observeOwnListing(ownerPhone: String): Flow<LocalListingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListing(listing: LocalListingEntity)

    @Query("DELETE FROM local_listings WHERE ownerPhone = :ownerPhone")
    suspend fun deleteOwnListing(ownerPhone: String)

    @Query("SELECT * FROM swipes WHERE phoneNumber = :phone ORDER BY updatedAt DESC")
    fun observeSwipes(phone: String): Flow<List<SwipeEntity>>

    @Query("SELECT * FROM swipes WHERE phoneNumber = :phone AND listingId = :listingId LIMIT 1")
    suspend fun getSwipeOnce(phone: String, listingId: String): SwipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSwipe(swipe: SwipeEntity)

    @Query("UPDATE swipes SET unreadCount = 0 WHERE phoneNumber = :phone AND listingId = :listingId")
    suspend fun clearUnread(phone: String, listingId: String)

    @Query("DELETE FROM swipes WHERE phoneNumber = :phone AND listingId = :listingId")
    suspend fun deleteSwipe(phone: String, listingId: String)

    @Query("DELETE FROM swipes WHERE phoneNumber = :phone AND decision = 'PASSED'")
    suspend fun deletePassedSwipes(phone: String)

    @Query("SELECT * FROM match_messages WHERE phoneNumber = :phone AND listingId = :listingId ORDER BY createdAt ASC")
    fun observeMessages(phone: String, listingId: String): Flow<List<MatchMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MatchMessageEntity): Long

    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun observeUserSettings(userId: String): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getUserSettingsOnce(userId: String): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserSettings(settings: UserSettingsEntity)
}
