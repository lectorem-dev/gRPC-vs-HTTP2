package ru.ya.sender.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.ya.libs.FleasProblemDto;
import ru.ya.sender.adapter.http2.ClientHttp2;

import java.util.List;

@RestController
@RequestMapping("/api/sender")
@RequiredArgsConstructor
public class SenderController {

    private final ClientHttp2 clientHttp2;

    @PostMapping("/send")
    public Mono<Long> sendProblems(@RequestBody List<FleasProblemDto> problems) {
        return clientHttp2.sendProblems(problems);
    }
}