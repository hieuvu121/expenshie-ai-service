package com.be9expensphie.ai.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;

@Configuration
public class OpenAiRestClientConfig {

    @Bean
    public RestClientCustomizer removeExtraBodyCustomizer() {
        ObjectMapper mapper = new ObjectMapper();
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            if (body != null && body.length > 0) {
                try {
                    JsonNode node = mapper.readTree(body);
                    if (node instanceof ObjectNode objectNode && objectNode.has("extra_body")) {
                        objectNode.remove("extra_body");
                        byte[] newBody = mapper.writeValueAsBytes(node);
                        HttpRequest wrapped = new HttpRequestWrapper(request) {
                            @Override
                            public HttpHeaders getHeaders() {
                                HttpHeaders headers = new HttpHeaders();
                                headers.addAll(super.getHeaders());
                                headers.setContentLength(newBody.length);
                                return headers;
                            }
                        };
                        return execution.execute(wrapped, newBody);
                    }
                } catch (Exception ignored) {}
            }
            return execution.execute(request, body);
        });
    }
}
