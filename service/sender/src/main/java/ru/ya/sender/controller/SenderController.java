package ru.ya.sender.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import ru.ya.libs.FleasAnswerDto;
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
    public Flux<FleasAnswerDto> sendProblems(
            @RequestParam(defaultValue = "http") String protocol, // <-- выбор протокола
            @RequestBody Flux<FleasProblemDto> problems) {

        if ("grpc".equalsIgnoreCase(protocol)) {
            log.info("Protocol selected: gRPC");
            return clientGrpc.sendProblems(problems);
        } else {
            log.info("Protocol selected: HTTP2");
            return clientHttp2.sendProblems(problems);
        }
    }
}