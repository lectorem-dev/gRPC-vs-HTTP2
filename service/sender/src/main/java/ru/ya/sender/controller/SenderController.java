package ru.ya.sender.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import ru.ya.libs.FleasAnswerDto;
import ru.ya.libs.FleasProblemDto;
import ru.ya.sender.adapter.http2.ClientHttp2;

@RestController
@RequestMapping("/api/sender")
@RequiredArgsConstructor
public class SenderController {

    private final ClientHttp2 clientHttp2;

    @PostMapping("/send")
    public Flux<FleasAnswerDto> sendProblems(@RequestBody Flux<FleasProblemDto> problems) {
        return clientHttp2.sendProblems(problems);
    }
}