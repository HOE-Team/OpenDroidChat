package com.hoeteam.opendroidchat

import com.hoeteam.opendroidchat.network.LlmApiService
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomUrlTest {

    private val service = LlmApiService()

    @Test
    fun acceptsV1Base() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            service.normalizeCustomUrl("https://example.com/v1")
        )
    }

    @Test
    fun acceptsHostOnly() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            service.normalizeCustomUrl("https://example.com")
        )
    }

    @Test
    fun acceptsOpenAiCompatibleBase() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            service.normalizeCustomUrl("https://generativelanguage.googleapis.com/v1beta/openai")
        )
    }

    @Test
    fun keepsFullEndpointAndQuery() {
        assertEquals(
            "https://example.openai.azure.com/openai/deployments/d/chat/completions?api-version=2024-10-21",
            service.normalizeCustomUrl(
                "https://example.openai.azure.com/openai/deployments/d/chat/completions?api-version=2024-10-21"
            )
        )
    }
}
