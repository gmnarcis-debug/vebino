package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Transaction
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<FullProjectBundle>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProject(projectId: Long): Flow<FullProjectBundle?>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): FullProjectBundle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjectSettings(settings: ProjectSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWebSettings(webSettings: WebSettingsEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    @Transaction
    @Query(
        "SELECT * FROM projects WHERE name LIKE '%' || :query || '%' " +
            "OR websiteUrl LIKE '%' || :query || '%' " +
            "OR packageName LIKE '%' || :query || '%' ORDER BY updatedAt DESC"
    )
    fun searchProjects(query: String): Flow<List<FullProjectBundle>>

    @Query("SELECT COUNT(*) FROM projects WHERE packageName = :packageName AND id != :excludeProjectId")
    suspend fun countByPackageName(packageName: String, excludeProjectId: Long = 0L): Int

    @Query("SELECT COUNT(*) FROM projects")
    fun observeProjectCount(): Flow<Int>
}

@Dao
interface BuildDao {
    @Query("SELECT * FROM build_tasks ORDER BY startedAt DESC LIMIT 1")
    fun observeLatestBuildTask(): Flow<BuildTaskEntity?>

    @Query("SELECT * FROM build_tasks ORDER BY startedAt DESC LIMIT 10")
    fun observeRecentBuildTasks(): Flow<List<BuildTaskEntity>>

    @Query("SELECT * FROM build_tasks ORDER BY startedAt DESC LIMIT 200")
    fun observeAllBuildTasks(): Flow<List<BuildTaskEntity>>

    @Query("SELECT * FROM build_logs WHERE taskId = :taskId ORDER BY id ASC")
    suspend fun getBuildLogs(taskId: Long): List<BuildLogEntity>

    @Query("SELECT * FROM build_tasks WHERE id = :taskId")
    fun observeBuildTask(taskId: Long): Flow<BuildTaskEntity?>

    @Query("SELECT * FROM build_tasks WHERE id = :taskId")
    suspend fun getBuildTask(taskId: Long): BuildTaskEntity?

    @Query("SELECT * FROM build_tasks WHERE status IN ('QUEUED', 'RUNNING')")
    suspend fun getActiveBuildTasks(): List<BuildTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildTask(task: BuildTaskEntity): Long

    @Update
    suspend fun updateBuildTask(task: BuildTaskEntity)

    @Query("SELECT * FROM build_logs WHERE taskId = :taskId ORDER BY id ASC")
    fun observeBuildLogs(taskId: Long): Flow<List<BuildLogEntity>>

    @Insert
    suspend fun insertBuildLog(log: BuildLogEntity)

    @Insert
    suspend fun insertBuildLogs(logs: List<BuildLogEntity>)

    @Query("SELECT * FROM build_results ORDER BY createdAt DESC")
    fun observeAllBuildResults(): Flow<List<BuildResultEntity>>

    @Query("SELECT * FROM build_results WHERE id = :resultId")
    suspend fun getBuildResultById(resultId: Long): BuildResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildResult(result: BuildResultEntity): Long

    @Update
    suspend fun updateBuildResult(result: BuildResultEntity)

    @Query("DELETE FROM build_results WHERE id = :resultId")
    suspend fun deleteBuildResultById(resultId: Long)
}

@Dao
interface SigningDao {
    @Query("SELECT * FROM signing_keys ORDER BY isDefault DESC, createdAt DESC")
    fun observeSigningKeys(): Flow<List<SigningKeyMetadataEntity>>

    @Query("SELECT * FROM signing_keys ORDER BY isDefault DESC, createdAt DESC")
    suspend fun getAllSigningKeys(): List<SigningKeyMetadataEntity>

    @Query("SELECT * FROM signing_keys WHERE id = :id")
    suspend fun getSigningKeyById(id: Long): SigningKeyMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSigningKey(key: SigningKeyMetadataEntity): Long

    @Update
    suspend fun updateSigningKey(key: SigningKeyMetadataEntity)

    @Query("UPDATE signing_keys SET isDefault = 0")
    suspend fun clearDefaultKeys()

    @Query("UPDATE signing_keys SET isDefault = 1 WHERE id = :id")
    suspend fun markAsDefault(id: Long)

    @Query("DELETE FROM signing_keys WHERE id = :id")
    suspend fun deleteSigningKey(id: Long)
}

@Dao
interface TemplateAndHistoryDao {
    @Query("SELECT * FROM app_templates")
    fun observeTemplates(): Flow<List<AppTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<AppTemplateEntity>)

    @Query("SELECT * FROM recent_websites ORDER BY inspectedAt DESC LIMIT 12")
    fun observeRecentWebsites(): Flow<List<RecentWebsiteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentWebsite(website: RecentWebsiteEntity)
}
