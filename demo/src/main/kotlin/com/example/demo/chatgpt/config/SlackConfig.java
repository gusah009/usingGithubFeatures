package com.example.demo.chatgpt.config;

import static com.example.demo.chatgpt.service.PapagoService.Language.ENGLISH;
import static com.example.demo.chatgpt.service.PapagoService.Language.KOREAN;

import com.example.demo.chatgpt.service.ChatGptService;
import com.example.demo.chatgpt.service.PapagoDetectLanguageService;
import com.example.demo.chatgpt.service.PapagoService;
import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.util.SlackRequestParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackConfig {

    private final PapagoService papagoService;
    private final PapagoDetectLanguageService detectLanguageService;
    private final ChatGptService chatGptService;

    @Value("${slack.token}")
    private String slackToken;
    @Value("${slack.signingSecret}")
    private String signingSecret;
    @Value("${slack.channelId}")
    private String channelId;

    public SlackConfig(PapagoService papagoService, PapagoDetectLanguageService detectLanguageService, ChatGptService chatGptService) {
        this.papagoService = papagoService;
        this.detectLanguageService = detectLanguageService;
        this.chatGptService = chatGptService;
    }

    @Bean
    public App app() {
        AppConfig appConfig = AppConfig.builder().singleTeamBotToken(slackToken).signingSecret(signingSecret).build();
        App app = new App(appConfig);

        app.command("/gpt", (payload, ctx) -> {
            SlashCommandPayload pl = payload.getPayload();
            String msg = pl.getText();

            if (detectLanguageService.detect(msg) == KOREAN) {
                System.out.println("========================");
                System.out.println("this is translate msg~!!");
                System.out.println(msg);
                msg = papagoService.translate(KOREAN, ENGLISH, msg);
                System.out.println(msg);
                System.out.println("========================");
            }
            System.out.println("========================");
            System.out.println("this is translate answer~!!");
            String answer = chatGptService.send(msg);
            System.out.println(answer);
            String koreanAnswer = papagoService.translate(ENGLISH, KOREAN, answer);
            System.out.println(koreanAnswer);
            System.out.println("========================");
            ctx.client().chatPostMessage(r -> r.channel(channelId)
                    .text("<@" + pl.getUserId() + "> : " + pl.getText() + "\r\n\r\n" +
                            koreanAnswer));

            return ctx.ack();
        });

        return app;
    }

    @Bean
    public SlackRequestParser slackRequestParser() {
        return new SlackRequestParser(app().config());
    }
}
