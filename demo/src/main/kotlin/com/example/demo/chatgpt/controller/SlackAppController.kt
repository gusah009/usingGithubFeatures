package com.example.demo.chatgpt.controller

import com.slack.api.bolt.App
import com.slack.api.bolt.request.Request
import com.slack.api.bolt.request.RequestHeaders
import com.slack.api.bolt.response.Response
import com.slack.api.bolt.util.QueryStringParser
import com.slack.api.bolt.util.SlackRequestParser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

@RestController
class SlackAppController(private val app: App, private val requestParser: SlackRequestParser) {
    @PostMapping("/chat-gpt")
    @Throws(IOException::class)
    fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        val slackReq = buildSlackRequest(req)
        if (slackReq != null) {
            try {
                val slackResp = app.run(slackReq)
                writeResponse(resp, slackResp)
            } catch (e: Exception) {
                System.out.printf("Failed to handle a request - {} %n", e.message)
                resp.status = 500
                resp.contentType = "application/json"
                resp.writer.write("{\"error\":\"Something is wrong\"}")
            }
        } else {
            writeResponse(resp, Response.builder().statusCode(400).body("Invalid Request").build())
        }
    }

    @Throws(IOException::class)
    fun buildSlackRequest(req: HttpServletRequest): Request<*> {
        val requestBody = doReadRequestBodyAsString(req)
        val headers = RequestHeaders(toHeaderMap(req))
        val rawRequest = SlackRequestParser.HttpRequest.builder()
            .requestUri(req.requestURI)
            .queryString(QueryStringParser.toMap(req.queryString))
            .headers(headers)
            .requestBody(requestBody)
            .remoteAddress(req.remoteAddr)
            .build()
        return requestParser.parse(rawRequest)
    }

    companion object {
        @Throws(IOException::class)
        fun doReadRequestBodyAsString(req: HttpServletRequest): String {
            return req.reader.lines().collect(Collectors.joining(System.lineSeparator()))
        }

        fun toHeaderMap(req: HttpServletRequest): Map<String, List<String>> {
            val headers: MutableMap<String, List<String>> = HashMap()
            val names = req.headerNames
            while (names.hasMoreElements()) {
                val name = names.nextElement()
                val values: List<String> = Collections.list(req.getHeaders(name))
                headers[name] = values
            }
            return headers
        }

        @Throws(IOException::class)
        fun writeResponse(resp: HttpServletResponse, slackResp: Response) {
            resp.status = slackResp.statusCode
            for ((name, value1) in slackResp.headers) {
                for (value in value1) {
                    resp.addHeader(name, value)
                }
            }
            resp.setHeader("Content-Type", slackResp.contentType)
            if (slackResp.body != null) {
                resp.writer.write(slackResp.body)
            }
        }
    }
}
