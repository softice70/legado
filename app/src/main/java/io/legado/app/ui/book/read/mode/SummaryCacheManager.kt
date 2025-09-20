package io.legado.app.ui.book.read.mode

import android.content.Context
import android.content.SharedPreferences
import io.legado.app.data.appDb
import io.legado.app.data.entities.SummaryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 摘要缓存管理器
 * 管理AI生成摘要的本地缓存
 */
class SummaryCacheManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "summary_cache_prefs"
        private const val CACHE_VALIDITY_KEY = "cache_validity_days"
        private const val MAX_CACHE_SIZE_KEY = "max_cache_chapters"
        private const val DEFAULT_VALIDITY_DAYS = 30L
        private const val DEFAULT_MAX_CACHE = 100
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取缓存有效期（天数）
     */
    fun getCacheValidityDays(): Long {
        return prefs.getLong(CACHE_VALIDITY_KEY, DEFAULT_VALIDITY_DAYS)
    }

    /**
     * 设置缓存有效期
     */
    fun setCacheValidityDays(days: Long) {
        prefs.edit().putLong(CACHE_VALIDITY_KEY, days).apply()
    }

    /**
     * 获取最大缓存章节数
     */
    fun getMaxCacheSize(): Int {
        return prefs.getInt(MAX_CACHE_SIZE_KEY, DEFAULT_MAX_CACHE)
    }

    /**
     * 设置最大缓存章节数
     */
    fun setMaxCacheSize(size: Int) {
        prefs.edit().putInt(MAX_CACHE_SIZE_KEY, size).apply()
    }

    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(cacheTime: Long): Boolean {
        val validityMs = TimeUnit.DAYS.toMillis(getCacheValidityDays())
        return System.currentTimeMillis() - cacheTime <= validityMs
    }

    /**
     * 获取缓存的摘要
     */
    suspend fun getCachedSummary(chapterUrl: String): String? {
        return withContext(Dispatchers.IO) {
            val cache = appDb.summaryCacheDao.getByChapterUrl(chapterUrl)
            if (cache != null && isCacheValid(cache.createdAt)) {
                cache.summary
            } else {
                // 清理过期缓存
                if (cache != null) {
                    appDb.summaryCacheDao.delete(cache)
                }
                null
            }
        }
    }

    /**
     * 保存摘要到缓存
     */
    suspend fun saveSummary(chapterUrl: String, bookUrl: String, summaryText: String) {
        withContext(Dispatchers.IO) {
            val cache = SummaryCache.create(
                chapterUrl = chapterUrl,
                bookUrl = bookUrl,
                summaryText = summaryText,
                originalText = ""
            )
            appDb.summaryCacheDao.insert(cache)
            
            // 清理超出限制的缓存
            cleanupOldCache()
        }
    }

    /**
     * 检查是否存在缓存
     */
    suspend fun hasCachedSummary(chapterUrl: String): Boolean {
        return getCachedSummary(chapterUrl) != null
    }

    /**
     * 清理过期缓存
     */
    private suspend fun cleanupOldCache() {
        withContext(Dispatchers.IO) {
            val maxSize = getMaxCacheSize()
            val allCaches = appDb.summaryCacheDao.getAll()
            
            if (allCaches.size > maxSize) {
                // 按创建时间排序，删除最旧的
                val toDelete = allCaches.sortedBy { it.createdAt }
                    .take(allCaches.size - maxSize)
                
                toDelete.forEach { cache ->
                    appDb.summaryCacheDao.delete(cache)
                }
            }
            
            // 清理过期缓存
            val validityMs = TimeUnit.DAYS.toMillis(getCacheValidityDays())
            val expiredCaches = allCaches.filter { cache ->
                !isCacheValid(cache.createdAt)
            }
            
            expiredCaches.forEach { cache ->
                appDb.summaryCacheDao.delete(cache)
            }
        }
    }

    /**
     * 清除所有缓存
     */
    suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            appDb.summaryCacheDao.clearAll()
        }
    }

    /**
     * 获取缓存统计信息
     */
    suspend fun getCacheStats(): CacheStats {
        return withContext(Dispatchers.IO) {
            val allCaches = appDb.summaryCacheDao.getAll()
            val validCaches = allCaches.filter { isCacheValid(it.createdAt) }
            
            CacheStats(
                totalCached = validCaches.size,
                totalSize = validCaches.sumOf { it.summary.length },
                oldestCache = validCaches.minOfOrNull { it.createdAt } ?: 0L,
                newestCache = validCaches.maxOfOrNull { it.createdAt } ?: 0L
            )
        }
    }

    /**
     * 缓存统计信息数据类
     */
    data class CacheStats(
        val totalCached: Int,
        val totalSize: Int,
        val oldestCache: Long,
        val newestCache: Long
    )
}

/**
 * 扩展：SummaryCache实体类
 * 需要在数据库中添加对应的表和DAO
 */
// 已在现有数据库结构中，需要添加：
// 1. SummaryCache实体类
// 2. SummaryCacheDao接口
// 3. 数据库迁移脚本