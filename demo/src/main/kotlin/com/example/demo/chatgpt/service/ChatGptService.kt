package com.example.demo.chatgpt.service

import com.theokanning.openai.OpenAiService
import com.theokanning.openai.completion.CompletionChoice
import com.theokanning.openai.completion.CompletionRequest
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class ChatGptService(private val openAiService: OpenAiService) {
    fun send(message: String?): String {
        val completionRequest = CompletionRequest.builder()
            .prompt(message)
            .model("text-davinci-003")
            .build()
        return openAiService.createCompletion(completionRequest).choices.stream().map { obj: CompletionChoice -> obj.text }
            .collect(Collectors.joining())
    }
}
