package ru.ya.receiver.controller.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ya.libs.FleasAnswerDto;
import ru.ya.libs.FleasProblemDto;
import ru.ya.libs.grpc.FleasAnswer;
import ru.ya.libs.grpc.FleasProblem;
import ru.ya.libs.grpc.FleasServiceGrpcGrpc;
import ru.ya.receiver.service.FleasService;

import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class FleasControllerGRPC extends FleasServiceGrpcGrpc.FleasServiceGrpcImplBase {
    private static final Logger log = LoggerFactory.getLogger(FleasControllerGRPC.class);

    private final FleasService fleasService;

    @Override
    public StreamObserver<FleasProblem> calculate(StreamObserver<FleasAnswer> responseObserver) {
        log.info("Создан gRPC StreamObserver: получаем задачи через gRPC");

        return new StreamObserver<>() {
            @Override
            public void onNext(FleasProblem fleasProblem) {
                // конвертация в DTO
                FleasProblemDto dto = FleasProblemDto.builder()
                        .n(fleasProblem.getN())
                        .m(fleasProblem.getM())
                        .feederRow(fleasProblem.getFeederRow())
                        .feederCol(fleasProblem.getFeederCol())
                        .fleasCount(fleasProblem.getFleasCount5())
                        .fleas(fleasProblem.getFleas6List().stream()
                                .map(fp -> ru.ya.libs.FleaPositionDto.builder()
                                        .row(fp.getRow())
                                        .col(fp.getCol())
                                        .build())
                                .collect(Collectors.toList()))
                        .build();

                FleasAnswerDto answerDto = fleasService.calculateMinimalPathSum(dto);

                // конвертация в gRPC ответ
                FleasAnswer answer = FleasAnswer.newBuilder()
                        .setResult(answerDto.getResult())
                        .setDurationMs(answerDto.getDurationMs())
                        .build();

                responseObserver.onNext(answer);
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}