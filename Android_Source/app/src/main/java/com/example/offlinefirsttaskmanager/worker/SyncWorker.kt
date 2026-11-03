
package com.example.offlinefirsttaskmanager.worker

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.offlinefirsttaskmanager.data.local.AppDatabase
import com.example.offlinefirsttaskmanager.data.remote.RetrofitClient
import com.example.offlinefirsttaskmanager.data.repository.SyncRepository
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.Locale

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val startTime = SystemClock.elapsedRealtime()

        return try {
            val database = AppDatabase.getDatabase(applicationContext)

            val repository = SyncRepository(
                database,
                RetrofitClient.api
            )

            val result = repository.syncNow()

            val durationMs =
                SystemClock.elapsedRealtime() - startTime

            val durationSeconds = String.format(
                Locale.US,
                "%.3f",
                durationMs / 1000.0
            )

            Log.i(
                "SyncMetrics",
                "Mode=Automatic, " +
                        "Uploaded=${result.uploaded}, " +
                        "Downloaded=${result.downloaded}, " +
                        "Conflicts=${result.conflicts}, " +
                        "LocalCompleted=${result.pendingCompleted}/${result.pendingBefore}, " +
                        "PullSucceeded=${result.pullSucceeded}, " +
                        "Duration=$durationSeconds seconds, " +
                        "Error=${result.errorMessage ?: "None"}"
            )

            if (result.errorMessage != null) {
                Result.retry()
            } else {
                Result.success()
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: IOException) {
            Log.e(
                "SyncMetrics",
                "Network error after " +
                        "${SystemClock.elapsedRealtime() - startTime} ms",
                e
            )
            Result.retry()

        } catch (e: Exception) {
            Log.e(
                "SyncMetrics",
                "Sync failed after " +
                        "${SystemClock.elapsedRealtime() - startTime} ms",
                e
            )
            Result.failure()
        }
    }
}
