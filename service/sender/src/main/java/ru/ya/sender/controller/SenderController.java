package ru.ya.sender.controller;

import lombok.RequiredArgsConstructor;
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

    private final ClientHttp2 clientHttp2;
    private final ClientGRPC clientGrpc;

    @PostMapping("/send")
    public Flux<FleasAnswerDto> sendProblems(
            @RequestParam(defaultValue = "http") String protocol, // <-- выбор протокола
            @RequestBody Flux<FleasProblemDto> problems) {

        if ("grpc".equalsIgnoreCase(protocol)) {
            return clientGrpc.sendProblems(problems);
        } else {
            return clientHttp2.sendProblems(problems);
        }
    }
}