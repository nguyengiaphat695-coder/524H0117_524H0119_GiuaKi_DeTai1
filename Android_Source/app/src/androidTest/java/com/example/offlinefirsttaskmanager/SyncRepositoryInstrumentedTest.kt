package com.example.offlinefirsttaskmanager

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.offlinefirsttaskmanager.data.local.AppDatabase
import com.example.offlinefirsttaskmanager.data.local.TaskDao
import com.example.offlinefirsttaskmanager.data.local.TaskEntity
import com.example.offlinefirsttaskmanager.data.remote.CreateTaskRequest
import com.example.offlinefirsttaskmanager.data.remote.HealthResponse
import com.example.offlinefirsttaskmanager.data.remote.TaskApi
import com.example.offlinefirsttaskmanager.data.remote.TaskResponse
import com.example.offlinefirsttaskmanager.data.remote.UpdateTaskRequest
import com.example.offlinefirsttaskmanager.data.repository.SyncRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@RunWith(AndroidJUnit4::class)
class SyncRepositoryInstrumentedTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TaskDao
    private lateinit var api: FakeTaskApi
    private lateinit var repository: SyncRepository

    @Before
    fun prepare() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.taskDao()
        api = FakeTaskApi()
        repository = SyncRepository(database, api)
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun pendingCreateIsUploadedAndSecondSyncDoesNotDuplicate() = runBlocking {
        dao.insertTask(localTask("task-01", "Task offline"))

        val first = repository.syncNow()
        val second = repository.syncNow()

        assertEquals(1, first.uploaded)
        assertEquals(1, first.pendingBefore)
        assertEquals(1, first.pendingCompleted)
        assertEquals(0, second.pendingBefore)
        assertTrue(second.pullSucceeded)
        assertEquals(0, second.uploaded)
        assertEquals(1, api.records.size)
        assertEquals("SYNCED", dao.getTaskById("task-01")?.syncStatus)
        assertEquals(1L, dao.getTaskById("task-01")?.serverVersion)
    }

    @Test
    fun downloadUpdatesRoomAndFlow() = runBlocking {
        api.records["task-02"] = serverTask("task-02", "From server", 1L, 2000L)

        val result = repository.syncNow()

        assertEquals(1, result.downloaded)
        assertEquals("From server", dao.getAllTasks().first().single().title)
        assertEquals("SYNCED", dao.getTaskById("task-02")?.syncStatus)
    }

    @Test
    fun networkFailurePreservesLocalTaskForRetry() = runBlocking {
        dao.insertTask(localTask("task-03", "Stay offline"))
        api.online = false

        val first = repository.syncNow()

        assertNotNull(first.errorMessage)
        assertEquals("FAILED", dao.getTaskById("task-03")?.syncStatus)
        assertEquals("Stay offline", dao.getAllTasks().first().single().title)

        api.online = true
        val second = repository.syncNow()

        assertEquals(null, second.errorMessage)
        assertEquals("SYNCED", dao.getTaskById("task-03")?.syncStatus)
        assertEquals(1, api.records.size)
    }

    @Test
    fun serverWinsWhenItsTimestampIsLater() = runBlocking {
        api.records["task-04"] = serverTask("task-04", "Server newer", 2L, 4000L)
        dao.insertTask(localTask("task-04", "Android stale", 3000L, 1L))

        val result = repository.syncNow()

        assertEquals(1, result.conflicts)
        assertEquals(1, result.downloaded)
        assertEquals("Server newer", dao.getTaskById("task-04")?.title)
        assertEquals("SYNCED", dao.getTaskById("task-04")?.syncStatus)
    }

    @Test
    fun localWinsWhenItsTimestampIsLater() = runBlocking {
        api.records["task-05"] = serverTask("task-05", "Server older", 2L, 1000L)
        dao.insertTask(localTask("task-05", "Android newer", 5000L, 1L))

        val result = repository.syncNow()

        assertEquals(1, result.conflicts)
        assertEquals(1, result.uploaded)
        assertEquals("Android newer", api.records["task-05"]?.title)
        assertEquals(3L, dao.getTaskById("task-05")?.serverVersion)
        assertEquals("SYNCED", dao.getTaskById("task-05")?.syncStatus)
    }

    @Test
    fun unsentDeletedTaskIsNotVisibleOrUploaded() = runBlocking {
        dao.insertTask(localTask("task-06", "Never sent").copy(isDeleted = true))

        val result = repository.syncNow()

        assertEquals(null, result.errorMessage)
        assertEquals(0, result.uploaded)
        assertTrue(api.records.isEmpty())
        assertEquals("SYNCED", dao.getTaskById("task-06")?.syncStatus)
        assertTrue(dao.getAllTasks().first().isEmpty())
    }

    @Test
    fun lostCreateResponseFollowedByOfflineDeleteReachesServer() = runBlocking {
        api.records["task-07"] = serverTask("task-07", "Create succeeded remotely", 1L, 2000L)
        dao.insertTask(localTask("task-07", "Deleted while unacknowledged", 3000L)
            .copy(isDeleted = true, serverVersion = 0L))

        val result = repository.syncNow()

        assertEquals(null, result.errorMessage)
        assertEquals(1, result.uploaded)
        assertTrue(api.records["task-07"]!!.isDeleted)
        assertEquals(2L, api.records["task-07"]!!.serverVersion)
        assertTrue(dao.getTaskById("task-07")!!.isDeleted)
        assertEquals("SYNCED", dao.getTaskById("task-07")?.syncStatus)
    }

    @Test
    fun serverTombstoneWinsOverLateLocalEdit() = runBlocking {
        api.records["task-08"] = serverTask("task-08", "Deleted server", 2L, 1000L)
            .copy(isDeleted = true)
        dao.insertTask(localTask("task-08", "Not deleted local", 5000L, 1L))

        val result = repository.syncNow()

        assertEquals(1, result.conflicts)
        assertTrue(dao.getTaskById("task-08")!!.isDeleted)
        assertFalse(dao.getAllTasks().first().any { it.id == "task-08" })
    }

    @Test
    fun remoteTombstoneIsDownloadedAfterPreviousSync() = runBlocking {
        dao.insertTask(localTask("task-09", "Old synced", 1000L, 1L)
            .copy(syncStatus = "SYNCED"))
        api.records["task-09"] = serverTask("task-09", "Deleted", 2L, 2000L)
            .copy(isDeleted = true)

        val result = repository.syncNow()

        assertEquals(1, result.downloaded)
        assertTrue(dao.getTaskById("task-09")!!.isDeleted)
        assertTrue(dao.getAllTasks().first().isEmpty())
    }

    @Test
    fun timeoutFailureLeavesTaskForNextRetry() = runBlocking {
        dao.insertTask(localTask("timeout-01", "Retry after timeout"))
        api.timeoutOnce = true

        val first = repository.syncNow()
        assertNotNull(first.errorMessage)
        assertEquals("FAILED", dao.getTaskById("timeout-01")?.syncStatus)
        assertEquals("Retry after timeout", dao.getTaskById("timeout-01")?.title)

        val second = repository.syncNow()
        assertEquals(null, second.errorMessage)
        assertEquals(1, second.uploaded)
        assertEquals("SYNCED", dao.getTaskById("timeout-01")?.syncStatus)
    }

    @Test
    fun twoIndependentRoomDevicesDetectAndResolveVersionConflict() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val deviceB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val secondRepository = SyncRepository(deviceB, api)
            dao.insertTask(localTask("shared-01", "Initial", 1000L))
            assertEquals(1, repository.syncNow().uploaded)
            assertEquals(1, secondRepository.syncNow().downloaded)

            val firstDeviceTask = dao.getTaskById("shared-01")!!
            dao.updateTask(firstDeviceTask.copy(
                title = "Device A change",
                updatedAt = 7000L,
                syncStatus = "PENDING"
            ))
            assertEquals(1, repository.syncNow().uploaded)

            val secondDeviceTask = deviceB.taskDao().getTaskById("shared-01")!!
            deviceB.taskDao().updateTask(secondDeviceTask.copy(
                title = "Device B change",
                updatedAt = 9000L,
                syncStatus = "PENDING"
            ))
            val resolved = secondRepository.syncNow()
            assertEquals(1, resolved.conflicts)
            assertEquals("Device B change", api.records["shared-01"]?.title)
            assertEquals("SYNCED", deviceB.taskDao().getTaskById("shared-01")?.syncStatus)
            assertEquals(3L, api.records["shared-01"]?.serverVersion)
        } finally {
            deviceB.close()
        }
    }

    @Test
    fun missingServerRecordIsCreatedAgainWithoutDuplicateLocalRow() = runBlocking {
        dao.insertTask(localTask("task-10", "Changed while server was reset", 3000L, 5L))

        val result = repository.syncNow()

        assertEquals(null, result.errorMessage)
        assertEquals(1, result.uploaded)
        assertEquals("Changed while server was reset", api.records["task-10"]?.title)
        assertEquals(1L, dao.getTaskById("task-10")?.serverVersion)
        assertEquals("SYNCED", dao.getTaskById("task-10")?.syncStatus)
        assertEquals(1, dao.getAllTasks().first().size)
    }

    @Test
    fun deletedLocalTaskStaysDeletedAfterServerRecordIsMissing() = runBlocking {
        dao.insertTask(localTask("task-11", "Removed", 3000L, 5L).copy(isDeleted = true))

        val result = repository.syncNow()

        assertEquals(null, result.errorMessage)
        assertEquals(0, result.uploaded)
        assertEquals(0L, dao.getTaskById("task-11")?.serverVersion)
        assertEquals("SYNCED", dao.getTaskById("task-11")?.syncStatus)
        assertTrue(dao.getAllTasks().first().isEmpty())
    }

    @Test
    fun editDuringUploadStaysPendingAndIsUploadedOnNextRun() = runBlocking {
        dao.insertTask(localTask("task-12", "First title", 1000L, 0L))
        api.afterCreate = {
            val original = dao.getTaskById("task-12")!!
            dao.updateTask(original.copy(
                title = "Edited during upload",
                updatedAt = 2000L,
                syncStatus = "PENDING"
            ))
        }

        val first = repository.syncNow()

        assertEquals(1, first.uploaded)
        assertEquals("PENDING", dao.getTaskById("task-12")?.syncStatus)
        assertEquals("Edited during upload", dao.getTaskById("task-12")?.title)
        assertEquals("First title", api.records["task-12"]?.title)

        val second = repository.syncNow()

        assertEquals(null, second.errorMessage)
        assertEquals(1, second.uploaded)
        assertEquals("Edited during upload", api.records["task-12"]?.title)
        assertEquals(2L, dao.getTaskById("task-12")?.serverVersion)
        assertEquals("SYNCED", dao.getTaskById("task-12")?.syncStatus)
    }

    private fun localTask(
        id: String,
        title: String,
        updatedAt: Long = 1000L,
        version: Long = 0L
    ) = TaskEntity(
        id = id,
        title = title,
        updatedAt = updatedAt,
        serverVersion = version,
        syncStatus = "PENDING"
    )

    private fun serverTask(
        id: String,
        title: String,
        version: Long,
        updatedAt: Long
    ) = TaskResponse(
        id = id,
        title = title,
        description = "",
        isCompleted = false,
        isDeleted = false,
        updatedAt = updatedAt,
        serverVersion = version
    )

    private class FakeTaskApi : TaskApi {
        val records = linkedMapOf<String, TaskResponse>()
        var online = true
        var timeoutOnce = false
        var afterCreate: (suspend () -> Unit)? = null
        private var nextServerTime = 6000L

        private fun connected() {
            if (timeoutOnce) {
                timeoutOnce = false
                throw SocketTimeoutException("Simulated timeout")
            }
            if (!online) throw IOException("Offline")
        }

        private fun httpError(code: Int): HttpException {
            return HttpException(Response.error<String>(code, "Error $code".toResponseBody()))
        }

        override suspend fun checkHealth(): HealthResponse {
            connected()
            return HealthResponse("ok")
        }

        override suspend fun getTasks(): List<TaskResponse> {
            connected()
            return records.values.toList()
        }

        override suspend fun getTask(id: String): TaskResponse {
            connected()
            return records[id] ?: throw httpError(404)
        }

        override suspend fun createTask(request: CreateTaskRequest, key: String): TaskResponse {
            connected()
            if (records.containsKey(request.id)) throw httpError(409)
            val response = TaskResponse(
                id = request.id,
                title = request.title,
                description = request.description,
                isCompleted = request.isCompleted,
                isDeleted = false,
                updatedAt = nextServerTime++,
                serverVersion = 1L
            ).also { records[request.id] = it }
            afterCreate?.invoke()
            return response
        }

        override suspend fun updateTask(
            id: String,
            request: UpdateTaskRequest,
            key: String
        ): TaskResponse {
            connected()
            val old = records[id] ?: throw httpError(404)
            if (old.isDeleted || old.serverVersion != request.expectedVersion) {
                throw httpError(409)
            }
            return old.copy(
                title = request.title,
                description = request.description,
                isCompleted = request.isCompleted,
                serverVersion = old.serverVersion + 1L,
                updatedAt = nextServerTime++
            ).also { records[id] = it }
        }

        override suspend fun deleteTask(id: String, version: Long, key: String): TaskResponse {
            connected()
            val old = records[id] ?: throw httpError(404)
            if (old.isDeleted) return old
            if (old.serverVersion != version) throw httpError(409)
            return old.copy(
                isDeleted = true,
                serverVersion = old.serverVersion + 1L,
                updatedAt = nextServerTime++
            ).also { records[id] = it }
        }
    }
}
