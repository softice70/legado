package io.legado.app.data.entities

import io.legado.app.help.book.BookHelp
import io.legado.app.model.ReadBook

/**
 * BookChapter扩展函数
 * 用于获取章节的原始文本内容
 */

/**
 * 获取章节的原始文本内容
 * @return 章节的原始文本内容
 */
suspend fun BookChapter.getRawText(): String {
    val book = ReadBook.book ?: return ""
    return try {
        BookHelp.getContent(book, this) ?: ""
    } catch (e: Exception) {
        ""
    }
}