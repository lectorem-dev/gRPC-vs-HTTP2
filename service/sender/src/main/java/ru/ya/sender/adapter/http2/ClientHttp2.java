package ru.ya.sender.adapter.http2;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.ya.libs.FleasProblemDto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;


@Component
public class ClientHttp2 {
    private final WebClient webClient;

    public ClientHttp2() {
        this.webClient = WebClient.builder()
                .baseUrl("http://receiver:8080/api") // прямо здесь указываем receiver
                .build();
    }

    public Mono<Long> sendProblems(List<FleasProblemDto> problems) {
        Instant start = Instant.now();

        return Mono.when(
                problems.stream()
                        .map(problem -> webClient.post()
                                .uri("/fleas/sum")
                                .bodyValue(problem)
                                .retrieve()
                                .bodyToMono(Long.class))
                        .toList()
        ).then(Mono.fromCallable(() -> Duration.between(start, Instant.now()).toMillis()));
    }
}
