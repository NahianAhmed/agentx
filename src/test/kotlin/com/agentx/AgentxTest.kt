package com.agentx

import com.agentx.controller.ChatController
import com.agentx.dto.ChatRequest
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import jakarta.inject.Inject

@MicronautTest
class AgentxTest {

    @Inject
    lateinit var application: EmbeddedApplication<*>

    @Inject
    lateinit var chatController: ChatController

    @Test
    fun testItWorks() {
        Assertions.assertTrue(application.isRunning)
    }

    @Test
    fun testChatControllerExists() {
        Assertions.assertNotNull(chatController)
    }
}

