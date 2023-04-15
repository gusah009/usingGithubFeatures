package com.example.demo.chatgpt.controller;

import com.slack.api.bolt.App;
import com.slack.api.bolt.request.Request;
import com.slack.api.bolt.request.RequestHeaders;
import com.slack.api.bolt.response.Response;
import com.slack.api.bolt.util.QueryStringParser;
import com.slack.api.bolt.util.SlackRequestParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class SlackAppController {

    private final App app;
    private final SlackRequestParser requestParser;

    public SlackAppController(App app, SlackRequestParser requestParser) {
        this.app = app;
        this.requestParser = requestParser;
    }

    @PostMapping("/chat-gpt")
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Request<?> slackReq = buildSlackRequest(req);
        if (slackReq != null) {
            try {
                Response slackResp = app.run(slackReq);
                writeResponse(resp, slackResp);
            } catch (Exception e) {
                System.out.printf("Failed to handle a request - {} %n", e.getMessage());
                resp.setStatus(500);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\":\"Something is wrong\"}");
            }
        } else {
            writeResponse(resp, Response.builder().statusCode(400).body("Invalid Request").build());
        }
    }

    public Request<?> buildSlackRequest(HttpServletRequest req) throws IOException {
        String requestBody = doReadRequestBodyAsString(req);
        RequestHeaders headers = new RequestHeaders(toHeaderMap(req));
        SlackRequestParser.HttpRequest rawRequest = SlackRequestParser.HttpRequest.builder()
                .requestUri(req.getRequestURI())
                .queryString(QueryStringParser.toMap(req.getQueryString()))
                .headers(headers)
                .requestBody(requestBody)
                .remoteAddress(req.getRemoteAddr())
                .build();
        return requestParser.parse(rawRequest);
    }

    public static String doReadRequestBodyAsString(HttpServletRequest req) throws IOException {
        return req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    }

    public static Map<String, List<String>> toHeaderMap(HttpServletRequest req) {
        Map<String, List<String>> headers = new HashMap<>();
        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            List<String> values = Collections.list(req.getHeaders(name));
            headers.put(name, values);
        }
        return headers;
    }

    public static void writeResponse(HttpServletResponse resp, Response slackResp) throws IOException {
        resp.setStatus(slackResp.getStatusCode());
        for (Map.Entry<String, List<String>> header : slackResp.getHeaders().entrySet()) {
            String name = header.getKey();
            for (String value : header.getValue()) {
                resp.addHeader(name, value);
            }
        }
        resp.setHeader("Content-Type", slackResp.getContentType());
        if (slackResp.getBody() != null) {
            resp.getWriter().write(slackResp.getBody());
        }
    }
}
