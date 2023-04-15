package com.example.demo.chatgpt.service;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ChatGptService {

    private final OpenAiService openAiService;

    public ChatGptService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String send(String message) {
        CompletionRequest completionRequest = CompletionRequest.builder()
                .prompt(message)
                .model("text-davinci-003")
                .build();
        return openAiService.createCompletion(completionRequest).getChoices().stream().map(CompletionChoice::getText)
                .collect(Collectors.joining());
    }
}
