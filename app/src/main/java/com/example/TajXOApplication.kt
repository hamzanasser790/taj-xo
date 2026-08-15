package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.GameRepository

class TajXOApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { GameRepository(database.cacheDao()) }
}
