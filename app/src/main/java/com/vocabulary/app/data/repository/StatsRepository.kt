package com.vocabulary.app.data.repository

import com.vocabulary.app.data.local.dao.ActivityDao
import com.vocabulary.app.data.local.dao.ProgressDao
import com.vocabulary.app.data.local.entity.DailyActivityEntity
import com.vocabulary.app.data.local.entity.ProgressEntity
import com.vocabulary.app.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val progressDao: ProgressDao,
    preferences: AppPreferences
) {
    val dailyGoal: Flow<Int> = preferences.dailyGoal

    fun observeActivity(): Flow<List<DailyActivityEntity>> = activityDao.observeAll()

    fun observeProgress(): Flow<List<ProgressEntity>> = progressDao.observeAll()
}
