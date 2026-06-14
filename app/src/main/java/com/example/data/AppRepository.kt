package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allApps: Flow<List<CreatedApp>> = appDao.getAllApps()

    suspend fun getAppById(id: Int): CreatedApp? = appDao.getAppById(id)

    suspend fun insertApp(app: CreatedApp): Long = appDao.insertApp(app)

    suspend fun updateApp(app: CreatedApp) = appDao.updateApp(app)

    suspend fun deleteApp(app: CreatedApp) = appDao.deleteApp(app)

    suspend fun deleteAppById(id: Int) = appDao.deleteAppById(id)
}
