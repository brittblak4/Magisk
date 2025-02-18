package com.topjohnwu.magisk.core

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.*
import com.topjohnwu.magisk.BuildConfig
import com.topjohnwu.magisk.data.repository.NetworkService
import com.topjohnwu.magisk.ktx.inject
import com.topjohnwu.magisk.view.Notifications
import org.koin.core.component.KoinComponent
import java.util.concurrent.TimeUnit

class UpdateCheckService(context: Context, workerParams: WorkerParameters)
    : CoroutineWorker(context, workerParams), KoinComponent {

    private val svc: NetworkService by inject()

    override suspend fun doWork(): Result {
        return svc.fetchUpdate()?.run {
            if (Info.env.isActive && BuildConfig.VERSION_CODE < magisk.versionCode)
                Notifications.managerUpdate(applicationContext)
            Result.success()
        } ?: Result.failure()
    }

    companion object {
        @SuppressLint("NewApi")
        fun schedule(context: Context) {
            if (Config.checkUpdate) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresDeviceIdle(true)
                    .build()
                val request = PeriodicWorkRequestBuilder<UpdateCheckService>(12, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    Const.ID.CHECK_MAGISK_UPDATE_WORKER_ID,
                    ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                WorkManager.getInstance(context)
                    .cancelUniqueWork(Const.ID.CHECK_MAGISK_UPDATE_WORKER_ID)
            }
        }
    }
}
