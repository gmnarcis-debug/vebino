package com.example

import android.app.Application
import com.example.data.crypto.SigningManager
import com.example.data.local.db.WebinoDatabase
import com.example.data.local.prefs.UserPreferencesDataStore
import com.example.data.network.WebsiteInspector
import com.example.data.repository.WebinoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WebinoApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: WebinoDatabase by lazy { WebinoDatabase.getInstance(this) }
    val preferencesDataStore: UserPreferencesDataStore by lazy { UserPreferencesDataStore(this) }
    val signingManager: SigningManager by lazy { SigningManager(this) }
    val websiteInspector: WebsiteInspector by lazy { WebsiteInspector() }
    val repository: WebinoRepository by lazy {
        WebinoRepository(
            context = this,
            db = database,
            prefsStore = preferencesDataStore,
            signingManager = signingManager,
            websiteInspector = websiteInspector,
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.initializeDefaults()
            repository.recoverInterruptedBuilds()
            com.example.buildengine.BuildStorageManager.cleanupOrphanedWorkDirs(this@WebinoApplication)
        }
    }
}
