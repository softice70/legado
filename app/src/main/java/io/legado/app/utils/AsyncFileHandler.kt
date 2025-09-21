package io.legado.app.utils

import io.legado.app.help.globalExecutor
import java.io.UnsupportedEncodingException
import java.util.logging.FileHandler
import java.util.logging.LogRecord

class AsyncFileHandler(pattern: String) : FileHandler(pattern) {

    constructor(pattern: String, encoding: String) : this(pattern) {
        try {
            // 使用反射设置编码，确保FileHandler使用UTF-8
            val field = FileHandler::class.java.getDeclaredField("outputEncoding")
            field.isAccessible = true
            field.set(this, encoding)
        } catch (e: Exception) {
            // 如果反射失败，不影响正常功能
            e.printStackTrace()
        }
    }

    override fun publish(record: LogRecord?) {
        if (!isLoggable(record)) {
            return
        }
        globalExecutor.execute {
            super.publish(record)
        }
    }

}
