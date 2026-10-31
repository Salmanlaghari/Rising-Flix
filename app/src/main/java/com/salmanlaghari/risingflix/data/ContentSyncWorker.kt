package com.salmanlaghari.risingflix.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ContentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val apiService = Retrofit.Builder()
                .baseUrl("https://raw.githubusercontent.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
            val repository = ContentRepository(apiService, applicationContext)
            if (repository.syncContent()) Result.success() else Result.retry()
        } catch (error: Exception) {
            Result.retry()
        }
    }
}

/** Installs app-native sync work once; WorkManager persists it across restarts and reboots. */
object ContentSyncScheduler {
    private const val PERIODIC_WORK_NAME = "risingflix-catalog-periodic-sync"
    private const val INITIAL_WORK_NAME = "risingflix-catalog-initial-sync"
    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val periodicRequest = PeriodicWorkRequestBuilder<ContentSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        val initialRequest = OneTimeWorkRequestBuilder<ContentSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            INITIAL_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            initialRequest
        )
    }
}
