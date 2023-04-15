package com.example.demo.chatgpt.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class PapagoService(private val objectMapper: ObjectMapper) {
    @Value("\${papago.clientId}")
    private val clientId: String? = null

    @Value("\${papago.clientSecret}")
    private val clientSecret: String? = null
    fun translate(source: Language, target: Language, message: String?): String? {
        val apiURL = "https://openapi.naver.com/v1/papago/n2mt"
        val text = URLEncoder.encode(message, StandardCharsets.UTF_8)
        val requestHeaders: MutableMap<String, String?> = HashMap()
        requestHeaders["X-Naver-Client-Id"] = clientId
        requestHeaders["X-Naver-Client-Secret"] = clientSecret
        val responseBody = post(source, target, apiURL, requestHeaders, text)
        val papagoResponse = try {
            objectMapper.readValue(responseBody, PapagoResponse::class.java)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
        return papagoResponse.message!!.result!!.translatedText
    }

    enum class Language(val value: String) {
        KOREAN("ko"),
        ENGLISH("en")

    }

    internal class PapagoResponse {
        val message: Message? = null

        internal class Message {
            val result: Result? = null

            internal class Result {
                val translatedText: String? = null
            }
        }
    }

    companion object {
        private fun post(src: Language, dst: Language, apiUrl: String, requestHeaders: Map<String, String?>, text: String): String {
            val con = connect(apiUrl)
            val postParams = "source=" + src.value + "&target=" + dst.value + "&text=" + text //원본언어: 한국어 (ko) -> 목적언어: 영어 (en)
            return try {
                con.requestMethod = "POST"
                for ((key, value) in requestHeaders) {
                    con.setRequestProperty(key, value)
                }
                con.doOutput = true
                DataOutputStream(con.outputStream).use { wr ->
                    wr.write(postParams.toByteArray())
                    wr.flush()
                }
                val responseCode = con.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 응답
                    readBody(con.inputStream)
                } else {  // 에러 응답
                    readBody(con.errorStream)
                }
            } catch (e: IOException) {
                throw RuntimeException("API 요청과 응답 실패", e)
            } finally {
                con.disconnect()
            }
        }

        private fun connect(apiUrl: String): HttpURLConnection {
            return try {
                val url = URL(apiUrl)
                url.openConnection() as HttpURLConnection
            } catch (e: MalformedURLException) {
                throw RuntimeException("API URL이 잘못되었습니다. : $apiUrl", e)
            } catch (e: IOException) {
                throw RuntimeException("연결이 실패했습니다. : $apiUrl", e)
            }
        }

        private fun readBody(body: InputStream): String {
            val streamReader = InputStreamReader(body)
            try {
                BufferedReader(streamReader).use { lineReader ->
                    val responseBody = StringBuilder()
                    var line: String?
                    while (lineReader.readLine().also { line = it } != null) {
                        responseBody.append(line)
                    }
                    return responseBody.toString()
                }
            } catch (e: IOException) {
                throw RuntimeException("API 응답을 읽는데 실패했습니다.", e)
            }
        }
    }
}
