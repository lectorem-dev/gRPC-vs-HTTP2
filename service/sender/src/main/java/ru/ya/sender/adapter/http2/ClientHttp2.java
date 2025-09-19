package ru.ya.sender.adapter.http2;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import ru.ya.libs.FleasAnswerDto;
import ru.ya.libs.FleasProblemDto;

@Component
public class ClientHttp2 {
    private final WebClient webClient;

    public ClientHttp2() {
        this.webClient = WebClient.builder()
                .baseUrl("http://receiver:8080/api") // прямо здесь указываем receiver
                .build();
    }

    public Flux<FleasAnswerDto> sendProblems(Flux<FleasProblemDto> problems) {
        return webClient.post()
                .uri("/fleas/sum")
                .body(problems, FleasProblemDto.class)
                .retrieve()
                .bodyToFlux(FleasAnswerDto.class);
    }
}
