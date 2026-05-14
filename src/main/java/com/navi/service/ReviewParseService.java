package com.navi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navi.config.LlmConfig;
import com.navi.util.HttpClientUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewParseService {

    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String systemPrompt;

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getResourceAsStream("/parse-prompt.txt")) {
            systemPrompt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            log.info("System prompt loaded, length={}", systemPrompt.length());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load parse-prompt.txt", e);
        }
    }

    public List<ParsedShop> parse(String reviewText) {
        try {
            log.info("=== ENCODING DEBUG ===");
            log.info("file.encoding: {}", System.getProperty("file.encoding"));
            log.info("default charset: {}", java.nio.charset.Charset.defaultCharset());
            log.info("reviewText first 80 chars: {}", reviewText.substring(0, Math.min(80, reviewText.length())));
            log.info("systemPrompt first 80 chars: {}", systemPrompt.substring(0, Math.min(80, systemPrompt.length())));

            String url = llmConfig.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";

            String messagesJson = String.format(
                    "[{\"role\":\"system\",\"content\":%s},{\"role\":\"user\",\"content\":%s}]",
                    escapeJson(systemPrompt),
                    escapeJson(reviewText)
            );

            String requestBody = String.format(
                    "{\"model\":\"%s\",\"messages\":%s,\"temperature\":0.1}",
                    llmConfig.getModelName(),
                    messagesJson
            );

            log.info("Sending request to LLM, body length={}", requestBody.length());

            String response = HttpClientUtil.post(url, requestBody, llmConfig.getApiKey());
            JsonNode root = objectMapper.readTree(response);

            String content = root.path("choices").path(0).path("message").path("content").asText();
            log.info("LLM parsed result: {}", content);

            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            } else if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();

            JsonNode shops = objectMapper.readTree(content);
            List<ParsedShop> result = new ArrayList<>();
            for (JsonNode shop : shops) {
                result.add(new ParsedShop(
                        shop.path("name").asText(),
                        shop.path("address").asText(),
                        shop.path("city").asText()
                ));
            }
            return result;
        } catch (Exception e) {
            log.error("parse review failed: {}", e.getMessage(), e);
            throw new RuntimeException("解析评论文本失败: " + e.getMessage());
        }
    }

    private String escapeJson(String s) {
        try {
            return objectMapper.writeValueAsString(s);
        } catch (Exception e) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }

    @Data
    public static class ParsedShop {
        private final String name;
        private final String address;
        private final String city;

        public ParsedShop(String name, String address, String city) {
            this.name = name;
            this.address = address;
            this.city = city;
        }
    }
}
