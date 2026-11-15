package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        LocalListingEntity::class,
        SwipeEntity::class,
        MatchMessageEntity::class,
        UserSettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CoM8Database : RoomDatabase() {
    abstract fun coM8Dao(): CoM8Dao

    companion object {
        @Volatile
        private var instance: CoM8Database? = null

        fun getInstance(context: Context): CoM8Database {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoM8Database::class.java,
                    "com8_local.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
