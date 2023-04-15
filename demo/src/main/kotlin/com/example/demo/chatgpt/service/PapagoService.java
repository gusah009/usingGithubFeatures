package com.example.demo.chatgpt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PapagoService {

    @Value("${papago.clientId}")
    private String clientId;
    @Value("${papago.clientSecret}")
    private String clientSecret;

    private final ObjectMapper objectMapper;

    public PapagoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String translate(Language source, Language target, String message) {
        String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
        String text = URLEncoder.encode(message, StandardCharsets.UTF_8);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);

        String responseBody = post(source, target, apiURL, requestHeaders, text);
        PapagoResponse papagoResponse = null;
        try {
            papagoResponse = objectMapper.readValue(responseBody, PapagoResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return papagoResponse.message.result.translatedText;
    }

    private static String post(Language src, Language dst, String apiUrl, Map<String, String> requestHeaders, String text) {
        HttpURLConnection con = connect(apiUrl);
        String postParams = "source=" + src.getValue() + "&target=" + dst.getValue() + "&text=" + text; //원본언어: 한국어 (ko) -> 목적언어: 영어 (en)
        try {
            con.setRequestMethod("POST");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postParams.getBytes());
                wr.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 응답
                return readBody(con.getInputStream());
            } else {  // 에러 응답
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private static HttpURLConnection connect(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    private static String readBody(InputStream body) {
        InputStreamReader streamReader = new InputStreamReader(body);

        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();

            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }

            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
        }
    }

    public enum Language {
        KOREAN("ko"),
        ENGLISH("en"),
        ;

        private final String value;

        Language(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    static class PapagoResponse {

        private Message message;

        public Message getMessage() {
            return message;
        }

        static class Message {

            private Result result;

            public Result getResult() {
                return result;
            }

            static class Result {

                private String translatedText;

                public String getTranslatedText() {
                    return translatedText;
                }
            }
        }
    }
}
