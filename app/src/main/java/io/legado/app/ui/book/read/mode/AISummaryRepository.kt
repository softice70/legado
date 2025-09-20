package io.legado.app.ui.book.read.mode

import android.content.Context
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.SummaryCache
import io.legado.app.data.dao.SummaryCacheDao
import io.legado.app.help.book.BookHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.LogUtils
import splitties.init.appCtx
import io.legado.app.help.http.okHttpClient
import io.legado.app.model.ReadBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * AI摘要仓库
 * 负责与AI服务交互生成章节摘要
 */
class AISummaryRepository private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: AISummaryRepository? = null
        
        // 静态内存缓存，确保在应用生命周期内保持
        private val memoryCache = mutableMapOf<String, Pair<String, Long>>()
        private const val MEMORY_CACHE_DURATION = 5 * 60 * 1000L // 5分钟
        
        fun getInstance(context: Context): AISummaryRepository {
            val instanceId = System.identityHashCode(INSTANCE)
            LogUtils.d("AISummaryRepository", "getInstance called, current instance ID: $instanceId")
            LogUtils.d("AISummaryRepository", "Memory cache size at getInstance: ${memoryCache.size}")
            LogUtils.d("AISummaryRepository", "Memory cache keys at getInstance: ${memoryCache.keys.joinToString()}")
            
            return INSTANCE ?: synchronized(this) {
                val existing = INSTANCE
                if (existing != null) {
                    LogUtils.d("AISummaryRepository", "Returning existing instance: ${System.identityHashCode(existing)}")
                    existing
                } else {
                    val newInstance = AISummaryRepository(context.applicationContext)
                    INSTANCE = newInstance
                    LogUtils.d("AISummaryRepository", "Created new singleton instance: ${System.identityHashCode(newInstance)}")
                    LogUtils.d("AISummaryRepository", "Memory cache size at creation: ${memoryCache.size}")
                    newInstance
                }
            }
        }
        
        // 添加调试方法
        fun debugMemoryCache() {
            LogUtils.d("AISummaryRepository", "DEBUG: Memory cache size: ${memoryCache.size}")
            LogUtils.d("AISummaryRepository", "DEBUG: Memory cache keys: ${memoryCache.keys.joinToString()}")
            memoryCache.forEach { (key, value) ->
                LogUtils.d("AISummaryRepository", "DEBUG: Cache entry - Key: $key, Value length: ${value.first.length}, Timestamp: ${value.second}")
            }
        }
        
        private const val DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val MAX_TEXT_LENGTH = 8000 // 最大文本长度限制
        private const val MIN_TEXT_LENGTH = 100  // 最小文本长度限制
        private const val CACHE_VALIDITY_DAYS = 7L // 缓存有效期7天
    }
    
    private val cacheTimeout = MEMORY_CACHE_DURATION
    
    init {
        val instanceId = System.identityHashCode(this)
        LogUtils.d("AISummaryRepository", "AISummaryRepository instance created: $instanceId")
        LogUtils.d("AISummaryRepository", "Memory cache size at init: ${memoryCache.size}")
    }

    data class SummaryRequest(
        val text: String,
        val ratio: Float = 0.3f,
        val model: String = "gpt-3.5-turbo"
    )

    data class SummaryResponse(
        val summary: String,
        val originalLength: Int,
        val summaryLength: Int,
        val model: String
    )



    /**
     * 获取章节摘要（带缓存）
     */
    suspend fun getSummary(chapter: BookChapter, rawContent: String? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                LogUtils.d("AISummaryRepository", "getSummary called for chapter: ${chapter.title}")
                val chapterUrl = chapter.url
                val content = rawContent ?: getChapterContent(chapter) ?: return@withContext null
                
                LogUtils.d("AISummaryRepository", "Content length: ${content.length}")
                
                // 检查文本长度
                if (content.length < MIN_TEXT_LENGTH) {
                    LogUtils.d("AISummaryRepository", "Content too short, returning original")
                    return@withContext content // 文本太短，直接返回原文
                }
                
                // 1. 先检查内存缓存
                LogUtils.d("AISummaryRepository", "Checking memory cache for: $chapterUrl")
                LogUtils.d("AISummaryRepository", "Memory cache size: ${memoryCache.size}")
                val memoryCached = getMemoryCachedSummary(chapterUrl)
                if (memoryCached != null) {
                    LogUtils.d("AISummaryRepository", "Found memory cached summary: ${memoryCached.take(100)}...")
                    return@withContext memoryCached
                }
                LogUtils.d("AISummaryRepository", "No memory cache found")
                
                // 2. 检查数据库缓存
                val cachedSummary = getCachedSummary(chapterUrl)
                if (cachedSummary != null && cachedSummary.isNotBlank()) {
                    LogUtils.d("AISummaryRepository", "Found database cached summary: ${cachedSummary.take(100)}...")
                    // 同时保存到内存缓存
                    saveToMemoryCache(chapterUrl, cachedSummary)
                    return@withContext cachedSummary
                }
                
                LogUtils.d("AISummaryRepository", "No cached summary, generating new one")
                
                // 3. 缓存未命中，生成新的摘要
                val summary = generateSummary(chapter, content)
                
                LogUtils.d("AISummaryRepository", "Generated summary: ${summary?.take(100)}...")
                
                // 4. 保存到缓存
                if (summary != null && summary.isNotBlank()) {
                    // 先保存到内存缓存，确保立即可用
                    saveToMemoryCache(chapterUrl, summary)
                    LogUtils.d("AISummaryRepository", "Summary saved to memory cache")
                    
                    // 异步保存到数据库
                    saveSummaryCache(chapterUrl, content, summary)
                    LogUtils.d("AISummaryRepository", "Summary saved to database cache")
                    
                    return@withContext summary
                } else {
                    LogUtils.d("AISummaryRepository", "Generated summary is null or blank")
                    return@withContext null
                }
            } catch (e: Exception) {
                LogUtils.e("AISummaryRepository", "Error in getSummary: ${e.message}")
                null
            }
        }
    }

    /**
     * 生成章节摘要（支持传入原始内容）
     */
    suspend fun generateSummary(chapter: BookChapter, rawContent: String? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                val content = rawContent ?: getChapterContent(chapter) ?: return@withContext null
                
                // 检查文本长度
                if (content.length < MIN_TEXT_LENGTH) {
                    return@withContext content // 文本太短，直接返回原文
                }
                
                val truncatedContent = if (content.length > MAX_TEXT_LENGTH) {
                    content.substring(0, MAX_TEXT_LENGTH)
                } else {
                    content
                }
                
                val summaryRatio = AppConfig.summaryRatio / 100f
                val summary = callAIService(truncatedContent, summaryRatio)
                
                summary?.let {
                    // 清理摘要格式
                    cleanSummaryText(it)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 获取内存缓存的摘要
     */
    private fun getMemoryCachedSummary(chapterUrl: String): String? {
        val instanceId = System.identityHashCode(this)
        LogUtils.d("AISummaryRepository", "getMemoryCachedSummary called for: $chapterUrl (instance: $instanceId)")
        LogUtils.d("AISummaryRepository", "Memory cache size: ${memoryCache.size}")
        LogUtils.d("AISummaryRepository", "Memory cache keys: ${memoryCache.keys.joinToString(", ")}")
        
        val cached = memoryCache[chapterUrl]
        LogUtils.d("AISummaryRepository", "Memory cache entry exists: ${cached != null}")
        
        return if (cached != null && System.currentTimeMillis() - cached.second < cacheTimeout) {
            LogUtils.d("AISummaryRepository", "Memory cache hit for: $chapterUrl")
            cached.first
        } else {
            if (cached != null) {
                val age = System.currentTimeMillis() - cached.second
                LogUtils.d("AISummaryRepository", "Memory cache expired for: $chapterUrl, age: ${age}ms, timeout: ${cacheTimeout}ms")
                memoryCache.remove(chapterUrl)
            } else {
                LogUtils.d("AISummaryRepository", "No memory cache entry for: $chapterUrl")
            }
            null
        }
    }
    
    /**
     * 保存到内存缓存
     */
    private fun saveToMemoryCache(chapterUrl: String, summary: String) {
        val instanceId = System.identityHashCode(this)
        val timestamp = System.currentTimeMillis()
        
        // 添加详细的调试信息
        LogUtils.d("AISummaryRepository", "BEFORE saveToMemoryCache:")
        LogUtils.d("AISummaryRepository", "  - Memory cache size: ${memoryCache.size}")
        LogUtils.d("AISummaryRepository", "  - Memory cache keys: ${memoryCache.keys.joinToString()}")
        LogUtils.d("AISummaryRepository", "  - Saving key: $chapterUrl")
        LogUtils.d("AISummaryRepository", "  - Summary length: ${summary.length}")
        LogUtils.d("AISummaryRepository", "  - Instance ID: $instanceId")
        LogUtils.d("AISummaryRepository", "  - Memory cache object ID: ${System.identityHashCode(memoryCache)}")
        
        memoryCache[chapterUrl] = Pair(summary, timestamp)
        
        LogUtils.d("AISummaryRepository", "AFTER saveToMemoryCache:")
        LogUtils.d("AISummaryRepository", "  - Memory cache size: ${memoryCache.size}")
        LogUtils.d("AISummaryRepository", "  - Memory cache keys: ${memoryCache.keys.joinToString()}")
        
        // 验证保存是否成功
        val saved = memoryCache[chapterUrl]
        if (saved != null) {
            LogUtils.d("AISummaryRepository", "  - Verification: Successfully saved, length: ${saved.first.length}")
        } else {
            LogUtils.e("AISummaryRepository", "  - Verification: FAILED to save to memory cache!")
        }
        
        // 调用调试方法
        debugMemoryCache()
    }
    
    /**
     * 清除内存缓存
     */
    private fun clearMemoryCache() {
        memoryCache.clear()
        LogUtils.d("AISummaryRepository", "Memory cache cleared")
    }

    /**
     * 获取缓存的摘要
     */
    private suspend fun getCachedSummary(chapterUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                LogUtils.d("AISummaryRepository", "Querying database cache for chapterUrl: $chapterUrl")
                
                // 先查询所有缓存记录，用于调试
                val allCaches = appDb.summaryCacheDao.getAll()
                LogUtils.d("AISummaryRepository", "Total cache records in database: ${allCaches.size}")
                allCaches.forEach { cache ->
                    LogUtils.d("AISummaryRepository", "Cache record: chapterUrl=${cache.chapterUrl}, bookUrl=${cache.bookUrl}")
                }
                
                val cache = appDb.summaryCacheDao.getByChapterUrl(chapterUrl)
                LogUtils.d("AISummaryRepository", "Database cache query result: ${if (cache != null) "found" else "not found"}")
                
                cache?.let {
                    LogUtils.d("AISummaryRepository", "Database cache found, checking expiry. Created: ${it.createdAt}, Expires: ${it.expiresAt}, Current: ${System.currentTimeMillis()}")
                    // 检查缓存是否过期
                    if (!it.isExpired()) {
                        LogUtils.d("AISummaryRepository", "Database cache is valid, returning summary")
                        it.summary
                    } else {
                        LogUtils.d("AISummaryRepository", "Database cache expired, deleting")
                        // 缓存过期，删除旧缓存
                        appDb.summaryCacheDao.deleteByChapterUrl(chapterUrl)
                        null
                    }
                }
            } catch (e: Exception) {
                LogUtils.e("AISummaryRepository", "Error getting cached summary: ${e.message}")
                null
            }
        }
    }

    /**
     * 保存摘要到缓存
     */
    private suspend fun saveSummaryCache(
        chapterUrl: String,
        originalText: String,
        summaryText: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val bookUrl = ReadBook.book?.bookUrl ?: ""
                LogUtils.d("AISummaryRepository", "Saving cache for chapterUrl: $chapterUrl")
                LogUtils.d("AISummaryRepository", "BookUrl: $bookUrl")
                LogUtils.d("AISummaryRepository", "Summary length: ${summaryText.length}")
                
                val cache = SummaryCache.create(
                    chapterUrl = chapterUrl,
                    bookUrl = bookUrl,
                    summaryText = summaryText,
                    originalText = originalText
                )
                
                LogUtils.d("AISummaryRepository", "Cache object created, inserting to database")
                appDb.summaryCacheDao.insert(cache)
                LogUtils.d("AISummaryRepository", "Cache inserted successfully")
                
                // 立即验证保存是否成功
                val verification = appDb.summaryCacheDao.getByChapterUrl(chapterUrl)
                LogUtils.d("AISummaryRepository", "Save verification: ${if (verification != null) "success" else "failed"}")
                
            } catch (e: Exception) {
                LogUtils.e("AISummaryRepository", "Error saving cache: ${e.message}")
            }
        }
    }

    /**
     * 清除章节缓存
     */
    suspend fun clearChapterCache(chapterUrl: String) {
        withContext(Dispatchers.IO) {
            // 清除内存缓存
            memoryCache.remove(chapterUrl)
            // 清除数据库缓存
            appDb.summaryCacheDao.deleteByChapterUrl(chapterUrl)
            LogUtils.dWithStack("AISummaryRepository", "Cache cleared for chapterUrl: $chapterUrl")
        }
    }

    /**
     * 清除所有摘要缓存
     */
    suspend fun clearAllCache() {
        withContext(Dispatchers.IO) {
            // 清除内存缓存
            clearMemoryCache()
            // 清除数据库缓存
            appDb.summaryCacheDao.clearAll()
            LogUtils.dWithStack("AISummaryRepository", "All cache cleared")
        }
    }

    /**
     * 获取缓存统计信息
     */
    suspend fun getCacheStats(): SummaryCacheDao.CacheStats? {
        return withContext(Dispatchers.IO) {
            appDb.summaryCacheDao.getCacheStats()
        }
    }

    /**
     * 调用AI服务生成摘要
     */
    private suspend fun callAIService(text: String, ratio: Float): String? {
        LogUtils.d("AISummaryRepository", "callAIService called")
        LogUtils.d("AISummaryRepository", "AI config valid: ${AppConfig.isAiConfigValid()}")
        LogUtils.d("AISummaryRepository", "AI provider: ${AppConfig.aiProvider}")
        
        if (!AppConfig.isAiConfigValid()) {
            LogUtils.d("AISummaryRepository", "AI config invalid, using local summary")
            return generateLocalSummary(text, ratio)
        }

        return try {
            // 首先获取配置的provider
            val provider = AppConfig.aiProvider
            LogUtils.d("AISummaryRepository", "Configured provider: $provider")
            
            // 根据provider值或关键词决定调用哪个API
            val result = when {
                // 直接匹配
                provider == "wenxin" -> {
                    LogUtils.d("AISummaryRepository", "Calling Wenxin API")
                    callWenxinAPI(text, ratio)
                }
                provider == "qianwen" -> {
                    LogUtils.d("AISummaryRepository", "Calling Qianwen API")
                    callQianwenAPI(text, ratio)
                }
                provider == "openai" -> {
                    LogUtils.d("AISummaryRepository", "Calling OpenAI API")
                    callOpenAIAPI(text, ratio)
                }
                // 关键词匹配（处理配置了完整模型名称的情况）
                provider.contains("文心") || provider.contains("ERNIE") -> {
                    LogUtils.d("AISummaryRepository", "Provider contains Wenxin/ERNIE keywords, using Wenxin API")
                    callWenxinAPI(text, ratio)
                }
                provider.contains("千问") || provider.contains("QIANWEN") || provider.contains("QWEN") -> {
                    LogUtils.d("AISummaryRepository", "Provider contains Qianwen/Qwen keywords, using Qianwen API")
                    callQianwenAPI(text, ratio)
                }
                provider.contains("GPT") || provider.contains("OPENAI") -> {
                    LogUtils.d("AISummaryRepository", "Provider contains GPT/OpenAI keywords, using OpenAI API")
                    callOpenAIAPI(text, ratio)
                }
                else -> {
                    LogUtils.d("AISummaryRepository", "Unknown provider '$provider', using local summary")
                    generateLocalSummary(text, ratio)
                }
            }
            LogUtils.d("AISummaryRepository", "AI service result: ${result?.take(100)}...")
            result
        } catch (e: Exception) {
            LogUtils.e("AISummaryRepository", "Error calling AI service: ${e.message}")
            generateLocalSummary(text, ratio)
        }
    }

    /**
     * 调用文心一言API
     */
    private suspend fun callWenxinAPI(text: String, ratio: Float): String? {
        val apiUrl = AppConfig.getAiApiUrl() ?: return generateLocalSummary(text, ratio)
        val headers = AppConfig.getAiAuthHeaders()
        val prompt = buildSummaryPrompt(text, ratio)
        
        val requestBody = buildWenxinRequestBody(prompt)
        val request = okhttp3.Request.Builder()
            .url(apiUrl)
            .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        return if (response.isSuccessful) {
            val responseBody = response.body?.string()
            responseBody?.let { parseWenxinResponse(it) }
        } else {
            generateLocalSummary(text, ratio)
        }
    }

    /**
     * 调用通义千问API
     */
    private suspend fun callQianwenAPI(text: String, ratio: Float): String? {
        val apiUrl = AppConfig.getAiApiUrl() ?: return generateLocalSummary(text, ratio)
        val headers = AppConfig.getAiAuthHeaders()
        val prompt = buildSummaryPrompt(text, ratio)
        
        val requestBody = buildQianwenRequestBody(prompt)
        val request = okhttp3.Request.Builder()
            .url(apiUrl)
            .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        return if (response.isSuccessful) {
            val responseBody = response.body?.string()
            responseBody?.let { parseQianwenResponse(it) }
        } else {
            generateLocalSummary(text, ratio)
        }
    }

    /**
     * 调用OpenAI API
     */
    private suspend fun callOpenAIAPI(text: String, ratio: Float): String? {
        val apiUrl = AppConfig.getAiApiUrl() ?: return generateLocalSummary(text, ratio)
        val headers = AppConfig.getAiAuthHeaders()
        val prompt = buildSummaryPrompt(text, ratio)
        
        val requestBody = buildOpenAIRequestBody(prompt)
        val request = okhttp3.Request.Builder()
            .url(apiUrl)
            .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        return if (response.isSuccessful) {
            val responseBody = response.body?.string()
            responseBody?.let { parseOpenAIResponse(it) }
        } else {
            generateLocalSummary(text, ratio)
        }
    }

    /**
     * 构建摘要提示词
     */
    private fun buildSummaryPrompt(text: String, ratio: Float): String {
        val abstractLen = (text.length * ratio).toInt()
        val minAbstractLen = (text.length * (ratio - 0.1f).coerceAtLeast(0.1f)).toInt()
        val maxAbstractLen = (text.length * (ratio + 0.1f)).toInt()
        
        return """
            请为下面的文字写一个摘要，摘要的长度大约是${abstractLen}字左右，
            最短不少于${minAbstractLen}字，最长不超过${maxAbstractLen}字，
            摘要的文本风格尽量使用原文本的行文风格。
            
            要求：
            1. 保留关键信息和主要情节
            2. 保持原文的核心意思
            3. 语言流畅，易于理解
            4. 不要添加个人观点或解释

            文本内容：
            $text
        """.trimIndent()
    }

    /**
     * 构建文心一言请求体
     */
    private fun buildWenxinRequestBody(prompt: String): okhttp3.RequestBody {
        val requestMap = mapOf(
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            ),
            "temperature" to AppConfig.aiTemperature,
            "max_output_tokens" to AppConfig.aiMaxTokens,
            "top_p" to 0.8,
            "penalty_score" to 1.0
        )
        
        val json = GSON.toJson(requestMap)
        return json.toRequestBody("application/json".toMediaType())
    }

    /**
     * 构建通义千问请求体
     */
    private fun buildQianwenRequestBody(prompt: String): okhttp3.RequestBody {
        val requestMap = mapOf(
            "model" to AppConfig.qianwenModel,
            "input" to mapOf(
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to prompt
                    )
                )
            ),
            "parameters" to mapOf(
                "temperature" to AppConfig.aiTemperature,
                "max_tokens" to AppConfig.aiMaxTokens,
                "top_p" to 0.8
            )
        )
        
        val json = GSON.toJson(requestMap)
        return json.toRequestBody("application/json".toMediaType())
    }

    /**
     * 构建OpenAI请求体
     */
    private fun buildOpenAIRequestBody(prompt: String): okhttp3.RequestBody {
        val requestMap = mapOf(
            "model" to AppConfig.openaiModel,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            ),
            "max_tokens" to AppConfig.aiMaxTokens,
            "temperature" to AppConfig.aiTemperature
        )
        
        val json = GSON.toJson(requestMap)
        return json.toRequestBody("application/json".toMediaType())
    }

    /**
     * 解析文心一言响应
     */
    private fun parseWenxinResponse(response: String): String? {
        return try {
            val jsonObject = GSON.fromJsonObject<Map<String, Any>>(response).getOrNull()
            jsonObject?.get("result") as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析通义千问响应
     */
    private fun parseQianwenResponse(response: String): String? {
        return try {
            val jsonObject = GSON.fromJsonObject<Map<String, Any>>(response).getOrNull()
            val output = jsonObject?.get("output") as? Map<String, Any>
            val choices = output?.get("choices") as? List<Map<String, Any>>
            val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
            message?.get("content") as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析OpenAI响应
     */
    private fun parseOpenAIResponse(response: String): String? {
        return try {
            val jsonObject = GSON.fromJsonObject<Map<String, Any>>(response).getOrNull()
            val choices = jsonObject?.get("choices") as? List<Map<String, Any>>
            val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
            message?.get("content") as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 生成本地摘要（备用方案）
     */
    private fun generateLocalSummary(text: String, ratio: Float): String {
        val sentences = text.split("[。！？]".toRegex())
            .filter { it.trim().isNotEmpty() }
        
        val targetCount = (sentences.size * ratio).toInt().coerceAtLeast(1)
        val step = sentences.size.toFloat() / targetCount
        
        val summarySentences = mutableListOf<String>()
        for (i in 0 until targetCount) {
            val index = (i * step).toInt()
            if (index < sentences.size) {
                summarySentences.add(sentences[index])
            }
        }
        
        return summarySentences.joinToString("。") + "..."
    }

    /**
     * 获取章节内容
     */
    private suspend fun getChapterContent(chapter: BookChapter): String? {
        return withContext(Dispatchers.IO) {
            val book = io.legado.app.model.ReadBook.book ?: return@withContext null
            BookHelp.getContent(book, chapter)
        }
    }

    /**
     * 清理摘要文本
     */
    private fun cleanSummaryText(text: String): String {
        return text
            .replace("摘要：", "")
            .replace("总结：", "")
            .replace("简要：", "")
            .trim()
    }

    /**
     * 检查是否支持AI摘要
     */
    fun isAISupported(): Boolean {
        return AppConfig.isAiConfigValid()
    }

    /**
     * 获取当前提供商支持的模型列表
     */
    fun getSupportedModels(): List<String> {
        return when (AppConfig.aiProvider) {
            "wenxin" -> listOf(
                "ernie-4.0-8k", "ernie-4.0-8k-preview", "ernie-3.5-8k", 
                "ernie-3.5-8k-preview", "ernie-lite-8k", "ernie-speed-8k",
                "ernie-speed-128k", "ernie-tiny-8k"
            )
            "qianwen" -> listOf(
                "qwen-turbo", "qwen-plus", "qwen-max", "qwen-max-1201",
                "qwen-max-longcontext", "qwen-long", "qwen-coder-turbo", "qwen-math-turbo"
            )
            "openai" -> listOf(
                "gpt-3.5-turbo", "gpt-4", "gpt-4-turbo", "gpt-4o", "gpt-4o-mini"
            )
            else -> listOf("local")
        }
    }

    /**
     * 获取当前使用的模型名称
     */
    fun getCurrentModel(): String {
        return AppConfig.getCurrentAiModel()
    }

    /**
     * 测试AI服务连接
     */
    suspend fun testConnection(): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!AppConfig.isAiConfigValid()) {
                    return@withContext Pair(false, "AI配置无效")
                }

                val testText = "这是一个测试文本，用于验证AI服务连接是否正常。"
                val result = callAIService(testText, 0.5f)
                
                if (result != null && result.isNotBlank()) {
                    Pair(true, "连接成功")
                } else {
                    Pair(false, "服务响应为空")
                }
            } catch (e: Exception) {
                Pair(false, e.message ?: "连接失败")
            }
        }
    }
}