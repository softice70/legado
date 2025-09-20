package io.legado.app.ui.book.read.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import io.legado.app.R

/**
 * 摘要加载视图
 * 显示AI摘要生成过程中的加载状态和错误提示
 */
class SummaryLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val progressBar: ProgressBar
    private val loadingText: TextView
    private val errorView: View
    private val errorText: TextView
    private val retryButton: TextView

    var onRetryClickListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_summary_loading, this, true)
        
        progressBar = findViewById(R.id.progress_bar)
        loadingText = findViewById(R.id.loading_text)
        errorView = findViewById(R.id.error_view)
        errorText = findViewById(R.id.error_text)
        retryButton = findViewById(R.id.retry_button)

        retryButton.setOnClickListener {
            onRetryClickListener?.invoke()
        }

        // 默认隐藏
        hide()
    }

    /**
     * 显示加载中状态
     */
    fun showLoading(message: String = "正在生成摘要...") {
        visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        errorView.visibility = View.GONE
        
        loadingText.text = message
    }

    /**
     * 显示错误状态
     */
    fun showError(errorMessage: String) {
        visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        loadingText.visibility = View.GONE
        errorView.visibility = View.VISIBLE
        
        errorText.text = errorMessage
    }

    /**
     * 隐藏加载视图
     */
    fun hide() {
        visibility = View.GONE
    }

    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean {
        return visibility == View.VISIBLE
    }

    /**
     * 设置加载文本
     */
    fun setLoadingText(text: String) {
        loadingText.text = text
    }

    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String {
        return errorText.text.toString()
    }

    /**
     * 预定义的加载状态
     */
    companion object {
        const val LOADING_GENERATING = "正在生成摘要..."
        const val LOADING_CACHING = "正在加载缓存..."
        const val ERROR_NETWORK = "网络连接失败，请检查网络后重试"
        const val ERROR_API_LIMIT = "API调用次数已达上限"
        const val ERROR_TIMEOUT = "请求超时，请稍后重试"
        const val ERROR_UNKNOWN = "生成摘要失败，请重试"
    }
}

/**
 * 需要添加的布局文件：res/layout/view_summary_loading.xml
 * 
 * 布局内容：
 * - ProgressBar（居中旋转）
 * - TextView（加载提示文字）
 * - 错误视图（包含错误图标、文字、重试按钮）
 * 
 * 样式：
 * - 半透明背景
 * - 圆角卡片样式
 * - 动画效果
 */