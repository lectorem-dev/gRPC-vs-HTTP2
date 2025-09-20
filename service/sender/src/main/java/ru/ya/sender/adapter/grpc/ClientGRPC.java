package ru.ya.sender.adapter.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import ru.ya.libs.FleasAnswerDto;
import ru.ya.libs.FleasProblemDto;
import ru.ya.libs.grpc.FleaPosition;
import ru.ya.libs.grpc.FleasAnswer;
import ru.ya.libs.grpc.FleasProblem;
import ru.ya.libs.grpc.FleasServiceGrpcGrpc;

@Component
public class ClientGRPC {

    private final FleasServiceGrpcGrpc.FleasServiceGrpcStub asyncStub;

    public ClientGRPC() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("receiver", 9090)
                .usePlaintext()
                .build();
        this.asyncStub = FleasServiceGrpcGrpc.newStub(channel);
    }

    public Flux<FleasAnswerDto> sendProblems(Flux<FleasProblemDto> problems) {
        return Flux.create(sink -> {
            StreamObserver<FleasAnswer> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(FleasAnswer value) {
                    sink.next(
                            FleasAnswerDto.builder()
                                    .result(value.getResult())
                                    .durationMs(value.getDurationMs())
                                    .build()
                    );
                }

                @Override
                public void onError(Throwable t) {
                    sink.error(t);
                }

                @Override
                public void onCompleted() {
                    sink.complete();
                }
            };

            StreamObserver<FleasProblem> requestObserver = asyncStub.calculate(responseObserver);

            problems.subscribe(dto -> {
                FleasProblem problem = FleasProblem.newBuilder()
                        .setN(dto.getN())
                        .setM(dto.getM())
                        .setFeederRow(dto.getFeederRow())
                        .setFeederCol(dto.getFeederCol())
                        .setFleasCount5(dto.getFleasCount())
                        .addAllFleas6(dto.getFleas().stream()
                                .map(f -> FleaPosition.newBuilder()
                                        .setRow(f.getRow())
                                        .setCol(f.getCol())
                                        .build())
                                .toList())
                        .build();
                requestObserver.onNext(problem);
            }, sink::error, requestObserver::onCompleted);
        }, FluxSink.OverflowStrategy.BUFFER);
    }
}