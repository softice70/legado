package io.legado.app.ui.book.read.mode

import android.content.Context
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.BookHelp
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.read.ui.SummaryLoadingView
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.LogUtils
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import splitties.init.appCtx

class ReadModeManager(
    private val context: Context,
    private val summaryLoadingView: SummaryLoadingView
) {
    // 跟踪正在进行的摘要请求
    private val ongoingRequests = ConcurrentHashMap<String, Job>()
    
    enum class ReadMode {
        NORMAL, SUMMARY
    }
    
    companion object {
        private const val PREF_MODE_KEY = "read_mode"
        private const val TAG = "ReadModeManager"
    }
    
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _currentMode = MutableLiveData<ReadMode>()
    private val summaryRepository = AISummaryRepository.getInstance(context)
    
    init {
        // 初始化当前模式
        val savedModeOrdinal = context.getPrefInt(PREF_MODE_KEY, ReadMode.NORMAL.ordinal)
        _currentMode.value = ReadMode.values().getOrNull(savedModeOrdinal) ?: ReadMode.NORMAL
    }
    
    val currentMode: LiveData<ReadMode> = _currentMode
    
    /**
     * 切换阅读模式
     */
    fun switchMode(newMode: ReadMode, chapter: BookChapter?, callback: (Boolean) -> Unit) {
        LogUtils.d("ReadModeManager", "switchMode called: $newMode")
        
        if (_currentMode.value == newMode) {
            LogUtils.d("ReadModeManager", "Already in target mode")
            callback(true)
            return
        }

        when (newMode) {
            ReadMode.NORMAL -> {
                LogUtils.d("ReadModeManager", "Switching to NORMAL mode")
                // 切换到正常模式，清除摘要缓存并重新加载内容
                _currentMode.value = ReadMode.NORMAL
                saveModePreference(newMode)
                
                // 重新获取原始内容
                if (chapter != null) {
                    scope.launch {
                        callback(true)
                        // 重新加载当前章节内容
                        (context as? ReadBookActivity)?.lifecycleScope?.launch {
                            io.legado.app.model.ReadBook.reloadCurrentChapter()
                        }
                    }
                } else {
                    callback(true)
                    (context as? ReadBookActivity)?.lifecycleScope?.launch {
                        io.legado.app.model.ReadBook.reloadCurrentChapter()
                    }
                }
            }
            ReadMode.SUMMARY -> {
                LogUtils.d("ReadModeManager", "Switching to SUMMARY mode")
                // 切换到摘要模式，需要获取摘要
                if (chapter == null) {
                    LogUtils.e("ReadModeManager", "Chapter is null")
                    callback(false)
                    (context as? ReadBookActivity)?.toastOnUi("无法获取当前章节")
                    return
                }

                LogUtils.d("ReadModeManager", "Chapter: ${chapter.title}")

                // 立即返回callback，避免界面无响应
                callback(true)
                
                // 检查当前章节是否有摘要缓存
                val hasSummaryCache = checkSummaryCache(chapter)
                
                // 如果没有缓存，使用Toast提示用户
                if (!hasSummaryCache) {
                    (context as? ReadBookActivity)?.toastOnUi("摘要正在获取中")
                }
                
                // 在协程中异步处理摘要获取，不阻塞UI
                scope.launch {
                    try {
                        LogUtils.d("ReadModeManager", "Getting summary for chapter in background")
                        val summary = withContext(Dispatchers.IO) {
                            summaryRepository.getSummary(chapter)
                        }
                        
                        LogUtils.d("ReadModeManager", "Summary result: ${summary?.take(100)}...")
                        
                        if (summary != null && summary.isNotBlank()) {
                            LogUtils.d("ReadModeManager", "Summary obtained, updating mode")
                            _currentMode.value = ReadMode.SUMMARY
                            saveModePreference(ReadMode.SUMMARY)
                            
                            // 确保在主线程中更新UI
                            withContext(Dispatchers.Main) {
                                (context as? ReadBookActivity)?.lifecycleScope?.launch {
                                    LogUtils.d("ReadModeManager", "Reloading current chapter with summary")
                                    io.legado.app.model.ReadBook.reloadCurrentChapter()
                                }
                            }
                        } else if (!hasSummaryCache) {
                            // 只有在没有缓存的情况下才显示获取失败的提示
                            LogUtils.e("ReadModeManager", "Failed to get summary or summary is blank")
                            (context as? ReadBookActivity)?.toastOnUi("获取摘要失败")
                        }
                    } catch (e: Exception) {
                        LogUtils.e("ReadModeManager", "Exception in switchMode: ${e.message}")
                        // 在异步处理过程中只显示错误提示，不再调用callback
                        if (!hasSummaryCache) {
                            (context as? ReadBookActivity)?.toastOnUi("获取摘要失败: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /**
     * 加载章节摘要
     */
    private fun loadSummary(chapter: BookChapter, callback: (Boolean) -> Unit) {
        scope.launch {
            try {
                // 获取摘要（带缓存）
                val summary = summaryRepository.getSummary(chapter)

                if (summary != null) {
                    _currentMode.value = ReadMode.SUMMARY
                    saveModePreference(ReadMode.SUMMARY)
                    (context as? ReadBookActivity)?.runOnUiThread {
                        (context as? ReadBookActivity)?.upContent(resetPageOffset = true)
                    }
                    callback(true)
                } else {
                    callback(false)
                }
            } catch (e: Exception) {
                LogUtils.e("ReadModeManager", "加载摘要失败: ${e.message}")
                callback(false)
            }
        }
    }

    /**
     * 获取当前模式
     */
    fun getCurrentMode(): ReadMode {
        return _currentMode.value ?: ReadMode.NORMAL
    }

    /**
     * 获取当前模式下应该显示的内容
     */
    suspend fun getDisplayContent(chapter: BookChapter): String {
        return when (_currentMode.value) {
            ReadMode.SUMMARY -> {
                LogUtils.d("ReadModeManager", "Getting display content for SUMMARY mode")
                val summary = summaryRepository.getSummary(chapter)
                LogUtils.d("ReadModeManager", "Summary result: ${summary?.take(100)}...")
                if (summary != null && summary.isNotBlank()) {
                    LogUtils.d("ReadModeManager", "Returning summary content")
                    summary
                } else {
                    LogUtils.d("ReadModeManager", "Summary is null or blank, returning raw text")
                    chapter.getRawText()
                }
            }
            else -> {
                LogUtils.d("ReadModeManager", "Getting display content for NORMAL mode")
                chapter.getRawText()
            }
        }
    }

    /**
     * 获取章节标题（带模式标识）
     */
    fun getChapterTitle(chapterTitle: String): String {
        return when (_currentMode.value) {
            ReadMode.SUMMARY -> "$chapterTitle（摘要）"
            else -> chapterTitle
        }
    }

    /**
     * 检查摘要是否可用
     */
    suspend fun isSummaryAvailable(chapter: BookChapter): Boolean {
        return try {
            val summary = summaryRepository.getSummary(chapter)
            summary != null && summary != chapter.getRawText()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        scope.launch {
            summaryRepository.clearAllCache()
        }
    }

    /**
     * 保存模式偏好
     */
    private fun saveModePreference(mode: ReadMode) {
        appCtx.putPrefInt(PREF_MODE_KEY, mode.ordinal)
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
        ongoingRequests.clear()
    }

    /**
     * 获取朗读内容
     */
    suspend fun getReadAloudContent(chapter: BookChapter): String {
        return when (_currentMode.value) {
            ReadMode.SUMMARY -> {
                summaryRepository.getSummary(chapter) ?: chapter.getRawText()
            }
            else -> chapter.getRawText()
        }
    }

    /**
     * 预加载摘要
     * 1. 首先检查当前章节是否有摘要，若有则尝试加载下一章节的摘要
     * 2. 若没有则加载本章节的摘要，在本章节的摘要请求完成后，再继续尝试加载下一章节的摘要
     */
    fun preloadSummary(currentChapter: BookChapter, nextChapter: BookChapter? = null) {
        scope.launch {
            try {
                // 先检查当前章节是否有摘要或已有请求正在进行
                val currentSummary = getSummaryWithRequestTracking(currentChapter)
                if (currentSummary != null && currentSummary.isNotBlank()) {
                    LogUtils.d("ReadModeManager", "Current chapter has summary, preloading next chapter")
                    // 当前章节有摘要，尝试预加载下一章节
                    nextChapter?.let {
                        getSummaryWithRequestTracking(it)
                        LogUtils.d("ReadModeManager", "Next chapter summary preloaded")
                    }
                } else {
                    LogUtils.d("ReadModeManager", "Current chapter has no summary, loading current first")
                    // 当前章节没有摘要，先加载当前章节的摘要
                    getSummaryWithRequestTracking(currentChapter)
                    LogUtils.d("ReadModeManager", "Current chapter summary loaded, now preloading next chapter")
                    // 然后尝试加载下一章节的摘要
                    nextChapter?.let {
                        getSummaryWithRequestTracking(it)
                        LogUtils.d("ReadModeManager", "Next chapter summary preloaded")
                    }
                }
            } catch (e: Exception) {
                LogUtils.e("ReadModeManager", "Exception during preload: ${e.message}")
            }
        }
    }

    /**
     * 带请求跟踪的摘要获取方法
     * 确保不会对同一章节发送多个并行请求
     */
    private suspend fun getSummaryWithRequestTracking(chapter: BookChapter): String? {
        val chapterUrl = chapter.url
        
        // 检查是否已有请求正在进行
        ongoingRequests[chapterUrl]?.let { job ->
            if (job.isActive) {
                LogUtils.d("ReadModeManager", "Request already in progress for chapter: $chapterUrl")
                // 如果请求正在进行，则等待其完成
                try {
                    job.join()
                    // 请求完成后，尝试从缓存获取结果
                    return summaryRepository.getSummary(chapter)
                } catch (e: Exception) {
                    LogUtils.e("ReadModeManager", "Error waiting for ongoing request: ${e.message}")
                    // 请求失败，从正在进行的请求列表中移除
                    ongoingRequests.remove(chapterUrl)
                }
            }
        }
        
        // 创建新的异步请求
        val requestJob = scope.async(Dispatchers.IO) {
            try {
                summaryRepository.getSummary(chapter)
            } catch (e: Exception) {
                LogUtils.e("ReadModeManager", "Error getting summary: ${e.message}")
                null
            } finally {
                // 请求完成后，从正在进行的请求列表中移除
                ongoingRequests.remove(chapterUrl)
            }
        }
        
        // 将请求添加到正在进行的请求列表
        ongoingRequests[chapterUrl] = requestJob
        
        // 等待请求完成并返回结果
        return requestJob.await()
    }
    
    /**
     * 预加载下一章摘要（保持原有方法签名兼容）
     */
    fun preloadNextChapter(chapter: BookChapter) {
        preloadSummary(getCurrentChapter() ?: return, chapter)
    }

    /**
     * 切换摘要模式
     */
    fun toggleSummaryMode() {
        val currentChapter = getCurrentChapter() ?: return
        val newMode = if (_currentMode.value == ReadMode.SUMMARY) {
            ReadMode.NORMAL
        } else {
            ReadMode.SUMMARY
        }
        
        switchMode(newMode, currentChapter) { success ->
            if (success) {
                // 重新加载当前章节内容
                (context as? ReadBookActivity)?.lifecycleScope?.launch {
                    ReadBook.reloadCurrentChapter()
                }
            }
        }
    }

    /**
     * 获取当前章节
     */
    private fun getCurrentChapter(): BookChapter? {
        return ReadBook.book?.let { book ->
            appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex)
        }
    }

    /**
     * 获取章节缓存状态
     */
    fun getCacheStatus(chapter: BookChapter): CacheStatus {
        // 暂时返回NONE状态，因为缺少实际的检查方法
        return CacheStatus.NONE
    }

    /**
     * 获取章节缓存进度
     */
    fun getCacheProgress(chapter: BookChapter): Int {
        // 暂时返回0进度，因为缺少实际的检查方法
        return 0
    }

    /**
     * 缓存状态枚举
     */
    enum class CacheStatus {
        NONE,       // 无缓存
        GENERATING, // 生成中
        CACHED      // 已缓存
    }

    /**
     * 监听缓存状态变化
     */
    fun observeCacheStatus(chapter: BookChapter): LiveData<CacheStatus> {
        return MutableLiveData(CacheStatus.NONE)
    }

    /**
     * 监听缓存进度变化
     */
    fun observeCacheProgress(chapter: BookChapter): LiveData<Int> {
        return MutableLiveData(0)
    }

    /**
     * 监听当前章节摘要生成状态
     */
    fun observeCurrentSummaryStatus(): LiveData<CacheStatus> {
        return MutableLiveData(CacheStatus.NONE)
    }

    /**
     * 监听当前章节缓存进度
     */
    fun observeCurrentCacheProgress(): LiveData<Int> {
        return MutableLiveData(0)
    }

    /**
     * 监听摘要内容变化
     */
    fun observeSummaryContent(chapter: BookChapter): LiveData<String> {
        return MutableLiveData("")
    }

    /**
     * 监听当前章节摘要内容
     */
    fun observeCurrentSummaryContent(): LiveData<String> {
        return MutableLiveData("")
    }

    /**
     * 监听当前章节摘要生成状态变化
     */
    fun observeCurrentSummaryState(): LiveData<Pair<CacheStatus, Int>> {
        return MutableLiveData(Pair(CacheStatus.NONE, 0))
    }
    
    /**
     * 检查章节是否有摘要缓存
     * 只检查缓存状态，不实际获取摘要内容
     */
    private fun checkSummaryCache(chapter: BookChapter): Boolean {
        return try {
            runBlocking(Dispatchers.IO) {
                val cache = appDb.summaryCacheDao.getByChapterUrl(chapter.url)
                // 检查缓存是否存在且未过期
                cache != null && cache.expiresAt > System.currentTimeMillis()
            }
        } catch (e: Exception) {
            LogUtils.e("ReadModeManager", "Error checking summary cache: ${e.message}")
            false
        }
    }
    
    /**
     * 显示摘要加载提示
     */
    private fun showSummaryLoading() {
        // 确保在主线程执行
        if (context is ReadBookActivity) {
            val activity = context as ReadBookActivity
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // 已经在主线程
                summaryLoadingView.showLoading(SummaryLoadingView.LOADING_GENERATING)
                LogUtils.d(TAG, "Summary loading view shown (main thread)")
            } else {
                // 不在主线程，切换到主线程
                activity.runOnUiThread {
                    summaryLoadingView.showLoading(SummaryLoadingView.LOADING_GENERATING)
                    LogUtils.d(TAG, "Summary loading view shown (runOnUiThread)")
                }
            }
        } else {
            LogUtils.e(TAG, "Context is not ReadBookActivity")
        }
    }
    
    /**
     * 隐藏摘要加载提示
     */
    private fun hideSummaryLoading() {
        // 确保在主线程执行
        if (context is ReadBookActivity) {
            val activity = context as ReadBookActivity
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // 已经在主线程
                summaryLoadingView.hide()
                LogUtils.d(TAG, "Summary loading view hidden (main thread)")
            } else {
                // 不在主线程，切换到主线程
                activity.runOnUiThread {
                    summaryLoadingView.hide()
                    LogUtils.d(TAG, "Summary loading view hidden (runOnUiThread)")
                }
            }
        } else {
            LogUtils.e(TAG, "Context is not ReadBookActivity")
        }
    }

    /**
     * 获取缓存的摘要内容
     */
    fun getCachedSummary(chapter: BookChapter): String? {
        // 暂时返回null，因为缺少实际的缓存获取方法
        return null
    }

    /**
     * 创建包含缓存状态、进度和内容的综合LiveData
     */
    fun createSummaryStateLiveData(): LiveData<Triple<CacheStatus, Int, String>> {
        return MutableLiveData(Triple(CacheStatus.NONE, 0, ""))
    }

    // 扩展函数获取章节原始文本
    private suspend fun BookChapter.getRawText(): String {
        val book = io.legado.app.model.ReadBook.book ?: return ""
        return BookHelp.getContent(book, this) ?: ""
    }
}