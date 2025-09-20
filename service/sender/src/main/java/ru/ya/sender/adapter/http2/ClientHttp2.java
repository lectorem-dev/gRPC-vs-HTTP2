package ru.ya.sender.adapter.http2;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import ru.ya.libs.FleasAnswerWithMetricsDto;
import ru.ya.libs.FleasProblemDto;

@Component
public class ClientHttp2 {
    private final WebClient webClient;

    public ClientHttp2() {
        this.webClient = WebClient.builder()
                .baseUrl("http://receiver:8080/api") // прямо здесь указываем receiver
                .build();
    }

    public Flux<FleasAnswerWithMetricsDto> sendProblems(Flux<FleasProblemDto> problems) {
        long startTotal = System.nanoTime();

        long serializationStart = System.nanoTime();
        // Здесь мы замеряем время подготовки body для WebClient
        Flux<FleasAnswerWithMetricsDto> responseFlux = webClient.post()
                .uri("/fleas/sum")
                .body(problems, FleasProblemDto.class)
                .exchangeToFlux(response -> response.bodyToFlux(FleasAnswerWithMetricsDto.class));
        long serializationTime = System.nanoTime() - serializationStart;

        return responseFlux.map(dto -> {
            long totalTime = System.nanoTime() - startTotal;
            dto.setSerializationTimeNs(serializationTime); // записываем прямо в ответ
            long networkTime = totalTime - dto.getSerializationTimeNs() - dto.getDeserializationTimeNs();
            dto.setNetworkTimeNs(networkTime);
            dto.setTotalTimeNs(totalTime);
            return dto;
        });
    }
}
