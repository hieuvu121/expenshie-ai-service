package com.be9expensphie.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient chatClient;

    public String parseExpense(String prompt) {
        log.info("Sending prompt to OpenAI");
        String content= chatClient.prompt()
                .system("""                                                                                                                                                                                                            
                          You are an expense parsing assistant.
                          Parse the user description into a JSON object with exactly these fields:                                                                                                                                       
                          amount (number), category (string), description (string),
                          currency (string, default AUD), method (EQUAL or AMOUNT),
                          date (YYYY-MM-DD, default today if not mentioned).
                          Return ONLY valid JSON — no markdown, no explanation, no code block.                                                                                                                                           
                          """)
                .user(prompt)
                .call()
                .content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI returned empty response for expense parsing");
        }
        return content;
    }

    public String generateSuggestion(String prompt) {
        log.info("Generating expense suggestion via OpenAI");
        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OpenAI returned empty response for suggestion");
        }
        return content;
    }
}
