package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.SummaryCache

/**
 * 摘要缓存DAO接口
 * 提供对摘要缓存数据的操作方法
 */
@Dao
interface SummaryCacheDao {

    /**
     * 插入或替换摘要缓存
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: SummaryCache)

    /**
     * 根据章节URL获取摘要缓存
     */
    @Query("SELECT * FROM summaryCache WHERE chapterUrl = :chapterUrl")
    suspend fun getByChapterUrl(chapterUrl: String): SummaryCache?

    /**
     * 获取所有摘要缓存
     */
    @Query("SELECT * FROM summaryCache ORDER BY createdAt DESC")
    suspend fun getAll(): List<SummaryCache>

    /**
     * 根据章节URL删除摘要缓存
     */
    @Query("DELETE FROM summaryCache WHERE chapterUrl = :chapterUrl")
    suspend fun deleteByChapterUrl(chapterUrl: String)

    /**
     * 根据书籍URL删除所有摘要缓存
     */
    @Query("DELETE FROM summaryCache WHERE bookUrl = :bookUrl")
    suspend fun deleteByBookUrl(bookUrl: String)

    /**
     * 根据书籍URL获取摘要缓存列表
     */
    @Query("SELECT * FROM summaryCache WHERE bookUrl = :bookUrl ORDER BY createdAt DESC")
    suspend fun getByBookUrl(bookUrl: String): List<SummaryCache>

    /**
     * 删除指定的摘要缓存
     */
    @Delete
    suspend fun delete(cache: SummaryCache)

    /**
     * 清除所有摘要缓存
     */
    @Query("DELETE FROM summaryCache")
    suspend fun clearAll()

    /**
     * 获取缓存总数
     */
    @Query("SELECT COUNT(*) FROM summaryCache")
    suspend fun getCount(): Int

    /**
     * 获取过期的缓存
     */
    @Query("SELECT * FROM summaryCache WHERE expiresAt < :currentTime")
    suspend fun getExpired(currentTime: Long = System.currentTimeMillis()): List<SummaryCache>

    /**
     * 删除过期的缓存
     */
    @Query("DELETE FROM summaryCache WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis()): Int

    /**
     * 根据创建时间范围获取缓存
     */
    @Query("SELECT * FROM summaryCache WHERE createdAt BETWEEN :startTime AND :endTime")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<SummaryCache>

    /**
     * 批量插入摘要缓存
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<SummaryCache>)

    /**
     * 获取缓存统计信息
     */
    @Query("""
        SELECT 
            COUNT(*) as count,
            AVG(summaryRatio) as avgRatio,
            MIN(createdAt) as oldestTime,
            MAX(createdAt) as newestTime,
            COUNT(CASE WHEN expiresAt > :currentTime THEN 1 END) as validCount
        FROM summaryCache
    """)
    suspend fun getCacheStats(currentTime: Long = System.currentTimeMillis()): CacheStats?

    /**
     * 缓存统计结果数据类
     */
    data class CacheStats(
        val count: Int,
        val avgRatio: Float,
        val oldestTime: Long,
        val newestTime: Long,
        val validCount: Int
    )
}