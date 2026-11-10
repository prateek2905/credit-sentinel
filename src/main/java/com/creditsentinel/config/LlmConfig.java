package com.creditsentinel.config;

import com.creditsentinel.agent.ScriptedCreditChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LlmConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "creditsentinel.llm-mode", havingValue = "scripted", matchIfMissing = true)
    ChatModel scriptedCreditChatModel(ObjectMapper objectMapper) {
        return new ScriptedCreditChatModel(objectMapper);
    }
}
