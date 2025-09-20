package io.legado.app.ui.book.read.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.mode.ReadModeManager

/**
 * 正常阅读模式设置Fragment
 * 管理正常阅读模式下的配置选项
 */
class NormalReadSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_normal_read, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPreferences()
    }

    private fun setupPreferences() {
        // 字体设置
        findPreference<androidx.preference.ListPreference>("font_size")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                updateFontSizeSummary(newValue as String)
                true
            }
            updateFontSizeSummary(value ?: "16")
        }

        // 行间距设置
        findPreference<androidx.preference.ListPreference>("line_spacing")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                updateLineSpacingSummary(newValue as String)
                true
            }
            updateLineSpacingSummary(value ?: "1.5")
        }

        // 阅读模式切换
        findPreference<SwitchPreferenceCompat>("summary_mode")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                ReadBook.readModeManager.switchMode(
                    if (enabled) ReadModeManager.ReadMode.SUMMARY 
                    else ReadModeManager.ReadMode.NORMAL,
                    null
                ) { success ->
                    if (!success) {
                        isChecked = !enabled
                    }
                }
                true
            }
        }
    }

    private fun updateFontSizeSummary(size: String) {
        findPreference<androidx.preference.ListPreference>("font_size")?.summary = 
            "字体大小：$size sp"
    }

    private fun updateLineSpacingSummary(spacing: String) {
        findPreference<androidx.preference.ListPreference>("line_spacing")?.summary = 
            "行间距：$spacing 倍"
    }
}

/**
 * 需要添加的XML配置文件：res/xml/pref_normal_read.xml
 * 
 * 配置内容：
 * - 字体大小设置
 * - 行间距设置
 * - 文本编码设置（从原位置移动）
 * - 其他正常阅读模式下的设置项
 */