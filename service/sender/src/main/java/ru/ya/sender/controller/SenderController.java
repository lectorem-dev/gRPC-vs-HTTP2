package ru.ya.sender.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import ru.ya.libs.FleasAnswerWithMetricsDto;
import ru.ya.libs.FleasProblemDto;
import ru.ya.sender.adapter.grpc.ClientGRPC;
import ru.ya.sender.adapter.http2.ClientHttp2;

@RestController
@RequestMapping("/api/sender")
@RequiredArgsConstructor
public class SenderController {
    private static final Logger log = LoggerFactory.getLogger(SenderController.class);

    private final ClientHttp2 clientHttp2;
    private final ClientGRPC clientGrpc;

    @PostMapping("/send")
    public Flux<FleasAnswerWithMetricsDto> sendProblems(
            @RequestParam(defaultValue = "http") String protocol,
            @RequestBody Flux<FleasProblemDto> problems) {

        long startTime = System.nanoTime(); // начало измерения

        Flux<FleasAnswerWithMetricsDto> responseFlux;

        if ("grpc".equalsIgnoreCase(protocol)) {
            log.debug("Protocol selected: gRPC");
            responseFlux = clientGrpc.sendProblems(problems);
        } else {
            log.debug("Protocol selected: HTTP2");
            responseFlux = clientHttp2.sendProblems(problems);
        }

        return responseFlux.map(dto -> {
            long totalTimeNs = System.nanoTime() - startTime;
            dto.setTotalTimeNs(totalTimeNs); // добавляем totalTimeNs
            return dto;
        });
    }
}