package com.be9expensphie.ai.producer;

import com.be9expensphie.common.event.AiResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiResponseEventProducer {
    private final KafkaTemplate<String, AiResponseEvent> aiResponseKafkaTemplate;

    public void publish(AiResponseEvent response, Iterable<Header> requestHeaders) {
        ProducerRecord<String, AiResponseEvent> record = new ProducerRecord<>("ai-response-events", null, response);
        for (Header header : requestHeaders) {
            record.headers().add(header);
        }
        aiResponseKafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish AI response to ai-response-events", ex);
                    }
                });
    }
}
