package ru.ya.sender.adapter.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import ru.ya.libs.FleasAnswerDto;
import ru.ya.libs.FleasAnswerWithMetricsDto;
import ru.ya.libs.FleasProblemDto;
import ru.ya.libs.grpc.*;

@Component
public class ClientGRPC {
    private static final Logger log = LoggerFactory.getLogger(ClientGRPC.class);

    private final FleasServiceGrpc.FleasServiceStub asyncStub;

    public ClientGRPC() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        this.asyncStub = FleasServiceGrpc.newStub(channel);
    }

    public Flux<FleasAnswerWithMetricsDto> sendProblems(Flux<FleasProblemDto> problems) {
        long startTotal = System.nanoTime();

        return Flux.create(sink -> {
            StreamObserver<FleasAnswerWithMetrics> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(FleasAnswerWithMetrics value) {
                    long totalTime = System.nanoTime() - startTotal;
                    long networkTime = totalTime - value.getSerializationTimeNs() - value.getDeserializationTimeNs();

                    log.debug("Получен ответ: result={}, durationMs={}",
                            value.getAnswer().getResult(), value.getAnswer().getDurationNs());
                    log.debug("Metrics: serialization={} ns, deserialization={} ns, network={} ns, total={} ns",
                            value.getSerializationTimeNs(), value.getDeserializationTimeNs(), networkTime, totalTime);

                    sink.next(FleasAnswerWithMetricsDto.builder()
                            .answer(FleasAnswerDto.builder()
                                    .result(value.getAnswer().getResult())
                                    .durationNs(value.getAnswer().getDurationNs())
                                    .build())
                            .serializationTimeNs(value.getSerializationTimeNs())
                            .deserializationTimeNs(value.getDeserializationTimeNs())
                            .networkTimeNs(networkTime)
                            .totalTimeNs(totalTime)
                            .build());
                }

                @Override
                public void onError(Throwable t) {
                    log.error("Ошибка gRPC: {}", t.getMessage(), t);
                    sink.error(t);
                }

                @Override
                public void onCompleted() {
                    log.debug("gRPC поток завершён");
                    sink.complete();
                }
            };

            StreamObserver<FleasProblem> requestObserver = asyncStub.calculate(responseObserver);

            // TODO: не мешает ли это честным испытаниям?
            problems.subscribe(dto -> {
                long serializationStart = System.nanoTime();

                FleasProblem problem = FleasProblem.newBuilder()
                        .setN(dto.getN())
                        .setM(dto.getM())
                        .setFeederRow(dto.getFeederRow())
                        .setFeederCol(dto.getFeederCol())
                        .setFleasCount5(dto.getFleasCount()) // без цифр
                        .addAllFleas6(dto.getFleas().stream()
                                .map(f -> FleaPosition.newBuilder()
                                        .setRow(f.getRow())
                                        .setCol(f.getCol())
                                        .build())
                                .toList())
                        .build();

                long serializationTime = System.nanoTime() - serializationStart;
                log.debug("Serialization time для задачи: {} ns", serializationTime);
                requestObserver.onNext(problem);
            }, sink::error, requestObserver::onCompleted);
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}