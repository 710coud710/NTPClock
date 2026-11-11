package com.example.ntpclock

import android.app.Application
import android.content.Context
import com.example.ntpclock.admin.AndroidDeviceOwnerManager
import com.example.ntpclock.admin.AndroidSystemClockController
import com.example.ntpclock.admin.DeviceOwnerManager
import com.example.ntpclock.admin.SystemClockController
import com.example.ntpclock.core.network.AndroidNetworkMonitor
import com.example.ntpclock.core.network.NetworkMonitor
import com.example.ntpclock.core.time.ClockProvider
import com.example.ntpclock.core.time.SystemClockProvider
import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.data.local.NtpDatabase
import com.example.ntpclock.data.preferences.NtpSettingsDataStore
import com.example.ntpclock.data.repository.SettingsRepositoryImpl
import com.example.ntpclock.data.repository.SyncHistoryRepositoryImpl
import com.example.ntpclock.domain.model.SyncTriggerSource
import com.example.ntpclock.domain.repository.SettingsRepository
import com.example.ntpclock.domain.repository.SyncHistoryRepository
import com.example.ntpclock.domain.usecase.QueryNtpTimeUseCase
import com.example.ntpclock.domain.usecase.VerifyClockSyncUseCase
import com.example.ntpclock.ntp.NtpClient
import com.example.ntpclock.ntp.NtpSampleSelector
import com.example.ntpclock.ntp.UdpNtpClient
import com.example.ntpclock.sync.NtpSyncCoordinator
import com.example.ntpclock.worker.SyncWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clockProvider: ClockProvider = SystemClockProvider()
    val timeZoneManager: TimeZoneManager = TimeZoneManager()
    val networkMonitor: NetworkMonitor = AndroidNetworkMonitor(appContext)
    val deviceOwnerManager: DeviceOwnerManager = AndroidDeviceOwnerManager(appContext)
    val systemClockController: SystemClockController =
        AndroidSystemClockController(appContext, deviceOwnerManager)

    private val settingsDataStore = NtpSettingsDataStore(appContext)
    private val database = NtpDatabase.create(appContext)

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(settingsDataStore)
    val syncHistoryRepository: SyncHistoryRepository =
        SyncHistoryRepositoryImpl(database.syncHistoryDao())

    val ntpClient: NtpClient = UdpNtpClient(clockProvider)
    val sampleSelector: NtpSampleSelector = NtpSampleSelector(ntpClient)

    private val queryNtpTimeUseCase = QueryNtpTimeUseCase(sampleSelector)
    private val verifyClockSyncUseCase = VerifyClockSyncUseCase(clockProvider)

    val syncCoordinator: NtpSyncCoordinator = NtpSyncCoordinator(
        networkMonitor = networkMonitor,
        deviceOwnerManager = deviceOwnerManager,
        systemClockController = systemClockController,
        queryNtpTimeUseCase = queryNtpTimeUseCase,
        verifyClockSyncUseCase = verifyClockSyncUseCase,
        settingsRepository = settingsRepository,
        historyRepository = syncHistoryRepository,
        clockProvider = clockProvider,
    )

    val syncWorkScheduler: SyncWorkScheduler = SyncWorkScheduler(appContext)
}

class NtpClockApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Keep WorkManager schedule aligned with DataStore settings.
        applicationScope.launch {
            container.settingsRepository.settings
                .distinctUntilChanged { old, new ->
                    old.autoSyncEnabled == new.autoSyncEnabled &&
                        old.syncIntervalMinutes == new.syncIntervalMinutes
                }
                .collect { settings ->
                    if (settings.autoSyncEnabled) {
                        container.syncWorkScheduler.schedulePeriodicSync(settings.syncIntervalMinutes)
                    } else {
                        container.syncWorkScheduler.cancelPeriodicSync()
                    }
                }
        }

        // Trigger a one-time sync when local LAN/Wi-Fi reconnects after being offline.
        applicationScope.launch {
            var wasConnected = container.networkMonitor.isLocalNetworkConnected()
            container.networkMonitor.observeLocalNetworkConnected()
                .drop(1)
                .collect { isConnected ->
                    if (!wasConnected && isConnected) {
                        val settings = container.settingsRepository.getCurrentSettings()
                        if (settings.autoSyncEnabled) {
                            container.syncWorkScheduler.enqueueOneTimeSync(
                                SyncTriggerSource.NETWORK_RECONNECT
                            )
                        }
                    }
                    wasConnected = isConnected
                }
        }
    }
}

