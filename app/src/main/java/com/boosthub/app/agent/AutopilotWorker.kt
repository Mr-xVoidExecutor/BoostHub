package com.boosthub.app.agent

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AutopilotWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Autopilot.runOnce(applicationContext)
        return Result.success()
    }
}

object Scheduler {
    fun apply(ctx: Context, on: Boolean) {
        val wm = WorkManager.getInstance(ctx)
        if (on) {
            val req = PeriodicWorkRequestBuilder<AutopilotWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            wm.enqueueUniquePeriodicWork("autopilot", ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork("autopilot")
        }
    }
}
