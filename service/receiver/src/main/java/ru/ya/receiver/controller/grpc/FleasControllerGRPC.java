package ru.ya.receiver.controller.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ya.libs.FleaPositionDto;
import ru.ya.libs.FleasAnswerDto;
import ru.ya.libs.FleasProblemDto;
import ru.ya.libs.grpc.FleasAnswer;
import ru.ya.libs.grpc.FleasAnswerWithMetrics;

import ru.ya.libs.grpc.FleasProblem;
import ru.ya.libs.grpc.FleasServiceGrpc;
import ru.ya.receiver.service.FleasService;

import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class FleasControllerGRPC extends FleasServiceGrpc.FleasServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(FleasControllerGRPC.class);

    private final FleasService fleasService;

    @Override
    public StreamObserver<FleasProblem> calculate(StreamObserver<FleasAnswerWithMetrics> responseObserver) {
        log.info("gRPC StreamObserver создан: получаем задачи через gRPC");

        return new StreamObserver<>() {
            @Override
            public void onNext(FleasProblem fleasProblem) {
                log.info("Получена задача: n={}, m={}, fleasCount={}", fleasProblem.getN(),
                        fleasProblem.getM(), fleasProblem.getFleas6Count());

                long deserializationStart = System.nanoTime();

                FleasProblemDto dto = FleasProblemDto.builder()
                        .n(fleasProblem.getN())
                        .m(fleasProblem.getM())
                        .feederRow(fleasProblem.getFeederRow())
                        .feederCol(fleasProblem.getFeederCol())
                        .fleasCount(fleasProblem.getFleas6Count())
                        .fleas(fleasProblem.getFleas6List().stream()
                                .map(fp -> FleaPositionDto.builder()
                                        .row(fp.getRow())
                                        .col(fp.getCol())
                                        .build())
                                .collect(Collectors.toList()))
                        .build();

                long deserializationTime = System.nanoTime() - deserializationStart;
                log.info("Deserialization time: {} ns", deserializationTime);

                FleasAnswerDto answerDto = fleasService.calculateMinimalPathSum(dto);

                long serializationStart = System.nanoTime();

                FleasAnswer answer = FleasAnswer.newBuilder()
                        .setResult(answerDto.getResult())
                        .setDurationNs(answerDto.getDurationNs())
                        .build();

                FleasAnswerWithMetrics answerWithMetrics = FleasAnswerWithMetrics.newBuilder()
                        .setAnswer(answer)
                        .setDeserializationTimeNs(deserializationTime)
                        .setSerializationTimeNs(System.nanoTime() - serializationStart)
                        .build();

                log.info("Отправляем ответ: result={}, durationMs={}", answer.getResult(), answer.getDurationNs());

                responseObserver.onNext(answerWithMetrics);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Ошибка gRPC: {}", t.getMessage(), t);
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                log.info("gRPC поток завершён");
                responseObserver.onCompleted();
            }
        };
    }
}