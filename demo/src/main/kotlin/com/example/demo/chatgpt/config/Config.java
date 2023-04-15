package com.example.demo.chatgpt.config;

import com.theokanning.openai.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Value("${chatgpt.token}")
    private String chatGptToken;

    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(chatGptToken);
    }
}
