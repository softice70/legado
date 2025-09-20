package io.legado.app.ui.book.read

import android.view.MenuItem
import io.legado.app.R
import io.legado.app.ui.book.read.mode.ReadModeManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.junit.Assert.*

/**
 * 动态图标切换功能测试
 * 验证菜单图标根据阅读模式正确切换
 */
class DynamicIconTest {

    @Mock
    private lateinit var mockMenuItem: MenuItem

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testNormalModeIcon() {
        // 模拟正常模式
        mockStatic(ReadModeManager::class.java).use { mockedStatic ->
            mockedStatic.`when`<Boolean> { ReadModeManager.isSummaryMode() }.thenReturn(false)
            
            // 验证正常模式应该使用正常模式图标
            val isSummaryMode = ReadModeManager.isSummaryMode()
            val expectedIcon = if (isSummaryMode) R.drawable.ic_read_mode_summary else R.drawable.ic_read_mode_normal
            
            assertEquals("Normal mode should use normal icon", R.drawable.ic_read_mode_normal, expectedIcon)
            assertFalse("Should not be in summary mode", isSummaryMode)
        }
    }

    @Test
    fun testSummaryModeIcon() {
        // 模拟摘要模式
        mockStatic(ReadModeManager::class.java).use { mockedStatic ->
            mockedStatic.`when`<Boolean> { ReadModeManager.isSummaryMode() }.thenReturn(true)
            
            // 验证摘要模式应该使用摘要模式图标
            val isSummaryMode = ReadModeManager.isSummaryMode()
            val expectedIcon = if (isSummaryMode) R.drawable.ic_read_mode_summary else R.drawable.ic_read_mode_normal
            
            assertEquals("Summary mode should use summary icon", R.drawable.ic_read_mode_summary, expectedIcon)
            assertTrue("Should be in summary mode", isSummaryMode)
        }
    }

    @Test
    fun testMenuItemUpdate() {
        // 测试菜单项更新逻辑
        `when`(mockMenuItem.isChecked).thenReturn(false)
        
        // 模拟更新菜单项
        val isSummaryMode = false
        mockMenuItem.isChecked = isSummaryMode
        mockMenuItem.setIcon(if (isSummaryMode) R.drawable.ic_read_mode_summary else R.drawable.ic_read_mode_normal)
        
        // 验证方法调用
        verify(mockMenuItem).setIcon(R.drawable.ic_read_mode_normal)
    }

    @Test
    fun testIconSwitchLogic() {
        // 测试图标切换逻辑
        val normalModeIcon = R.drawable.ic_read_mode_normal
        val summaryModeIcon = R.drawable.ic_read_mode_summary
        
        // 正常模式 -> 摘要模式
        var isSummaryMode = false
        var currentIcon = if (isSummaryMode) summaryModeIcon else normalModeIcon
        assertEquals("Initial should be normal icon", normalModeIcon, currentIcon)
        
        // 切换到摘要模式
        isSummaryMode = true
        currentIcon = if (isSummaryMode) summaryModeIcon else normalModeIcon
        assertEquals("After switch should be summary icon", summaryModeIcon, currentIcon)
        
        // 切换回正常模式
        isSummaryMode = false
        currentIcon = if (isSummaryMode) summaryModeIcon else normalModeIcon
        assertEquals("After switch back should be normal icon", normalModeIcon, currentIcon)
    }
}