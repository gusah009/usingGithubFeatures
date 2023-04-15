package com.example.demo.chatgpt.config

import com.example.demo.chatgpt.service.ChatGptService
import com.example.demo.chatgpt.service.PapagoDetectLanguageService
import com.example.demo.chatgpt.service.PapagoService
import com.slack.api.bolt.App
import com.slack.api.bolt.AppConfig
import com.slack.api.bolt.context.builtin.SlashCommandContext
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.util.SlackRequestParser
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SlackConfig(
    private val papagoService: PapagoService,
    private val detectLanguageService: PapagoDetectLanguageService,
    private val chatGptService: ChatGptService
) {
    @Value("\${slack.token}")
    private val slackToken: String? = null

    @Value("\${slack.signingSecret}")
    private val signingSecret: String? = null

    @Value("\${slack.channelId}")
    private val channelId: String? = null

    @Bean
    fun app(): App {
        val appConfig = AppConfig.builder().singleTeamBotToken(slackToken).signingSecret(signingSecret).build()
        val app = App(appConfig)
        app.command("/gpt") { payload: SlashCommandRequest, ctx: SlashCommandContext ->
            val pl = payload.payload
            var msg = pl.text
            if (detectLanguageService.detect(msg) == PapagoService.Language.KOREAN) {
                println("========================")
                println("this is translate msg~!!")
                println(msg)
                msg = papagoService.translate(PapagoService.Language.KOREAN, PapagoService.Language.ENGLISH, msg)
                println(msg)
                println("========================")
            }
            println("========================")
            println("this is translate answer~!!")
            val answer = chatGptService.send(msg)
            println(answer)
            val koreanAnswer = papagoService.translate(PapagoService.Language.ENGLISH, PapagoService.Language.KOREAN, answer)
            println(koreanAnswer)
            println("========================")
            ctx.client().chatPostMessage { r: ChatPostMessageRequestBuilder ->
                r.channel(channelId)
                    .text(
                        """
    <@${pl.userId}> : ${pl.text}
    
    $koreanAnswer
    """.trimIndent()
                    )
            }
            ctx.ack()
        }
        return app
    }

    @Bean
    fun slackRequestParser(): SlackRequestParser {
        return SlackRequestParser(app().config())
    }
}
