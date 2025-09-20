package io.legado.app.ui.book.read.settings

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.legado.app.R
import io.legado.app.ui.book.read.mode.SummaryCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 摘要阅读模式设置Fragment
 * 管理摘要模式下的所有配置选项
 */
class SummaryReadSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var cacheManager: SummaryCacheManager
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_summary_read, rootKey)
        cacheManager = SummaryCacheManager(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPreferences()
        loadCacheStats()
    }

    private fun setupPreferences() {
        // 摘要长度设置
        findPreference<ListPreference>("summary_length")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val lengthPercent = (newValue as String).toInt()
                updateSummaryLengthSummary(lengthPercent)
                true
            }
            updateSummaryLengthSummary(value?.toInt() ?: 30)
        }

        // AI模型选择
        findPreference<ListPreference>("summary_ai_provider")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                updateModelSummary(newValue as String)
                true
            }
            updateModelSummary(value ?: "local")
        }

        // 编码设置（从原位置移动过来）
        findPreference<ListPreference>("text_encoding")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                updateEncodingSummary(newValue as String)
                true
            }
            updateEncodingSummary(value ?: "UTF-8")
        }

        // 缓存开关
        findPreference<SwitchPreferenceCompat>("enable_cache")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (!enabled) {
                    showClearCacheDialog()
                }
                true
            }
        }

        // 缓存有效期
        findPreference<ListPreference>("cache_validity")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val days = (newValue as String).toLong()
                updateCacheValiditySummary(days)
                cacheManager.setCacheValidityDays(days)
                true
            }
            updateCacheValiditySummary(value?.toLong() ?: 30L)
        }

        // 清除缓存按钮
        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
            showClearCacheDialog()
            true
        }

        // 缓存统计
        findPreference<Preference>("cache_stats")?.apply {
            setOnPreferenceClickListener {
                loadCacheStats()
                true
            }
        }
    }

    private fun updateSummaryLengthSummary(percent: Int) {
        findPreference<ListPreference>("summary_length")?.summary = 
            "摘要长度为原文的 $percent%"
    }

    private fun updateModelSummary(provider: String) {
        val providerName = when (provider) {
            "openai" -> "OpenAI GPT"
            "wenxin" -> "百度文心一言"
            "qianwen" -> "阿里云通义千问"
            "local" -> "本地算法"
            else -> "未知提供商"
        }
        findPreference<ListPreference>("summary_ai_provider")?.summary = "当前使用：$providerName"
    }

    private fun updateCacheValiditySummary(days: Long) {
        findPreference<ListPreference>("cache_validity")?.summary = 
            "缓存有效期：$days 天"
    }

    private fun loadCacheStats() {
        scope.launch {
            val stats = withContext(Dispatchers.IO) {
                cacheManager.getCacheStats()
            }
            
            val statsPreference = findPreference<Preference>("cache_stats")
            statsPreference?.summary = buildString {
                append("已缓存章节：${stats.totalCached} 章\n")
                append("总大小：${formatFileSize(stats.totalSize)}\n")
                if (stats.totalCached > 0) {
                    append("最新缓存：${formatTime(stats.newestCache)}")
                }
            }
        }
    }

    private fun showClearCacheDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("清除缓存")
            .setMessage("确定要清除所有摘要缓存吗？")
            .setPositiveButton("清除") { _, _ ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        cacheManager.clearAll()
                    }
                    loadCacheStats()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun formatFileSize(size: Int): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = diff / (1000 * 60 * 60 * 24)
        return when {
            days == 0L -> "今天"
            days == 1L -> "昨天"
            days < 7 -> "${days}天前"
            else -> "${days / 7}周前"
        }
    }

    private fun updateEncodingSummary(encoding: String) {
        findPreference<ListPreference>("text_encoding")?.summary = 
            "文本编码：$encoding"
    }
}

/**
 * 需要添加的XML配置文件：res/xml/pref_summary_read.xml
 * 
 * 配置内容：
 * - 摘要长度选择（10%-50%）
 * - AI模型选择（OpenAI/文心一言/通义千问）
 * - 缓存开关
 * - 缓存有效期设置（7-90天）
 * - 清除缓存按钮
 * - 缓存统计信息
 */