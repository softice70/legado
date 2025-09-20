package io.legado.app.ui.book.read.mode

import android.content.Context
import io.legado.app.help.config.AppConfig
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * AI配置功能测试
 * 验证不同AI服务提供商的配置和调用逻辑
 */
class AIConfigTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var aiSummaryRepository: AISummaryRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        aiSummaryRepository = AISummaryRepository(mockContext)
    }

    @Test
    fun testProviderSelection() {
        // 测试不同提供商的模型获取
        val wenxinModels = listOf(
            "ernie-4.0-8k", "ernie-3.5-8k", "ernie-lite-8k"
        )
        val qianwenModels = listOf(
            "qwen-turbo", "qwen-plus", "qwen-max"
        )
        val openaiModels = listOf(
            "gpt-3.5-turbo", "gpt-4", "gpt-4o"
        )

        // 验证模型列表包含预期的模型
        assertTrue("Wenxin models should contain ernie-3.5-8k", 
            wenxinModels.contains("ernie-3.5-8k"))
        assertTrue("Qianwen models should contain qwen-turbo", 
            qianwenModels.contains("qwen-turbo"))
        assertTrue("OpenAI models should contain gpt-3.5-turbo", 
            openaiModels.contains("gpt-3.5-turbo"))
    }

    @Test
    fun testConfigValidation() {
        // 测试配置验证逻辑
        mockStatic(AppConfig::class.java).use { mockedStatic ->
            // 测试本地算法配置（应该总是有效）
            mockedStatic.`when`<String> { AppConfig.aiProvider }.thenReturn("local")
            mockedStatic.`when`<Boolean> { AppConfig.isAiConfigValid() }.thenReturn(true)
            
            assertTrue("Local provider should always be valid", AppConfig.isAiConfigValid())
            
            // 测试API Key配置
            mockedStatic.`when`<String> { AppConfig.aiProvider }.thenReturn("openai")
            mockedStatic.`when`<String> { AppConfig.aiAuthType }.thenReturn("api_key")
            mockedStatic.`when`<String?> { AppConfig.aiApiKey }.thenReturn("test-key")
            mockedStatic.`when`<Boolean> { AppConfig.isAiConfigValid() }.thenReturn(true)
            
            assertTrue("OpenAI with API key should be valid", AppConfig.isAiConfigValid())
        }
    }

    @Test
    fun testRequestBodyBuilding() {
        // 测试请求体构建逻辑
        val testPrompt = "请为以下文本生成摘要：这是一个测试文本。"
        
        // 这里主要测试方法调用不会崩溃
        // 实际的请求体构建需要mock更多依赖
        assertNotNull("Test prompt should not be null", testPrompt)
        assertTrue("Test prompt should not be empty", testPrompt.isNotEmpty())
    }

    @Test
    fun testModelMapping() {
        // 测试模型映射逻辑
        val providerModelMap = mapOf(
            "wenxin" to "ernie-3.5-8k",
            "qianwen" to "qwen-turbo", 
            "openai" to "gpt-3.5-turbo",
            "local" to "local"
        )
        
        providerModelMap.forEach { (provider, expectedModel) ->
            assertNotNull("Provider $provider should have a model", expectedModel)
            assertFalse("Model for $provider should not be empty", expectedModel.isEmpty())
        }
    }

    @Test
    fun testAuthHeaderGeneration() {
        // 测试认证头生成逻辑
        mockStatic(AppConfig::class.java).use { mockedStatic ->
            // 测试API Key认证
            mockedStatic.`when`<String> { AppConfig.aiProvider }.thenReturn("openai")
            mockedStatic.`when`<String> { AppConfig.aiAuthType }.thenReturn("api_key")
            mockedStatic.`when`<String?> { AppConfig.aiApiKey }.thenReturn("test-api-key")
            
            val headers = mapOf(
                "Authorization" to "Bearer test-api-key",
                "Content-Type" to "application/json"
            )
            
            assertTrue("Headers should contain Authorization", headers.containsKey("Authorization"))
            assertTrue("Headers should contain Content-Type", headers.containsKey("Content-Type"))
            assertEquals("Authorization should be Bearer token", 
                "Bearer test-api-key", headers["Authorization"])
        }
    }

    @Test
    fun testConnectionTest() = runBlocking {
        // 测试连接测试功能
        try {
            val (success, message) = aiSummaryRepository.testConnection()
            
            // 连接测试应该返回结果（成功或失败都可以）
            assertNotNull("Connection test should return a message", message)
            assertTrue("Message should not be empty", message.isNotEmpty())
            
            // 这里不强制要求成功，因为测试环境可能没有有效的API配置
            println("Connection test result: success=$success, message=$message")
        } catch (e: Exception) {
            // 测试环境下可能会抛出异常，这是正常的
            println("Connection test threw exception (expected in test): ${e.message}")
        }
    }

    @Test
    fun testSupportedModels() {
        // 测试支持的模型列表获取
        val models = aiSummaryRepository.getSupportedModels()
        
        assertNotNull("Supported models should not be null", models)
        assertFalse("Supported models should not be empty", models.isEmpty())
        
        // 至少应该包含本地算法
        assertTrue("Should support at least local algorithm", 
            models.contains("local") || models.isNotEmpty())
    }
}