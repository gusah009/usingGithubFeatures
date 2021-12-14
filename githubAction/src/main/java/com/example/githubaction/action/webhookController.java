package com.example.githubaction.action;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class webhookController {

    @PostMapping
    public void postWebhook() {

    }
}
