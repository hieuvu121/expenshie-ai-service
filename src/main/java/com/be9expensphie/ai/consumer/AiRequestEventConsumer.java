package com.be9expensphie.ai.consumer;

import com.be9expensphie.common.event.AiRequestEvent;
import com.be9expensphie.common.event.AiRequestType;
import com.be9expensphie.common.event.AiResponseEvent;
import com.be9expensphie.ai.producer.AiResponseEventProducer;
import com.be9expensphie.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiRequestEventConsumer {
    private final AiService aiService;
    private final AiResponseEventProducer aiResponseEventProducer;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2),
            dltTopicSuffix = ".DLT",
            kafkaTemplate = "retryKafkaTemplate"
    )
    @KafkaListener(topics = "ai-request-events", containerFactory = "aiRequestKafkaListenerContainerFactory")
    //need ConsumerRequest for header checking correlation id
    public void consume(ConsumerRecord<String, AiRequestEvent> record){
        AiRequestEvent event = record.value();
        log.info("Received AI request: householdId={}", event.getHouseholdId());

        AiResponseEvent response;
        try {
            String result;
            if (event.getType() == AiRequestType.GENERATE_SUGGESTION) {
                result = aiService.generateSuggestion(event.getPrompt());
            } else {
                result = aiService.parseExpense(event.getPrompt());
            }
            response = AiResponseEvent.builder()
                    .parsedJson(result)
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("AI call failed: type={}, householdId={}", event.getType(), event.getHouseholdId(), e);
            response = AiResponseEvent.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
        aiResponseEventProducer.publish(response, record.headers());
    }

    @DltHandler
    public void handleDlt(ConsumerRecord<String, AiRequestEvent> record) {
        log.error("ai-request-events exhausted retries — DLT: householdId={}", record.value().getHouseholdId());
        aiResponseEventProducer.publish(
                AiResponseEvent.builder().success(false).errorMessage("AI request failed after retries").build(),
                record.headers()
        );
    }
}
