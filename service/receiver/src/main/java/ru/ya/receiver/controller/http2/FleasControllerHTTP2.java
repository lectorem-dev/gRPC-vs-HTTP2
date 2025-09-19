package ru.ya.receiver.controller.http2;


import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ya.libs.FleasAnswerDto;
import ru.ya.libs.FleasProblemDto;
import ru.ya.receiver.service.FleasService;

@RestController
@RequestMapping("/api/fleas")
@RequiredArgsConstructor
public class FleasControllerHTTP2 {
    private final FleasService fleasService;

    @PostMapping(value = "/sum",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Flux<FleasAnswerDto> calculateFleasSum(@RequestBody Flux<FleasProblemDto> problems) {
        return problems.map(fleasService::calculateMinimalPathSum);
    }
}
