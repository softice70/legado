package io.legado.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.legado.app.data.entities.SummaryCache
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * 摘要缓存数据库测试类
 * 用于验证SummaryCache表和DAO的正确性
 */
@RunWith(AndroidJUnit4::class)
class SummaryDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var summaryCacheDao: SummaryCacheDao

    @Before
    fun setup() {
        // 使用内存数据库进行测试
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        summaryCacheDao = database.summaryCacheDao
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveSummaryCache() = runBlocking {
        // 创建测试数据
        val testChapterUrl = "https://example.com/chapter1"
        val testBookUrl = "https://example.com/book1"
        val testSummary = "这是章节1的摘要内容"
        val currentTime = System.currentTimeMillis()
        val expiresTime = currentTime + 30 * 24 * 60 * 60 * 1000L // 30天后

        val summaryCache = SummaryCache(
            chapterUrl = testChapterUrl,
            bookUrl = testBookUrl,
            summary = testSummary,
            summaryRatio = 0.3f,
            createdAt = currentTime,
            expiresAt = expiresTime
        )

        // 插入数据
        summaryCacheDao.insert(summaryCache)

        // 查询数据
        val retrieved = summaryCacheDao.getByChapterUrl(testChapterUrl)
        
        // 验证数据
        assertNotNull(retrieved)
        assertEquals(testChapterUrl, retrieved?.chapterUrl)
        assertEquals(testBookUrl, retrieved?.bookUrl)
        assertEquals(testSummary, retrieved?.summary)
        assertEquals(0.3f, retrieved?.summaryRatio)
        assertEquals(currentTime, retrieved?.createdAt)
        assertEquals(expiresTime, retrieved?.expiresAt)
    }

    @Test
    fun testGetExpiredSummaries() = runBlocking {
        val currentTime = System.currentTimeMillis()
        
        // 创建过期数据
        val expiredSummary = SummaryCache(
            chapterUrl = "https://example.com/expired",
            bookUrl = "https://example.com/book1",
            summary = "过期的摘要",
            summaryRatio = 0.3f,
            createdAt = currentTime - 31 * 24 * 60 * 60 * 1000L, // 31天前
            expiresAt = currentTime - 24 * 60 * 60 * 1000L // 1天前过期
        )

        // 创建未过期数据
        val validSummary = SummaryCache(
            chapterUrl = "https://example.com/valid",
            bookUrl = "https://example.com/book1",
            summary = "有效的摘要",
            summaryRatio = 0.3f,
            createdAt = currentTime,
            expiresAt = currentTime + 30 * 24 * 60 * 60 * 1000L // 30天后过期
        )

        summaryCacheDao.insert(expiredSummary)
        summaryCacheDao.insert(validSummary)

        // 查询过期数据
        val expiredSummaries = summaryCacheDao.getExpired(currentTime)
        
        // 验证只返回过期的数据
        assertEquals(1, expiredSummaries.size)
        assertEquals("https://example.com/expired", expiredSummaries[0].chapterUrl)
    }

    @Test
    fun testDeleteByBookUrl() = runBlocking {
        val bookUrl = "https://example.com/book1"
        
        // 创建同一本书的多个章节摘要
        val summary1 = SummaryCache(
            chapterUrl = "https://example.com/book1/chapter1",
            bookUrl = bookUrl,
            summary = "章节1摘要",
            summaryRatio = 0.3f,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )
        
        val summary2 = SummaryCache(
            chapterUrl = "https://example.com/book1/chapter2",
            bookUrl = bookUrl,
            summary = "章节2摘要",
            summaryRatio = 0.3f,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )

        summaryCacheDao.insert(summary1)
        summaryCacheDao.insert(summary2)

        // 验证插入成功
        assertEquals(2, summaryCacheDao.getByBookUrl(bookUrl).size)

        // 删除整本书的摘要
        summaryCacheDao.deleteByBookUrl(bookUrl)

        // 验证删除成功
        assertEquals(0, summaryCacheDao.getByBookUrl(bookUrl).size)
    }

    @Test
    fun testGetByBookUrl() = runBlocking {
        val bookUrl = "https://example.com/book1"
        
        val summary1 = SummaryCache(
            chapterUrl = "https://example.com/book1/chapter1",
            bookUrl = bookUrl,
            summary = "章节1摘要",
            summaryRatio = 0.3f,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )
        
        val summary2 = SummaryCache(
            chapterUrl = "https://example.com/book1/chapter2",
            bookUrl = bookUrl,
            summary = "章节2摘要",
            summaryRatio = 0.3f,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L
        )

        summaryCacheDao.insert(summary1)
        summaryCacheDao.insert(summary2)

        // 查询整本书的摘要
        val summaries = summaryCacheDao.getByBookUrl(bookUrl)
        
        // 验证返回正确数量的摘要
        assertEquals(2, summaries.size)
        assertTrue(summaries.any { it.chapterUrl == "https://example.com/book1/chapter1" })
        assertTrue(summaries.any { it.chapterUrl == "https://example.com/book1/chapter2" })
    }
}