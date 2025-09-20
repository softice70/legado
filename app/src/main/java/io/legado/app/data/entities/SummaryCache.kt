package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * 摘要缓存实体
 * 存储AI生成的章节摘要
 */
@Entity(tableName = "summaryCache")
data class SummaryCache(
    @PrimaryKey
    val chapterUrl: String,          // 章节URL作为唯一标识
    val bookUrl: String,             // 书籍URL
    val summary: String,             // 摘要内容
    val summaryRatio: Float = 0.3f,  // 摘要比例
    val createdAt: Long = System.currentTimeMillis(),  // 创建时间
    val expiresAt: Long = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L  // 过期时间（默认30天）
) {
    
    /**
     * 检查是否过期
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    /**
     * 获取缓存年龄（天）
     */
    fun getCacheAgeDays(): Long {
        return (System.currentTimeMillis() - createdAt) / (24 * 60 * 60 * 1000L)
    }

    /**
     * 获取剩余有效天数
     */
    fun getRemainingDays(): Long {
        val remaining = expiresAt - System.currentTimeMillis()
        return if (remaining > 0) remaining / (24 * 60 * 60 * 1000L) else 0
    }

    companion object {
        /**
         * 创建新的缓存实例
         */
        fun create(
            chapterUrl: String,
            bookUrl: String,
            summaryText: String,
            originalText: String,
            validityDays: Long = 30
        ): SummaryCache {
            val now = System.currentTimeMillis()
            val expiresAt = now + validityDays * 24 * 60 * 60 * 1000L
            val ratio = if (originalText.isNotEmpty()) {
                summaryText.length.toFloat() / originalText.length.toFloat()
            } else {
                0.3f
            }
            
            return SummaryCache(
                chapterUrl = chapterUrl,
                bookUrl = bookUrl,
                summary = summaryText,
                summaryRatio = ratio,
                createdAt = now,
                expiresAt = expiresAt
            )
        }
    }
}